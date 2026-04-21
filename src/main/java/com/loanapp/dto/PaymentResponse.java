package com.loanapp.dto;

import com.loanapp.model.Payment;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentResponse {
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
    public String remarks;
}
