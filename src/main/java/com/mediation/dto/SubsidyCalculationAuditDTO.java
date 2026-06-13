package com.mediation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubsidyCalculationAuditDTO {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String reason;

    private String auditedBy;
}
