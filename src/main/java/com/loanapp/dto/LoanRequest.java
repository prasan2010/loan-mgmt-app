package com.loanapp.dto;

import com.loanapp.model.Loan;
import jakarta.validation.constraints.*;

public class LoanRequest {
    @NotNull public Long customerId;
    @NotNull public Loan.LoanType loanType;
    @NotNull @Positive public Double principalAmount;
    @NotNull @Positive public Double interestRate;
    @NotNull @Positive public Integer tenureMonths;
    public String purpose;
    public String remarks;
}
