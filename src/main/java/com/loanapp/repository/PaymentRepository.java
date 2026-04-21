package com.loanapp.repository;

import com.loanapp.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByLoanId(Long loanId);

    List<Payment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.loan.id = :loanId AND p.status = 'SUCCESS'")
    Double getTotalPaymentsForLoan(@Param("loanId") Long loanId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS'")
    long countSuccessfulPayments();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double getTotalCollections();
}
