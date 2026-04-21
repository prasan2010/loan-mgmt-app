package com.loanapp;

import com.loanapp.model.*;
import com.loanapp.repository.*;
import com.loanapp.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock CustomerRepository customerRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks LoanService loanService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .status(Customer.CustomerStatus.ACTIVE)
                .build();
    }

    @Test
    void testEMICalculation() {
        // ₹5,00,000 at 10% p.a. for 36 months
        double emi = loanService.calculateEMI(500000, 10, 36);
        // Expected ~16133.57
        assertEquals(16133.57, emi, 1.0, "EMI should be approximately ₹16,134");
    }

    @Test
    void testEMICalculationZeroRate() {
        double emi = loanService.calculateEMI(300000, 0, 12);
        assertEquals(25000.0, emi, 0.01, "Zero rate EMI should be principal/months");
    }

    @Test
    void testCreateLoan() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(loanRepository.count()).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        Loan loan = loanService.createLoan(1L, Loan.LoanType.PERSONAL,
                500000.0, 12.0, 24, "Test purpose");

        assertNotNull(loan);
        assertNotNull(loan.getLoanNumber());
        assertEquals(Loan.LoanStatus.PENDING, loan.getStatus());
        assertEquals(500000.0, loan.getPrincipalAmount());
        assertTrue(loan.getEmiAmount() > 0);
    }

    @Test
    void testCreateLoanCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                loanService.createLoan(99L, Loan.LoanType.PERSONAL, 100000.0, 10.0, 12, "test")
        );
    }

    @Test
    void testApproveLoan() {
        Loan pendingLoan = Loan.builder()
                .id(1L)
                .loanNumber("LN202401001")
                .customer(testCustomer)
                .principalAmount(100000.0)
                .outstandingBalance(100000.0)
                .interestRate(10.0)
                .tenureMonths(12)
                .emiAmount(8791.59)
                .status(Loan.LoanStatus.PENDING)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(pendingLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan approved = loanService.approveLoan(1L);

        assertEquals(Loan.LoanStatus.ACTIVE, approved.getStatus());
        assertNotNull(approved.getDisbursementDate());
        assertNotNull(approved.getMaturityDate());
        assertNotNull(approved.getNextDueDate());
    }

    @Test
    void testRecordPayment() {
        Loan activeLoan = Loan.builder()
                .id(1L)
                .loanNumber("LN202401001")
                .customer(testCustomer)
                .principalAmount(100000.0)
                .outstandingBalance(100000.0)
                .interestRate(12.0)
                .tenureMonths(12)
                .emiAmount(8884.88)
                .status(Loan.LoanStatus.ACTIVE)
                .nextDueDate(LocalDate.now().plusDays(5))
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.count()).thenReturn(0L);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Payment payment = loanService.recordPayment(
                1L, 8884.88, Payment.PaymentMode.NEFT, LocalDate.now(), "Test payment");

        assertNotNull(payment);
        assertNotNull(payment.getPaymentReference());
        assertEquals(Payment.PaymentStatus.SUCCESS, payment.getStatus());
        assertTrue(payment.getPrincipalComponent() > 0);
        assertTrue(payment.getInterestComponent() > 0);
    }

    @Test
    void testPaymentOnInactiveLoan() {
        Loan closedLoan = Loan.builder()
                .id(2L)
                .status(Loan.LoanStatus.CLOSED)
                .build();

        when(loanRepository.findById(2L)).thenReturn(Optional.of(closedLoan));

        assertThrows(RuntimeException.class, () ->
                loanService.recordPayment(2L, 5000.0, Payment.PaymentMode.CASH, LocalDate.now(), "")
        );
    }
}
