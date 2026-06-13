package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.dto.SubsidyCalculationAuditDTO;
import com.mediation.entity.SubsidyCalculation;
import com.mediation.service.SubsidyCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subsidy-calculations")
@RequiredArgsConstructor
public class SubsidyCalculationController {

    private final SubsidyCalculationService subsidyCalculationService;

    @PostMapping("/calculate/{disputeId}")
    public ResponseEntity<ApiResponse<SubsidyCalculation>> calculate(@PathVariable Long disputeId) {
        return ResponseEntity.ok(ApiResponse.success(subsidyCalculationService.calculate(disputeId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SubsidyCalculation>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) Long mediatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                subsidyCalculationService.list(page, size, auditStatus, mediatorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subsidyCalculationService.getDetail(id)));
    }

    @PutMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<SubsidyCalculation>> audit(
            @PathVariable Long id,
            @Valid @RequestBody SubsidyCalculationAuditDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(subsidyCalculationService.audit(id, dto)));
    }

    @PutMapping("/{id}/special-audit")
    public ResponseEntity<ApiResponse<SubsidyCalculation>> specialAudit(
            @PathVariable Long id,
            @Valid @RequestBody SubsidyCalculationAuditDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(subsidyCalculationService.specialApproveAudit(id, dto)));
    }
}
