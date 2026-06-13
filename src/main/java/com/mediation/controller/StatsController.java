package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.service.AuditStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final AuditStatsService auditStatsService;

    @GetMapping("/reconciliation/monthly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyReconciliation(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getMonthlyReconciliation(y, m)));
    }

    @GetMapping("/audit/annual")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnnualAudit(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getAnnualAudit(y)));
    }

    @GetMapping("/abnormal-mediators")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAbnormalMediators(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.detectAbnormalMediators(y)));
    }

    @GetMapping("/subsidy-trend/monthly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySubsidyTrend(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getMonthlySubsidyTrend(y)));
    }

    @GetMapping("/case-level/distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaseLevelDistribution(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getCaseLevelDistribution(y)));
    }

    @GetMapping("/mediator-ranking/annual")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMediatorAnnualRanking(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "10") int topN) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getMediatorAnnualRanking(y, topN)));
    }

    @GetMapping("/budget-execution/curve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBudgetExecutionCurve(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getBudgetExecutionCurve(y)));
    }

    @GetMapping("/average-subsidy/mediator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAverageSubsidyPerMediator(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(auditStatsService.getAverageSubsidyPerMediator(y)));
    }
}
