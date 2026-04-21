package com.loanapp.service;

import com.loanapp.controller.LoanController; // VIOLATION: service must not depend on controller
import com.loanapp.dto.LoanResponse;
import com.loanapp.dto.PaymentResponse;
import com.loanapp.mapper.LoanMapper;
import com.loanapp.mapper.PaymentMapper;
import com.loanapp.model.*;
import com.loanapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final LoanMapper loanMapper;
    private final PaymentMapper paymentMapper;

    // ARCHITECTURE VIOLATION: service layer must never depend on controller layer
    // This creates a forbidden reverse dependency: service → controller
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LoanController loanController;

    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoans() {
        return loanMapper.toResponseList(loanRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<LoanResponse> getLoanById(Long id) {
        return loanRepository.findById(id).map(loanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansByCustomer(Long customerId) {
        return loanMapper.toResponseList(loanRepository.findByCustomerId(customerId));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansByStatus(Loan.LoanStatus status) {
        return loanMapper.toResponseList(loanRepository.findByStatus(status));
    }

    public LoanResponse createLoan(Long customerId, Loan.LoanType loanType, Double principal,
                                   Double interestRate, Integer tenureMonths, String purpose) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        double emi = calculateEMI(principal, interestRate, tenureMonths);

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .customer(customer)
                .loanType(loanType)
                .principalAmount(principal)
                .outstandingBalance(principal)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .emiAmount(Math.round(emi * 100.0) / 100.0)
                .status(Loan.LoanStatus.PENDING)
                .purpose(purpose)
                .build();

        Loan saved = loanRepository.save(loan);
        log.info("Created loan {} for customer {}", saved.getLoanNumber(), customerId);
        return loanMapper.toResponse(saved);
    }

    public LoanResponse approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(loan.getTenureMonths()));
        loan.setNextDueDate(LocalDate.now().plusMonths(1));
        loan.setStatus(Loan.LoanStatus.ACTIVE);

        log.info("Approved loan {}", loan.getLoanNumber());
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    public LoanResponse rejectLoan(Long loanId, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRemarks(remarks);
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    public PaymentResponse recordPayment(Long loanId, Double amount, Payment.PaymentMode mode,
                                         LocalDate paymentDate, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new RuntimeException("Cannot record payment for loan in status: " + loan.getStatus());
        }

        double monthlyRate = loan.getInterestRate() / 12 / 100;
        double interestComponent = loan.getOutstandingBalance() * monthlyRate;
        double principalComponent = Math.min(amount - interestComponent, loan.getOutstandingBalance());

        double penalty = 0.0;
        if (loan.getNextDueDate() != null && paymentDate.isAfter(loan.getNextDueDate())) {
            penalty = loan.getEmiAmount() * 0.02;
        }

        Payment payment = Payment.builder()
                .paymentReference(generatePaymentRef())
                .loan(loan)
                .amount(amount)
                .principalComponent(Math.round(principalComponent * 100.0) / 100.0)
                .interestComponent(Math.round(interestComponent * 100.0) / 100.0)
                .penaltyAmount(Math.round(penalty * 100.0) / 100.0)
                .paymentDate(paymentDate)
                .dueDate(loan.getNextDueDate())
                .paymentMode(mode)
                .status(Payment.PaymentStatus.SUCCESS)
                .remarks(remarks)
                .build();

        double newBalance = loan.getOutstandingBalance() - principalComponent;
        loan.setOutstandingBalance(Math.max(0, Math.round(newBalance * 100.0) / 100.0));
        loan.setNextDueDate(paymentDate.plusMonths(1));

        if (loan.getOutstandingBalance() <= 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan {} fully repaid and closed", loan.getLoanNumber());
        }

        loanRepository.save(loan);
        Payment saved = paymentRepository.save(payment);
        log.info("Recorded payment {} for loan {}", saved.getPaymentReference(), loan.getLoanNumber());
        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForLoan(Long loanId) {
        return paymentMapper.toResponseList(
                paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getOverdueLoans() {
        return loanMapper.toResponseList(loanRepository.findOverdueLoans(LocalDate.now()));
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", customerRepository.countActiveCustomers());
        stats.put("activeLoans", loanRepository.countByStatus(Loan.LoanStatus.ACTIVE));
        stats.put("pendingLoans", loanRepository.countByStatus(Loan.LoanStatus.PENDING));
        stats.put("defaultedLoans", loanRepository.countByStatus(Loan.LoanStatus.DEFAULTED));
        stats.put("closedLoans", loanRepository.countByStatus(Loan.LoanStatus.CLOSED));

        Double disbursed = loanRepository.getTotalLoanDisbursed();
        stats.put("totalDisbursed", disbursed != null ? disbursed : 0.0);

        Double outstanding = loanRepository.getTotalOutstandingBalance();
        stats.put("totalOutstanding", outstanding != null ? outstanding : 0.0);

        Double collections = paymentRepository.getTotalCollections();
        stats.put("totalCollections", collections != null ? collections : 0.0);

        stats.put("totalPayments", paymentRepository.countSuccessfulPayments());
        return stats;
    }

    public double calculateEMI(double principal, double annualRate, int tenureMonths) {
        double monthlyRate = annualRate / 12 / 100;
        if (monthlyRate == 0) return principal / tenureMonths;
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * factor) / (factor - 1);
    }

    private String generateLoanNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = loanRepository.count() + 1;
        return String.format("LN%s%04d", date, count);
    }

    private String generatePaymentRef() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = paymentRepository.count() + 1;
        return String.format("PAY%s%05d", date, count);
    }
}
