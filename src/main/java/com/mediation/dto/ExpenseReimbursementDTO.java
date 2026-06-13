package com.mediation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseReimbursementDTO {

    @NotNull(message = "案件ID不能为空")
    private Long disputeId;

    @NotNull(message = "调解员ID不能为空")
    private Long mediatorId;

    @NotEmpty(message = "费用明细不能为空")
    @Valid
    private List<ExpenseItemDTO> items;

    private String remark;

    private String submitter;

    @Data
    public static class ExpenseItemDTO {
        @NotNull(message = "费用类型不能为空")
        private String expenseType;

        @NotNull(message = "金额不能为空")
        private BigDecimal amount;

        @NotNull(message = "费用日期不能为空")
        private LocalDate expenseDate;

        private String description;
    }
}
