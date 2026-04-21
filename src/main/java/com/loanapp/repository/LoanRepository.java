package com.loanapp.repository;

import com.loanapp.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    List<Loan> findByCustomerId(Long customerId);

    List<Loan> findByStatus(Loan.LoanStatus status);

    List<Loan> findByLoanType(Loan.LoanType loanType);

    @Query("SELECT l FROM Loan l WHERE l.nextDueDate <= :date AND l.status = 'ACTIVE'")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.status IN ('ACTIVE', 'APPROVED')")
    Double getTotalLoanDisbursed();

    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.status = 'ACTIVE'")
    Double getTotalOutstandingBalance();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    long countByStatus(@Param("status") Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.customer.id = :customerId AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansForCustomer(@Param("customerId") Long customerId);
}
