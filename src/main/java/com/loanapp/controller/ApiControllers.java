package com.loanapp.controller;

import com.loanapp.model.*;
import com.loanapp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ======================== Dashboard Controller ========================

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
class DashboardController {

    private final LoanService loanService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(loanService.getDashboardStats());
    }
}

// ======================== Customer REST Controller ========================

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
class CustomerRestController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(customerService.searchCustomers(search));
        }
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, Object> body) {
        try {
            Customer customer = customerService.createCustomer(
                    (String) body.get("firstName"),
                    (String) body.get("lastName"),
                    (String) body.get("email"),
                    (String) body.get("phone"),
                    body.get("dateOfBirth") != null ? LocalDate.parse((String) body.get("dateOfBirth")) : null,
                    (String) body.get("panNumber"),
                    (String) body.get("address"),
                    body.get("creditScore") != null ? ((Number) body.get("creditScore")).intValue() : null,
                    body.get("annualIncome") != null ? ((Number) body.get("annualIncome")).doubleValue() : null
            );
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body) {
        try {
            Customer.CustomerStatus status = body.get("status") != null
                    ? Customer.CustomerStatus.valueOf((String) body.get("status")) : null;
            Customer updated = customerService.updateCustomer(id,
                    (String) body.get("phone"),
                    (String) body.get("address"),
                    body.get("creditScore") != null ? ((Number) body.get("creditScore")).intValue() : null,
                    body.get("annualIncome") != null ? ((Number) body.get("annualIncome")).doubleValue() : null,
                    status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}

// ======================== Loan REST Controller ========================

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
class LoanRestController {

    private final LoanService loanService;

    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans(
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
    public ResponseEntity<Loan> getLoan(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@RequestBody Map<String, Object> body) {
        try {
            Loan loan = loanService.createLoan(
                    ((Number) body.get("customerId")).longValue(),
                    Loan.LoanType.valueOf((String) body.get("loanType")),
                    ((Number) body.get("principalAmount")).doubleValue(),
                    ((Number) body.get("interestRate")).doubleValue(),
                    ((Number) body.get("tenureMonths")).intValue(),
                    (String) body.get("purpose")
            );
            return ResponseEntity.ok(loan);
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
                                         @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(loanService.rejectLoan(id, body.get("remarks")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<Payment>> getLoanPayments(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getPaymentsForLoan(id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<?> recordPayment(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        try {
            Payment payment = loanService.recordPayment(
                    id,
                    ((Number) body.get("amount")).doubleValue(),
                    Payment.PaymentMode.valueOf((String) body.get("paymentMode")),
                    body.get("paymentDate") != null
                            ? LocalDate.parse((String) body.get("paymentDate"))
                            : LocalDate.now(),
                    (String) body.get("remarks")
            );
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
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
                "principal", principal
        ));
    }
}
