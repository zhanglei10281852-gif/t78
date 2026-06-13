package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense_reimbursements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseReimbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reimbursement_no", unique = true, nullable = false)
    private String reimbursementNo;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "mediator_id", nullable = false)
    private Long mediatorId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReimbursementStatus status;

    @Column(name = "submitter")
    private String submitter;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "admin_approver")
    private String adminApprover;

    @Column(name = "admin_approved_at")
    private LocalDateTime adminApprovedAt;

    @Column(name = "admin_reject_reason", columnDefinition = "TEXT")
    private String adminRejectReason;

    @Column(name = "finance_confirmer")
    private String financeConfirmer;

    @Column(name = "finance_confirmed_at")
    private LocalDateTime financeConfirmedAt;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reimbursement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReimbursementStatus.草稿;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ReimbursementStatus {
        草稿, 待审批, 管理员驳回, 待财务确认, 财务驳回, 已报销
    }
}
