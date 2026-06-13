package com.mediation.controller;

import com.mediation.common.ApiResponse;
import com.mediation.dto.SubsidyStandardDTO;
import com.mediation.entity.SubsidyStandard;
import com.mediation.service.SubsidyStandardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subsidy-standards")
@RequiredArgsConstructor
public class SubsidyStandardController {

    private final SubsidyStandardService subsidyStandardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubsidyStandard>>> list() {
        return ResponseEntity.ok(ApiResponse.success(subsidyStandardService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubsidyStandard>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subsidyStandardService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubsidyStandard>> update(@PathVariable Long id,
                                                               @Valid @RequestBody SubsidyStandardDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(subsidyStandardService.update(id, dto)));
    }
}
