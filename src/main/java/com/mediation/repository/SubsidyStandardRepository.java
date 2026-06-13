package com.mediation.repository;

import com.mediation.entity.SubsidyStandard;
import com.mediation.entity.SubsidyStandard.CaseLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubsidyStandardRepository extends JpaRepository<SubsidyStandard, Long> {

    Optional<SubsidyStandard> findByCaseLevel(CaseLevel caseLevel);

    boolean existsByCaseLevel(CaseLevel caseLevel);
}
