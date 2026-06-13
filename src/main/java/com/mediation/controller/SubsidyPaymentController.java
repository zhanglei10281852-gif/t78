package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.dto.BatchPaymentDTO;
import com.mediation.entity.SubsidyPayment;
import com.mediation.service.SubsidyPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subsidy-payments")
@RequiredArgsConstructor
public class SubsidyPaymentController {

    private final SubsidyPaymentService subsidyPaymentService;

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<SubsidyPayment>>> batchPay(
            @Valid @RequestBody BatchPaymentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(subsidyPaymentService.batchPay(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SubsidyPayment>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long mediatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                subsidyPaymentService.list(page, size, mediatorId)));
    }

    @GetMapping("/mediator/{mediatorId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMediatorIncomeSummary(
            @PathVariable Long mediatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                subsidyPaymentService.getMediatorIncomeSummary(mediatorId)));
    }

    @GetMapping("/mediator/{mediatorId}/details")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMediatorIncomeDetails(
            @PathVariable Long mediatorId) {
        return ResponseEntity.ok(ApiResponse.success(
                subsidyPaymentService.getMediatorIncomeDetails(mediatorId)));
    }
}
