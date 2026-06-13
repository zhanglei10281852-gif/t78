package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.dto.AnnualBudgetDTO;
import com.mediation.entity.AnnualBudget;
import com.mediation.service.AnnualBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.Map;

@RestController
@RequestMapping("/api/annual-budgets")
@RequiredArgsConstructor
public class AnnualBudgetController {

    private final AnnualBudgetService annualBudgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnnualBudget>> createOrUpdate(@Valid @RequestBody AnnualBudgetDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(annualBudgetService.createOrUpdate(dto)));
    }

    @GetMapping("/{year}")
    public ResponseEntity<ApiResponse<AnnualBudget>> getByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.success(annualBudgetService.getByYear(year)));
    }

    @GetMapping("/progress/{year}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(@PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.success(annualBudgetService.getBudgetProgress(year)));
    }

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentYearProgress() {
        return ResponseEntity.ok(ApiResponse.success(
                annualBudgetService.getBudgetProgress(Year.now().getValue())));
    }
}
