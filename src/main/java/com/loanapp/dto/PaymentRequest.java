package com.loanapp.dto;

import com.loanapp.model.Payment;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class PaymentRequest {
    @NotNull public Long loanId;
    @NotNull @Positive public Double amount;
    @NotNull public Payment.PaymentMode paymentMode;
    public LocalDate paymentDate;
    public String remarks;
}
