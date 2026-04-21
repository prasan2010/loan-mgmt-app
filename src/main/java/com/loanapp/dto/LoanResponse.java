package com.loanapp.dto;

import com.loanapp.model.Loan;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanResponse {
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
