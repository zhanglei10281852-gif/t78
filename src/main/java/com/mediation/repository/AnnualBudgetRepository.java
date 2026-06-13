package com.mediation.repository;

import com.mediation.entity.AnnualBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnualBudgetRepository extends JpaRepository<AnnualBudget, Long> {

    Optional<AnnualBudget> findByBudgetYearAndOrganization(Integer budgetYear, String organization);

    Optional<AnnualBudget> findByBudgetYear(Integer budgetYear);

    boolean existsByBudgetYearAndOrganization(Integer budgetYear, String organization);
}
