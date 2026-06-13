package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subsidy_standards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsidyStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_level", unique = true, nullable = false)
    private CaseLevel caseLevel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_mediation_count")
    private Integer minMediationCount;

    @Column(name = "max_mediation_count")
    private Integer maxMediationCount;

    @Column(name = "min_involved_people")
    private Integer minInvolvedPeople;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum CaseLevel {
        简易纠纷, 一般纠纷, 复杂纠纷, 集体纠纷
    }
}
