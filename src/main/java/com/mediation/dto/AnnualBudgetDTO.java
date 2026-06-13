package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnnualBudgetDTO {

    @NotNull(message = "预算年份不能为空")
    private Integer budgetYear;

    @NotBlank(message = "调解组织不能为空")
    private String organization;

    @NotNull(message = "预算总额不能为空")
    private BigDecimal totalAmount;

    private BigDecimal warningThreshold;

    private BigDecimal criticalThreshold;
}
