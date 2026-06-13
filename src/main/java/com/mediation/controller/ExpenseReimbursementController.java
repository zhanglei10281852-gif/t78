package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.dto.ExpenseReimbursementDTO;
import com.mediation.dto.ReimbursementAuditDTO;
import com.mediation.entity.ExpenseReimbursement;
import com.mediation.service.ExpenseReimbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/expense-reimbursements")
@RequiredArgsConstructor
public class ExpenseReimbursementController {

    private final ExpenseReimbursementService expenseReimbursementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseReimbursement>> create(
            @Valid @RequestBody ExpenseReimbursementDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(expenseReimbursementService.create(dto)));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ExpenseReimbursement>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseReimbursementService.submit(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpenseReimbursement>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long mediatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseReimbursementService.list(page, size, status, mediatorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseReimbursementService.getDetail(id)));
    }

    @PutMapping("/{id}/admin-audit")
    public ResponseEntity<ApiResponse<ExpenseReimbursement>> adminAudit(
            @PathVariable Long id,
            @Valid @RequestBody ReimbursementAuditDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseReimbursementService.adminAudit(id, dto)));
    }

    @PutMapping("/{id}/finance-confirm")
    public ResponseEntity<ApiResponse<ExpenseReimbursement>> financeConfirm(
            @PathVariable Long id,
            @Valid @RequestBody ReimbursementAuditDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseReimbursementService.financeConfirm(id, dto)));
    }
}
