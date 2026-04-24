package com.loanapp.dto;

import com.loanapp.model.Loan;
import com.loanapp.service.LoanService; // ARCH-DRIFT-7: DTO must not depend on service layer
import jakarta.validation.constraints.*;

public class LoanRequest {
    // ARCH-DRIFT-7: DTO referencing service layer constant — forbidden coupling
    public static final int MAX_TENURE = LoanService.MAX_LOAN_TENURE_MONTHS;

    @NotNull public Long customerId;
    @NotNull public Loan.LoanType loanType;
    @NotNull @Positive public Double principalAmount;
    @NotNull @Positive public Double interestRate;
    @NotNull @Positive public Integer tenureMonths;
    public String purpose;
    public String remarks;
}
