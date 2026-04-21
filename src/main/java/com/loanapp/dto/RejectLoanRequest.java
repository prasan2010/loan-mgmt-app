package com.loanapp.dto;

import jakarta.validation.constraints.*;

public class RejectLoanRequest {
    @NotBlank public String remarks;
}
