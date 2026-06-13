package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subsidy_calculations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsidyCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculation_no", unique = true, nullable = false)
    private String calculationNo;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "mediator_id", nullable = false)
    private Long mediatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_level", nullable = false)
    private SubsidyStandard.CaseLevel caseLevel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "mediation_count")
    private Integer mediationCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus auditStatus;

    @Column(name = "audit_reason", columnDefinition = "TEXT")
    private String auditReason;

    @Column(name = "audited_by")
    private String auditedBy;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.calculationDate == null) {
            this.calculationDate = LocalDate.now();
        }
        if (this.auditStatus == null) {
            this.auditStatus = AuditStatus.待审核;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AuditStatus {
        待审核, 审核通过, 审核驳回
    }
}
