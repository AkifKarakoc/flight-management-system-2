package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.dto.request.AirportSegmentRequest;
import com.flightmanagement.flightservice.exception.BusinessException;
import com.flightmanagement.flightservice.service.AutoRouteService;
import com.flightmanagement.flightservice.service.FlightService;
import com.flightmanagement.flightservice.service.ReferenceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flights/helpers")
@RequiredArgsConstructor
@Slf4j
public class FlightHelperController {

    private final AutoRouteService autoRouteService;
    private final ReferenceDataService referenceDataService;
    private final FlightService flightService;

    /**
     * Direct route preview - single airport pair
     */
    @GetMapping("/route-preview/direct")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDirectRoutePreview(
            @RequestParam Long originAirportId,
            @RequestParam Long destinationAirportId) {

        log.debug("Getting direct route preview for {} -> {}", originAirportId, destinationAirportId);

        Map<String, Object> preview = flightService.previewDirectRoute(originAirportId, destinationAirportId);
        return ResponseEntity.ok(preview);
    }

    /**
     * Multi-segment route preview
     */
    @PostMapping("/route-preview/multi-segment")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getMultiSegmentRoutePreview(
            @RequestBody @Valid List<AirportSegmentRequest> segments) {

        log.debug("Getting multi-segment route preview for {} segments", segments.size());

        if (segments == null || segments.size() < 2) {
            throw new BusinessException("Multi-segment preview requires at least 2 segments");
        }

        Map<String, Object> preview = flightService.previewMultiSegmentRoute(segments);
        return ResponseEntity.ok(preview);
    }

    /**
     * Aktif route'ları getir (frontend dropdown için)
     */
    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Object[]> getActiveRoutes() {
        log.debug("Getting active routes for flight creation");

        try {
            Object[] routes = referenceDataService.getActiveRoutes();
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            log.error("Error fetching active routes: {}", e.getMessage());
            return ResponseEntity.ok(new Object[0]); // Empty array fallback
        }
    }

    /**
     * Flight creation options - hangi creation mode'ları destekleniyor
     */
    @GetMapping("/creation-options")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getFlightCreationOptions() {
        Map<String, Object> options = new HashMap<>();

        options.put("supportedModes", List.of("ROUTE", "AIRPORTS", "MULTI_AIRPORTS"));
        options.put("maxSegments", 10);
        options.put("minConnectionTime", 30);
        options.put("maxConnectionTime", 1440);
        options.put("defaultConnectionTime", 60);

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ROUTE", "Select from existing routes");
        descriptions.put("AIRPORTS", "Select origin and destination airports (system will find/create route)");
        descriptions.put("MULTI_AIRPORTS", "Create multi-segment flight with connecting airports");

        options.put("modeDescriptions", descriptions);

        return ResponseEntity.ok(options);
    }

    /**
     * Validate airport segments
     */
    @PostMapping("/validate-segments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> validateAirportSegments(
            @RequestBody @Valid List<AirportSegmentRequest> segments) {

        log.debug("Validating {} airport segments", segments.size());

        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Basic validation
            if (segments.size() < 2) {
                errors.add("Multi-segment flight must have at least 2 segments");
            }

            if (segments.size() > 10) {
                errors.add("Multi-segment flight cannot have more than 10 segments");
            }

            // Segment continuity
            for (int i = 0; i < segments.size() - 1; i++) {
                AirportSegmentRequest current = segments.get(i);
                AirportSegmentRequest next = segments.get(i + 1);

                if (!current.getDestinationAirportId().equals(next.getOriginAirportId())) {
                    errors.add(String.format("Segment %d destination must match segment %d origin", i + 1, i + 2));
                }
            }

            // Connection time warnings
            for (int i = 0; i < segments.size(); i++) {
                AirportSegmentRequest segment = segments.get(i);

                if (segment.getConnectionTimeMinutes() != null) {
                    if (segment.getConnectionTimeMinutes() < 45) {
                        warnings.add(String.format("Segment %d has tight connection time (%d minutes)",
                                i + 1, segment.getConnectionTimeMinutes()));
                    }

                    if (segment.getConnectionTimeMinutes() > 480) {
                        warnings.add(String.format("Segment %d has very long connection time (%d minutes)",
                                i + 1, segment.getConnectionTimeMinutes()));
                    }
                }
            }

            validation.put("valid", errors.isEmpty());
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("segmentCount", segments.size());
            validation.put("stopCount", segments.size() - 1);

        } catch (Exception e) {
            log.error("Error validating airport segments: {}", e.getMessage());
            errors.add("Validation error: " + e.getMessage());
            validation.put("valid", false);
            validation.put("errors", errors);
        }

        return ResponseEntity.ok(validation);
    }
}