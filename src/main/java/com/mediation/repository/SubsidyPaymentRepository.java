package com.mediation.repository;

import com.mediation.entity.SubsidyPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubsidyPaymentRepository extends JpaRepository<SubsidyPayment, Long> {

    Optional<SubsidyPayment> findByCalculationId(Long calculationId);

    boolean existsByCalculationId(Long calculationId);

    Page<SubsidyPayment> findByMediatorId(Long mediatorId, Pageable pageable);

    List<SubsidyPayment> findByMediatorId(Long mediatorId);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SubsidyPayment sp " +
            "WHERE sp.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByDateBetween(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SubsidyPayment sp " +
            "WHERE FUNCTION('YEAR', sp.paymentDate) = :year")
    BigDecimal sumAmountByYear(@Param("year") Integer year);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SubsidyPayment sp WHERE sp.mediatorId = :mediatorId")
    BigDecimal sumAmountByMediatorId(@Param("mediatorId") Long mediatorId);

    @Query("SELECT sp.mediatorId, COALESCE(SUM(sp.amount), 0) FROM SubsidyPayment sp " +
            "WHERE FUNCTION('YEAR', sp.paymentDate) = :year GROUP BY sp.mediatorId ORDER BY SUM(sp.amount) DESC")
    List<Object[]> sumAmountByMediatorAndYear(@Param("year") Integer year);

    @Query("SELECT FUNCTION('MONTH', sp.paymentDate), COALESCE(SUM(sp.amount), 0) FROM SubsidyPayment sp " +
            "WHERE FUNCTION('YEAR', sp.paymentDate) = :year " +
            "GROUP BY FUNCTION('MONTH', sp.paymentDate) ORDER BY FUNCTION('MONTH', sp.paymentDate)")
    List<Object[]> monthlyTrendByYear(@Param("year") Integer year);
}
