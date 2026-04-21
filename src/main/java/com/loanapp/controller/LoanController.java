package com.loanapp.controller;

import com.loanapp.dto.*;
import com.loanapp.model.Loan;
import com.loanapp.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAllLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId) {
        if (customerId != null) {
            return ResponseEntity.ok(loanService.getLoansByCustomer(customerId));
        }
        if (status != null) {
            return ResponseEntity.ok(loanService.getLoansByStatus(Loan.LoanStatus.valueOf(status)));
        }
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@Valid @RequestBody LoanRequest request) {
        try {
            LoanResponse response = loanService.createLoan(
                    request.customerId, request.loanType,
                    request.principalAmount, request.interestRate,
                    request.tenureMonths, request.purpose);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLoan(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(loanService.approveLoan(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoan(@PathVariable Long id,
                                         @Valid @RequestBody RejectLoanRequest request) {
        try {
            return ResponseEntity.ok(loanService.rejectLoan(id, request.remarks));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getLoanPayments(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getPaymentsForLoan(id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<?> recordPayment(@PathVariable Long id,
                                            @Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = loanService.recordPayment(
                    id, request.amount, request.paymentMode,
                    request.paymentDate != null ? request.paymentDate : LocalDate.now(),
                    request.remarks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<LoanResponse>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    @GetMapping("/emi-calculator")
    public ResponseEntity<Map<String, Object>> calculateEMI(
            @RequestParam double principal,
            @RequestParam double rate,
            @RequestParam int months) {
        double emi = loanService.calculateEMI(principal, rate, months);
        double totalAmount = emi * months;
        double totalInterest = totalAmount - principal;
        return ResponseEntity.ok(Map.of(
                "emi", Math.round(emi * 100.0) / 100.0,
                "totalAmount", Math.round(totalAmount * 100.0) / 100.0,
                "totalInterest", Math.round(totalInterest * 100.0) / 100.0,
                "principal", principal));
    }
}
