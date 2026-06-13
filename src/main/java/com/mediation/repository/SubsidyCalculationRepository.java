package com.mediation.repository;

import com.mediation.entity.SubsidyCalculation;
import com.mediation.entity.SubsidyCalculation.AuditStatus;
import com.mediation.entity.SubsidyStandard.CaseLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubsidyCalculationRepository extends JpaRepository<SubsidyCalculation, Long> {

    Optional<SubsidyCalculation> findByDisputeId(Long disputeId);

    boolean existsByDisputeId(Long disputeId);

    Page<SubsidyCalculation> findByAuditStatus(AuditStatus auditStatus, Pageable pageable);

    Page<SubsidyCalculation> findByMediatorId(Long mediatorId, Pageable pageable);

    List<SubsidyCalculation> findByMediatorIdAndAuditStatus(Long mediatorId, AuditStatus auditStatus);

    List<SubsidyCalculation> findByIdIn(List<Long> ids);

    @Query("SELECT sc FROM SubsidyCalculation sc WHERE sc.auditStatus = :auditStatus AND sc.id IN :ids")
    List<SubsidyCalculation> findByIdsAndAuditStatus(@Param("ids") List<Long> ids,
                                                     @Param("auditStatus") AuditStatus auditStatus);

    @Query("SELECT COALESCE(SUM(sc.amount), 0) FROM SubsidyCalculation sc WHERE sc.auditStatus = :auditStatus " +
            "AND sc.calculationDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByAuditStatusAndDateBetween(@Param("auditStatus") AuditStatus auditStatus,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(sc.amount), 0) FROM SubsidyCalculation sc WHERE sc.auditStatus = :auditStatus " +
            "AND FUNCTION('YEAR', sc.calculationDate) = :year")
    BigDecimal sumAmountByAuditStatusAndYear(@Param("auditStatus") AuditStatus auditStatus,
                                             @Param("year") Integer year);

    @Query("SELECT sc.mediatorId, COALESCE(SUM(sc.amount), 0) FROM SubsidyCalculation sc " +
            "WHERE sc.auditStatus = :auditStatus AND FUNCTION('YEAR', sc.calculationDate) = :year " +
            "GROUP BY sc.mediatorId ORDER BY SUM(sc.amount) DESC")
    List<Object[]> sumAmountByMediatorAndYear(@Param("auditStatus") AuditStatus auditStatus,
                                              @Param("year") Integer year);

    @Query("SELECT sc.caseLevel, COUNT(sc), COALESCE(SUM(sc.amount), 0) FROM SubsidyCalculation sc " +
            "WHERE sc.auditStatus = :auditStatus AND FUNCTION('YEAR', sc.calculationDate) = :year " +
            "GROUP BY sc.caseLevel")
    List<Object[]> statsByCaseLevelAndYear(@Param("auditStatus") AuditStatus auditStatus,
                                           @Param("year") Integer year);

    @Query("SELECT FUNCTION('MONTH', sc.calculationDate), COALESCE(SUM(sc.amount), 0) FROM SubsidyCalculation sc " +
            "WHERE sc.auditStatus = :auditStatus AND FUNCTION('YEAR', sc.calculationDate) = :year " +
            "GROUP BY FUNCTION('MONTH', sc.calculationDate) ORDER BY FUNCTION('MONTH', sc.calculationDate)")
    List<Object[]> monthlyTrendByYear(@Param("auditStatus") AuditStatus auditStatus,
                                      @Param("year") Integer year);

    @Query("SELECT sc.mediatorId, FUNCTION('MONTH', sc.calculationDate), COUNT(sc) FROM SubsidyCalculation sc " +
            "WHERE sc.auditStatus = :auditStatus AND FUNCTION('YEAR', sc.calculationDate) = :year " +
            "GROUP BY sc.mediatorId, FUNCTION('MONTH', sc.calculationDate)")
    List<Object[]> monthlyCaseCountByMediator(@Param("auditStatus") AuditStatus auditStatus,
                                              @Param("year") Integer year);
}
