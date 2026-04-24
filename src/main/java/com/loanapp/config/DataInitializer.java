package com.loanapp.config;

import com.loanapp.dto.CustomerResponse;
import com.loanapp.dto.LoanResponse;
import com.loanapp.model.*;
import com.loanapp.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerService customerService;
    private final LoanService loanService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing sample data...");

        // Create sample customers
        CustomerResponse c1 = customerService.createCustomer(
                "Arjun", "Sharma", "arjun.sharma@email.com", "9876543210",
                LocalDate.of(1985, 3, 15), "ABCPS1234D",
                "123, MG Road, Bangalore, KA", 750, 1200000.0);

        CustomerResponse c2 = customerService.createCustomer(
                "Priya", "Nair", "priya.nair@email.com", "9123456789",
                LocalDate.of(1990, 7, 22), "EFGPN5678F",
                "45, Anna Salai, Chennai, TN", 720, 850000.0);

        CustomerResponse c3 = customerService.createCustomer(
                "Rahul", "Gupta", "rahul.gupta@email.com", "9988776655",
                LocalDate.of(1978, 11, 30), "HIJRG9012K",
                "78, Linking Road, Mumbai, MH", 780, 2500000.0);

        CustomerResponse c4 = customerService.createCustomer(
                "Sneha", "Reddy", "sneha.reddy@email.com", "8877665544",
                LocalDate.of(1995, 1, 10), "KLMSR3456P",
                "12, Banjara Hills, Hyderabad, TS", 690, 600000.0);

        // Create loans
        LoanResponse l1 = loanService.createLoan(c1.id, Loan.LoanType.HOME, 5000000.0, 8.5, 240, "Purchase of apartment");
        loanService.approveLoan(l1.id);

        LoanResponse l2 = loanService.createLoan(c2.id, Loan.LoanType.PERSONAL, 500000.0, 12.5, 36, "Medical expenses");
        loanService.approveLoan(l2.id);

        LoanResponse l3 = loanService.createLoan(c3.id, Loan.LoanType.AUTO, 800000.0, 9.0, 60, "Purchase of SUV");
        loanService.approveLoan(l3.id);

        LoanResponse l4 = loanService.createLoan(c4.id, Loan.LoanType.EDUCATION, 300000.0, 10.5, 48, "MBA program fees");
        // Leave as PENDING

        LoanResponse l5 = loanService.createLoan(c1.id, Loan.LoanType.PERSONAL, 200000.0, 14.0, 24, "Home renovation");
        loanService.approveLoan(l5.id);

        // Record some payments
        loanService.recordPayment(l1.id, 43841.0, Payment.PaymentMode.AUTO_DEBIT,
                LocalDate.now().minusMonths(2), "EMI Month 1");
        loanService.recordPayment(l1.id, 43841.0, Payment.PaymentMode.AUTO_DEBIT,
                LocalDate.now().minusMonths(1), "EMI Month 2");

        loanService.recordPayment(l2.id, 16720.0, Payment.PaymentMode.NEFT,
                LocalDate.now().minusMonths(1), "EMI payment");

        loanService.recordPayment(l3.id, 16607.0, Payment.PaymentMode.UPI,
                LocalDate.now().minusMonths(2), "Auto EMI");

        log.info("Sample data initialized successfully!");
        log.info("Login credentials: admin/admin123 or officer/officer123");
    }
}