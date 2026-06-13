package com.mediation.repository;

import com.mediation.entity.ExpenseReimbursement;
import com.mediation.entity.ExpenseReimbursement.ReimbursementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExpenseReimbursementRepository extends JpaRepository<ExpenseReimbursement, Long> {

    Optional<ExpenseReimbursement> findByReimbursementNo(String reimbursementNo);

    Page<ExpenseReimbursement> findByStatus(ReimbursementStatus status, Pageable pageable);

    Page<ExpenseReimbursement> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<ExpenseReimbursement> findByMediatorIdAndStatus(Long mediatorId, ReimbursementStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(er.totalAmount), 0) FROM ExpenseReimbursement er " +
            "WHERE er.status = :status AND er.submittedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByStatusAndDateBetween(@Param("status") ReimbursementStatus status,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(er.totalAmount), 0) FROM ExpenseReimbursement er " +
            "WHERE er.status = :status AND FUNCTION('YEAR', er.submittedAt) = :year")
    BigDecimal sumAmountByStatusAndYear(@Param("status") ReimbursementStatus status,
                                        @Param("year") Integer year);
}
