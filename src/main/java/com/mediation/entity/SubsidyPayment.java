package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subsidy_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsidyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", unique = true, nullable = false)
    private String paymentNo;

    @Column(name = "calculation_id", nullable = false)
    private Long calculationId;

    @Column(name = "mediator_id", nullable = false)
    private Long mediatorId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "bank_transfer_no")
    private String bankTransferNo;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.paymentDate == null) {
            this.paymentDate = LocalDate.now();
        }
    }

    public enum PaymentMethod {
        银行转账, 现金
    }
}
