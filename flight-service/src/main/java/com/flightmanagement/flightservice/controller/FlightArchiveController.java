package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.dto.request.ArchiveSearchRequest;
import com.flightmanagement.flightservice.dto.response.ArchivedFlightResponse;
import com.flightmanagement.flightservice.service.ArchiveServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flights/archive")
@RequiredArgsConstructor
@Slf4j
public class FlightArchiveController {

    private final ArchiveServiceClient archiveServiceClient;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ArchivedFlightResponse>> searchArchivedFlights(
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String originAirportCode,
            @RequestParam(required = false) String destinationAirportCode,
            @RequestParam(defaultValue = "false") boolean includePayload,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "archivedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.debug("Searching archived flights - flightNumber: {}, airlineId: {}, startDate: {}, endDate: {}",
                flightNumber, airlineId, startDate, endDate);

        ArchiveSearchRequest searchRequest = ArchiveSearchRequest.builder()
                .flightNumber(flightNumber)
                .airlineId(airlineId)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .eventType(eventType)
                .originAirportCode(originAirportCode)
                .destinationAirportCode(destinationAirportCode)
                .includePayload(includePayload)
                .build();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        Page<ArchivedFlightResponse> results = archiveServiceClient.searchArchivedFlights(searchRequest, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{flightNumber}/{flightDate}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ArchivedFlightResponse> getArchivedFlight(
            @PathVariable String flightNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate) {

        log.debug("Getting archived flight: {} on {}", flightNumber, flightDate);

        ArchivedFlightResponse archivedFlight = archiveServiceClient.getArchivedFlight(flightNumber, flightDate);

        if (archivedFlight != null) {
            return ResponseEntity.ok(archivedFlight);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getArchiveStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.debug("Getting archive stats from {} to {}", startDate, endDate);

        Map<String, Object> stats = archiveServiceClient.getArchiveStats(startDate, endDate);

        // Add request info to response
        stats.put("requestedStartDate", startDate);
        stats.put("requestedEndDate", endDate);
        stats.put("requestedBy", "flight-service");

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getArchiveServiceHealth() {
        log.debug("Checking archive service health");

        Map<String, Object> health = new HashMap<>();

        boolean isHealthy = archiveServiceClient.isArchiveServiceHealthy();
        health.put("archiveServiceHealthy", isHealthy);
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("checkTime", java.time.LocalDateTime.now());

        if (isHealthy) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ArchivedFlightResponse>> getRecentArchivedFlights(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Getting recent archived flights for last {} days", days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        ArchiveSearchRequest searchRequest = ArchiveSearchRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .sortByDateDesc(true)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "archivedAt"));

        Page<ArchivedFlightResponse> results = archiveServiceClient.searchArchivedFlights(searchRequest, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getArchiveSummary() {
        log.debug("Getting archive summary");

        Map<String, Object> summary = new HashMap<>();

        try {
            // Last 7 days stats
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);

            Map<String, Object> weeklyStats = archiveServiceClient.getArchiveStats(startDate, endDate);
            summary.put("lastWeek", weeklyStats);

            // Last 30 days stats
            startDate = endDate.minusDays(30);
            Map<String, Object> monthlyStats = archiveServiceClient.getArchiveStats(startDate, endDate);
            summary.put("lastMonth", monthlyStats);

            // Service health
            summary.put("archiveServiceHealthy", archiveServiceClient.isArchiveServiceHealthy());
            summary.put("generatedAt", java.time.LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error generating archive summary: {}", e.getMessage());
            summary.put("error", "Failed to generate summary");
            summary.put("archiveServiceHealthy", false);
        }

        return ResponseEntity.ok(summary);
    }
}