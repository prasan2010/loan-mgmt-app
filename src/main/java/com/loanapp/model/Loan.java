package com.loanapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", unique = true, nullable = false)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @NotNull(message = "Principal amount is required")
    @Positive(message = "Principal amount must be positive")
    @Column(name = "principal_amount", nullable = false)
    private Double principalAmount;

    @Column(name = "outstanding_balance")
    private Double outstandingBalance;

    @NotNull(message = "Interest rate is required")
    @Positive(message = "Interest rate must be positive")
    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    @NotNull(message = "Tenure is required")
    @Positive(message = "Tenure must be positive")
    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount")
    private Double emiAmount;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum LoanType {
        PERSONAL("Personal Loan"),
        HOME("Home Loan"),
        AUTO("Auto Loan"),
        EDUCATION("Education Loan"),
        BUSINESS("Business Loan"),
        GOLD("Gold Loan");

        private final String displayName;

        LoanType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum LoanStatus {
        PENDING, APPROVED, ACTIVE, CLOSED, DEFAULTED, REJECTED
    }
}
