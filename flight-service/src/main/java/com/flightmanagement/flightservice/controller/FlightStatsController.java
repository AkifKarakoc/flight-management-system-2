package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.dto.response.stats.FlightChartDataDto;
import com.flightmanagement.flightservice.dto.response.stats.FlightTypeDistributionDto;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/flights/stats")
@RequiredArgsConstructor
public class FlightStatsController {

    private final FlightService flightService;

    @GetMapping("/count/date/{date}")
    public ResponseEntity<Map<String, Object>> getFlightCountByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long count = flightService.getFlightCountByDate(date);

        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("totalFlights", count);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/airline/{airlineId}/date/{date}")
    public ResponseEntity<Map<String, Object>> getFlightCountByAirlineAndDate(
            @PathVariable Long airlineId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long count = flightService.getFlightCountByAirlineAndDate(airlineId, date);

        Map<String, Object> response = new HashMap<>();
        response.put("airlineId", airlineId);
        response.put("date", date);
        response.put("flightCount", count);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/status/{status}/date/{date}")
    public ResponseEntity<Map<String, Object>> getFlightCountByStatus(
            @PathVariable FlightStatus status,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long count = flightService.getFlightCountByStatus(status, date);

        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("date", date);
        response.put("flightCount", count);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/{date}")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(flightService.getDailySummary(date));
    }

    @GetMapping("/daily-chart")
    public ResponseEntity<FlightChartDataDto> getFlightChartData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Eğer tarihler frontend'den gelmezse, varsayılan olarak son 7 günü ayarla
        LocalDate finalEndDate = Optional.ofNullable(endDate).orElse(LocalDate.now());
        LocalDate finalStartDate = Optional.ofNullable(startDate).orElse(finalEndDate.minusDays(6));

        return ResponseEntity.ok(flightService.getFlightChartData(finalStartDate, finalEndDate));
    }

    @GetMapping("/type-distribution")
    public ResponseEntity<List<FlightTypeDistributionDto>> getFlightTypeDistribution() {
        return ResponseEntity.ok(flightService.getFlightTypeDistribution());
    }
}