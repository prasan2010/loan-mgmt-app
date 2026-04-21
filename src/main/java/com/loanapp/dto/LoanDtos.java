package com.loanapp.dto;

import com.loanapp.model.Customer;
import com.loanapp.model.Loan;
import com.loanapp.model.Payment;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ======================== Customer DTOs ========================

class CustomerRequest {
    @NotBlank public String firstName;
    @NotBlank public String lastName;
    @Email @NotBlank public String email;
    @Pattern(regexp = "^[0-9]{10}$") public String phone;
    @NotNull public LocalDate dateOfBirth;
    public String panNumber;
    public String address;
    public Integer creditScore;
    public Double annualIncome;
}

class CustomerResponse {
    public Long id;
    public String firstName;
    public String lastName;
    public String fullName;
    public String email;
    public String phone;
    public LocalDate dateOfBirth;
    public String panNumber;
    public String address;
    public Integer creditScore;
    public Double annualIncome;
    public Customer.CustomerStatus status;
    public LocalDateTime createdAt;
    public int totalLoans;
}

// ======================== Loan DTOs ========================

class LoanRequest {
    @NotNull public Long customerId;
    @NotNull public Loan.LoanType loanType;
    @NotNull @Positive public Double principalAmount;
    @NotNull @Positive public Double interestRate;
    @NotNull @Positive public Integer tenureMonths;
    public String purpose;
    public String remarks;
}

class LoanResponse {
    public Long id;
    public String loanNumber;
    public Long customerId;
    public String customerName;
    public Loan.LoanType loanType;
    public Double principalAmount;
    public Double outstandingBalance;
    public Double interestRate;
    public Integer tenureMonths;
    public Double emiAmount;
    public LocalDate disbursementDate;
    public LocalDate maturityDate;
    public LocalDate nextDueDate;
    public Loan.LoanStatus status;
    public String purpose;
    public LocalDateTime createdAt;
}

// ======================== Payment DTOs ========================

class PaymentRequest {
    @NotNull public Long loanId;
    @NotNull @Positive public Double amount;
    @NotNull public Payment.PaymentMode paymentMode;
    @NotNull public LocalDate paymentDate;
    public String remarks;
}

class PaymentResponse {
    public Long id;
    public String paymentReference;
    public Long loanId;
    public String loanNumber;
    public Double amount;
    public Double principalComponent;
    public Double interestComponent;
    public Double penaltyAmount;
    public LocalDate paymentDate;
    public Payment.PaymentMode paymentMode;
    public Payment.PaymentStatus status;
    public LocalDateTime createdAt;
}

// ======================== Dashboard DTO ========================

class DashboardStats {
    public long totalCustomers;
    public long activeLoans;
    public long pendingLoans;
    public long defaultedLoans;
    public Double totalDisbursed;
    public Double totalOutstanding;
    public Double totalCollections;
    public long totalPayments;
}
