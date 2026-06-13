package com.mediation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchPaymentDTO {

    @NotEmpty(message = "核算单ID列表不能为空")
    private List<Long> calculationIds;

    @NotNull(message = "发放日期不能为空")
    private LocalDate paymentDate;

    @NotNull(message = "发放方式不能为空")
    private String paymentMethod;

    private String createdBy;

    private String remark;
}
