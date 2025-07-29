package com.flightmanagement.flightarchiveservice.controller;

import com.flightmanagement.flightarchiveservice.dto.response.KpiResponse;
import com.flightmanagement.flightarchiveservice.service.KpiCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiCalculationService kpiCalculationService;

    @GetMapping("/{date}")
    public ResponseEntity<KpiResponse> getKpiForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        KpiResponse kpi = kpiCalculationService.calculateKpisForDate(date);
        return ResponseEntity.ok(kpi);
    }

    @PostMapping("/calculate/{date}")
    public ResponseEntity<KpiResponse> calculateKpiForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        KpiResponse kpi = kpiCalculationService.calculateKpisForDate(date);
        return ResponseEntity.ok(kpi);
    }
}