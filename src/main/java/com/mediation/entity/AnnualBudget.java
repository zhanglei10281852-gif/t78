package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;

@Entity
@Table(name = "annual_budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"budget_year", "organization"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    @Column(nullable = false)
    private String organization;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "used_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal warningThreshold = new BigDecimal("80.00");

    @Column(name = "critical_threshold", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal criticalThreshold = new BigDecimal("95.00");

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.budgetYear == null) {
            this.budgetYear = Year.now().getValue();
        }
        if (this.usedAmount == null) {
            this.usedAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
