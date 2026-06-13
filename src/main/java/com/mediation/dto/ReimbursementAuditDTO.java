package com.mediation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReimbursementAuditDTO {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    private String reason;

    private String approver;
}
