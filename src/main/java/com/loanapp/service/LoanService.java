package com.loanapp.service;

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

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public Optional<Loan> getLoanByNumber(String loanNumber) {
        return loanRepository.findByLoanNumber(loanNumber);
    }

    public List<Loan> getLoansByCustomer(Long customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    public List<Loan> getLoansByStatus(Loan.LoanStatus status) {
        return loanRepository.findByStatus(status);
    }

    public Loan createLoan(Long customerId, Loan.LoanType loanType, Double principal,
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
        return saved;
    }

    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(loan.getTenureMonths()));
        loan.setNextDueDate(LocalDate.now().plusMonths(1));
        loan.setStatus(Loan.LoanStatus.ACTIVE);

        log.info("Approved loan {}", loan.getLoanNumber());
        return loanRepository.save(loan);
    }

    public Loan rejectLoan(Long loanId, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRemarks(remarks);
        return loanRepository.save(loan);
    }

    public Payment recordPayment(Long loanId, Double amount, Payment.PaymentMode mode,
                                  LocalDate paymentDate, String remarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new RuntimeException("Cannot record payment for loan in status: " + loan.getStatus());
        }

        // Calculate interest and principal components
        double monthlyRate = loan.getInterestRate() / 12 / 100;
        double interestComponent = loan.getOutstandingBalance() * monthlyRate;
        double principalComponent = Math.min(amount - interestComponent, loan.getOutstandingBalance());

        // Check for penalty (late payment)
        double penalty = 0.0;
        if (loan.getNextDueDate() != null && paymentDate.isAfter(loan.getNextDueDate())) {
            penalty = loan.getEmiAmount() * 0.02; // 2% penalty
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

        // Update outstanding balance
        double newBalance = loan.getOutstandingBalance() - principalComponent;
        loan.setOutstandingBalance(Math.max(0, Math.round(newBalance * 100.0) / 100.0));
        loan.setNextDueDate(paymentDate.plusMonths(1));

        // Close loan if fully paid
        if (loan.getOutstandingBalance() <= 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan {} fully repaid and closed", loan.getLoanNumber());
        }

        loanRepository.save(loan);
        Payment saved = paymentRepository.save(payment);
        log.info("Recorded payment {} for loan {}", saved.getPaymentReference(), loan.getLoanNumber());
        return saved;
    }

    public List<Payment> getPaymentsForLoan(Long loanId) {
        return paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
    }

    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now());
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

    /**
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * P = principal, r = monthly interest rate, n = tenure in months
     */
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
