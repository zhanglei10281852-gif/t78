package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubsidyStandardDTO {

    @NotBlank(message = "案件档位不能为空")
    private String caseLevel;

    @NotNull(message = "补贴金额不能为空")
    private BigDecimal amount;

    private String description;

    private Integer minMediationCount;

    private Integer maxMediationCount;

    private Integer minInvolvedPeople;

    private BigDecimal minAmount;
}
