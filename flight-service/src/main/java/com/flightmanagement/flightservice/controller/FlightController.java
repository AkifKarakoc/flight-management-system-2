package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.dto.response.CsvPreviewResponse;
import com.flightmanagement.flightservice.entity.enums.FlightType;
import com.flightmanagement.flightservice.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.dto.request.FlightRequest;
import com.flightmanagement.flightservice.dto.response.CsvUploadResult;
import com.flightmanagement.flightservice.dto.response.FlightResponse;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.exception.DuplicateResourceException;
import com.flightmanagement.flightservice.service.CsvProcessingService;
import com.flightmanagement.flightservice.service.FlightService;
import com.flightmanagement.flightservice.repository.FlightRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flightmanagement.flightservice.dto.request.AirportSegmentRequest;
import com.flightmanagement.flightservice.dto.request.ConnectingFlightRequest;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.exception.BusinessException;
import com.flightmanagement.flightservice.service.AutoRouteService;
import com.flightmanagement.flightservice.service.ReferenceDataService;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Slf4j
public class FlightController {

    private final FlightService flightService;
    private final CsvProcessingService csvProcessingService;
    private final FlightRepository flightRepository;
    private final ReferenceDataService referenceDataService;
    private final AutoRouteService autoRouteService;

    // ===============================
    // TEMEL FLIGHT CRUD İŞLEMLERİ
    // ===============================

    @GetMapping
    public ResponseEntity<Page<FlightResponse>> getAllFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate,
            @RequestParam(required = false) Long routeId) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        Page<FlightResponse> flights = flightService.getAllFlightsWithFilters(
                pageable, flightNumber, airlineId, flightDate);

        return ResponseEntity.ok(flights);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @GetMapping("/flight-number/{flightNumber}")
    public ResponseEntity<List<FlightResponse>> getFlightsByNumber(@PathVariable String flightNumber) {
        return ResponseEntity.ok(flightService.getFlightsByFlightNumber(flightNumber));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<FlightResponse>> getFlightsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(flightService.getFlightsByDate(date));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<FlightResponse>> getFlightsByAirline(@PathVariable Long airlineId) {
        return ResponseEntity.ok(flightService.getFlightsByAirline(airlineId));
    }

    @GetMapping("/airport/{airportId}")
    public ResponseEntity<List<FlightResponse>> getFlightsByAirport(@PathVariable Long airportId) {
        return ResponseEntity.ok(flightService.getFlightsByAirport(airportId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FlightResponse>> getFlightsByStatus(@PathVariable FlightStatus status) {
        return ResponseEntity.ok(flightService.getFlightsByStatus(status));
    }

    @GetMapping("/delayed")
    public ResponseEntity<List<FlightResponse>> getDelayedFlights(
            @RequestParam(defaultValue = "15") Integer minDelayMinutes) {
        return ResponseEntity.ok(flightService.getDelayedFlights(minDelayMinutes));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody FlightRequest request) {
        log.info("Creating new flight: {}", request.getFlightNumber());
        return new ResponseEntity<>(flightService.createFlight(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> updateFlight(@PathVariable Long id,
                                                       @Valid @RequestBody FlightRequest request) {
        log.info("Updating flight with ID: {}", id);
        return ResponseEntity.ok(flightService.updateFlight(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> updateFlightStatus(@PathVariable Long id,
                                                             @RequestParam FlightStatus status) {
        log.info("Updating flight status for ID: {} to {}", id, status);
        return ResponseEntity.ok(flightService.updateFlightStatus(id, status));
    }

    @PutMapping("/{id}/delay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> recordDelay(@PathVariable Long id,
                                                      @RequestParam Integer delayMinutes,
                                                      @RequestParam(required = false) String reason) {
        log.info("Recording delay for flight ID: {} - {} minutes", id, delayMinutes);
        return ResponseEntity.ok(flightService.recordDelay(id, delayMinutes, reason));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        log.info("Deleting flight with ID: {}", id);
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    // ===============================
    // ROUTE PREVIEW ENDPOİNTLERİ
    // ===============================

    @GetMapping("/route-preview/direct")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> previewDirectRoute(
            @RequestParam Long originAirportId,
            @RequestParam Long destinationAirportId) {

        log.debug("Direct route preview request: {} -> {}", originAirportId, destinationAirportId);

        try {
            Map<String, Object> preview = flightService.previewDirectRoute(originAirportId, destinationAirportId);
            return ResponseEntity.ok(preview);
        } catch (BusinessException e) {
            log.error("Business error in direct route preview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Business Error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error creating direct route preview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", "Failed to create route preview",
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @PostMapping("/route-preview/multi-segment")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> previewMultiSegmentRoute(
            @RequestBody @Valid List<AirportSegmentRequest> segments) {

        log.debug("Multi-segment route preview request for {} segments", segments.size());

        if (segments == null || segments.size() < 2) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Request",
                    "message", "Multi-segment preview requires at least 2 segments",
                    "timestamp", LocalDateTime.now()
            ));
        }

        if (segments.size() > 10) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Request",
                    "message", "Multi-segment preview cannot have more than 10 segments",
                    "timestamp", LocalDateTime.now()
            ));
        }

        try {
            Map<String, Object> preview = flightService.previewMultiSegmentRoute(segments);
            return ResponseEntity.ok(preview);
        } catch (BusinessException e) {
            log.error("Business error in multi-segment route preview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Business Error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error creating multi-segment route preview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", "Failed to create route preview",
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    // ===============================
    // CREATION MODE BİLGİ ENDPOİNTLERİ
    // ===============================

    @GetMapping("/creation-modes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getFlightCreationModes() {
        Map<String, Object> modes = new HashMap<>();

        List<Map<String, Object>> availableModes = new ArrayList<>();

        // Route-based mode
        Map<String, Object> routeMode = new HashMap<>();
        routeMode.put("id", "ROUTE");
        routeMode.put("name", "Existing Route");
        routeMode.put("description", "Select from existing routes in the system");
        routeMode.put("icon", "route");
        routeMode.put("color", "blue");
        routeMode.put("fields", List.of("routeId"));
        routeMode.put("complexity", "SIMPLE");
        routeMode.put("recommended", true);
        availableModes.add(routeMode);

        // Airport-based mode
        Map<String, Object> airportMode = new HashMap<>();
        airportMode.put("id", "AIRPORTS");
        airportMode.put("name", "Airport Pair");
        airportMode.put("description", "Select origin and destination airports (direct flight)");
        airportMode.put("icon", "airplane");
        airportMode.put("color", "green");
        airportMode.put("fields", List.of("originAirportId", "destinationAirportId"));
        airportMode.put("complexity", "SIMPLE");
        airportMode.put("recommended", false);
        availableModes.add(airportMode);

        // Multi-segment mode
        Map<String, Object> multiMode = new HashMap<>();
        multiMode.put("id", "MULTI_AIRPORTS");
        multiMode.put("name", "Multi-Segment");
        multiMode.put("description", "Create connecting flight with multiple segments");
        multiMode.put("icon", "connecting-flights");
        multiMode.put("color", "orange");
        multiMode.put("fields", List.of("airportSegments"));
        multiMode.put("complexity", "ADVANCED");
        multiMode.put("recommended", false);
        availableModes.add(multiMode);

        modes.put("modes", availableModes);
        modes.put("defaultMode", "ROUTE");
        modes.put("constraints", Map.of(
                "maxSegments", 10,
                "minConnectionTime", 30,
                "maxConnectionTime", 1440,
                "minSegmentsForMulti", 2
        ));

        return ResponseEntity.ok(modes);
    }

    @PostMapping("/validate-creation-request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> validateFlightCreationRequest(
            @RequestBody FlightRequest request) {

        log.debug("Validating flight creation request: mode = {}, flight = {}",
                request.getCreationMode(), request.getFlightNumber());

        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Basic validation
            if (request.getCreationMode() == null || request.getCreationMode().isEmpty()) {
                errors.add("Creation mode is required");
            }

            // Use FlightRequest's built-in validation
            List<String> requestErrors = request.getValidationErrors();
            errors.addAll(requestErrors);

            // Mode-specific additional validation
            if (errors.isEmpty() || request.getCreationMode() != null) {
                switch (request.getCreationMode()) {
                    case "ROUTE":
                        validateRouteBasedCreation(request, errors, warnings);
                        break;
                    case "AIRPORTS":
                        validateAirportBasedCreation(request, errors, warnings);
                        break;
                    case "MULTI_AIRPORTS":
                        validateMultiSegmentCreation(request, errors, warnings);
                        break;
                    default:
                        errors.add("Invalid creation mode: " + request.getCreationMode());
                }
            }

            // Flight timing validation
            if (request.getScheduledDeparture() != null && request.getScheduledArrival() != null) {
                if (!request.isFlightTimeValid()) {
                    errors.add("Scheduled arrival must be after scheduled departure");
                }

                // Duration warnings
                Integer duration = request.getEstimatedDurationMinutes();
                if (duration != null) {
                    if (duration < 30) {
                        warnings.add("Flight duration seems very short (" + duration + " minutes)");
                    } else if (duration > 1200) { // 20 hours
                        warnings.add("Flight duration seems very long (" + duration + " minutes)");
                    }
                }
            }

            // Flight type consistency
            if (!request.isFlightTypeConsistent()) {
                errors.add("Flight type is not consistent with passenger/cargo counts");
            }

            // Duplicate flight number check (if flight date is provided)
            if (request.getFlightDate() != null && request.getFlightNumber() != null) {
                try {
                    boolean exists = flightRepository.existsByFlightNumberAndFlightDate(
                            request.getFlightNumber(), request.getFlightDate());
                    if (exists) {
                        errors.add("Flight number already exists for this date: " + request.getFlightNumber());
                    }
                } catch (Exception e) {
                    warnings.add("Could not check for duplicate flight number");
                }
            }

            validation.put("valid", errors.isEmpty());
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("errorCount", errors.size());
            validation.put("warningCount", warnings.size());
            validation.put("creationMode", request.getCreationMode());
            validation.put("complexity", getComplexityLevel(request));
            validation.put("timestamp", LocalDateTime.now());

            if (errors.isEmpty()) {
                validation.put("message", "Flight creation request is valid");
                validation.put("readyToCreate", true);
            } else {
                validation.put("message", "Flight creation request has validation errors");
                validation.put("readyToCreate", false);
            }

            return ResponseEntity.ok(validation);

        } catch (Exception e) {
            log.error("Error validating flight creation request: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("errors", List.of("Validation process failed: " + e.getMessage()));
            errorResponse.put("warnings", List.of());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ===============================
    // ROUTE BAZLI ENDPOİNTLER
    // ===============================

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<FlightResponse>> getFlightsByRoute(
            @PathVariable Long routeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Getting flights for route ID: {} on date: {}", routeId, date);
        List<FlightResponse> flights = flightService.getFlightsByRoute(routeId, date);
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/route/{routeId}/paged")
    public ResponseEntity<Page<FlightResponse>> getFlightsByRoutePaged(
            @PathVariable Long routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledDeparture") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        Page<FlightResponse> flights = flightService.getFlightsByRoutePaged(routeId, pageable);
        return ResponseEntity.ok(flights);
    }

    @GetMapping("/{id}/route-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFlightRouteInfo(@PathVariable Long id) {
        log.info("Getting route info for flight ID: {}", id);
        Map<String, Object> routeInfo = flightService.getFlightRouteInfo(id);
        return ResponseEntity.ok(routeInfo);
    }

    // ===============================
    // CSV UPLOAD ENDPOİNTLERİ
    // ===============================

    @PostMapping("/upload/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CsvPreviewResponse> previewCsv(@RequestParam("file") MultipartFile file) {
        log.info("Processing CSV preview: {}", file.getOriginalFilename());

        try {
            CsvPreviewResponse preview = csvProcessingService.previewCsvFile(file);
            return ResponseEntity.ok(preview);

        } catch (BusinessException e) {
            log.error("CSV preview validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorPreview(e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during CSV preview: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorPreview("Internal server error"));
        }
    }

    @PostMapping("/upload/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CsvUploadResult> confirmCsvUpload(
            @RequestBody List<CsvPreviewResponse.PreviewRow> validRows) {

        log.info("Processing confirmed CSV upload for {} rows", validRows.size());

        try {
            // Filter only valid rows for safety
            List<CsvPreviewResponse.PreviewRow> confirmedValidRows = validRows.stream()
                    .filter(CsvPreviewResponse.PreviewRow::isValid)
                    .toList();

            if (confirmedValidRows.isEmpty()) {
                CsvUploadResult emptyResult = new CsvUploadResult();
                emptyResult.setTotalRows(0);
                emptyResult.setSuccessCount(0);
                emptyResult.setFailureCount(0);
                emptyResult.setErrors(List.of("No valid rows to import"));
                return ResponseEntity.badRequest().body(emptyResult);
            }

            CsvUploadResult result = csvProcessingService.confirmCsvUpload(confirmedValidRows);

            if (result.isCompleteFailure()) {
                return ResponseEntity.badRequest().body(result);
            } else if (result.isPartialSuccess()) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
            } else {
                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            log.error("Error during CSV upload confirmation: {}", e.getMessage(), e);
            CsvUploadResult errorResult = new CsvUploadResult();
            errorResult.setTotalRows(validRows.size());
            errorResult.setSuccessCount(0);
            errorResult.setFailureCount(validRows.size());
            errorResult.setErrors(List.of("Internal server error: " + e.getMessage()));
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @GetMapping("/csv-template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getCsvTemplate() {
        log.info("Generating CSV template");

        String template = csvProcessingService.generateCsvTemplate();
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=flight_upload_template.csv")
                .body(template);
    }

    // Helper method for error responses
    private CsvPreviewResponse createErrorPreview(String errorMessage) {
        CsvPreviewResponse errorResponse = new CsvPreviewResponse();
        errorResponse.setTotalRows(0);
        errorResponse.setValidRows(0);
        errorResponse.setInvalidRows(0);
        errorResponse.setPreviewData(new ArrayList<>());
        errorResponse.setGlobalErrors(List.of(errorMessage));
        errorResponse.setReadyForImport(false);
        return errorResponse;
    }

    // ===============================
    // AKTARMALI UÇUŞ ENDPOİNTLERİ
    // ===============================

    @PostMapping("/connecting")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> createConnectingFlight(
            @Valid @RequestBody ConnectingFlightRequest request) {

        log.info("Creating connecting flight: {}", request.getMainFlightNumber());

        try {
            FlightResponse response = flightService.createConnectingFlight(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DuplicateResourceException e) {
            log.error("Duplicate connecting flight: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating connecting flight: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create connecting flight");
        }
    }

    @GetMapping("/connecting/{mainFlightId}")
    public ResponseEntity<FlightResponse> getConnectingFlightDetails(
            @PathVariable Long mainFlightId) {

        log.info("Getting connecting flight details: {}", mainFlightId);
        FlightResponse response = flightService.getConnectingFlightDetails(mainFlightId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{mainFlightId}/segments")
    public ResponseEntity<List<FlightResponse>> getFlightSegments(
            @PathVariable Long mainFlightId) {

        log.info("Getting flight segments for main flight: {}", mainFlightId);
        List<FlightResponse> segments = flightService.getConnectingFlights(mainFlightId);
        return ResponseEntity.ok(segments);
    }

    @PutMapping("/connecting/{mainFlightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> updateConnectingFlight(
            @PathVariable Long mainFlightId,
            @Valid @RequestBody ConnectingFlightRequest request) {

        log.info("Updating connecting flight: {}", mainFlightId);

        try {
            FlightResponse response = flightService.updateConnectingFlight(mainFlightId, request);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Connecting flight not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating connecting flight: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update connecting flight");
        }
    }

    @DeleteMapping("/connecting/{mainFlightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConnectingFlight(@PathVariable Long mainFlightId) {

        log.info("Deleting connecting flight: {}", mainFlightId);

        try {
            flightService.deleteConnectingFlight(mainFlightId);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            log.error("Connecting flight not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting connecting flight: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete connecting flight");
        }
    }

    @GetMapping("/connecting")
    public ResponseEntity<Page<FlightResponse>> getConnectingFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate) {

        log.info("Getting connecting flights - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        Page<FlightResponse> response = flightService.getConnectingFlightsWithFilters(
                pageable, airlineId, flightDate);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> updateFlightSegment(
            @PathVariable Long segmentId,
            @Valid @RequestBody FlightRequest request) {

        log.info("Updating flight segment: {}", segmentId);

        try {
            Flight segment = flightRepository.findById(segmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Flight segment not found"));

            if (segment.getParentFlightId() == null) {
                throw new RuntimeException("Flight is not a segment of connecting flight");
            }

            FlightResponse response = flightService.updateFlight(segmentId, request);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Flight segment not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating flight segment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update flight segment");
        }
    }

    @PatchMapping("/segments/{segmentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> updateSegmentStatus(
            @PathVariable Long segmentId,
            @RequestParam FlightStatus status,
            @RequestParam(required = false) String reason) {

        log.info("Updating segment status: {} to {}", segmentId, status);

        try {
            FlightResponse response = flightService.updateFlightStatus(segmentId, status);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Flight segment not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating segment status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update segment status");
        }
    }

    // ===============================
    // SİSTEM BİLGİ ENDPOİNTLERİ
    // ===============================

    @GetMapping("/system-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("systemType", "ROUTE_BASED");
        info.put("version", "2.0");
        info.put("description", "Route-based Flight Management System");
        info.put("legacyMigrationStatus", "COMPLETED");
        info.put("totalFlights", flightRepository.count());
        info.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/migration-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        Map<String, Object> status = flightService.getMigrationStatus();
        return ResponseEntity.ok(status);
    }

    // ===============================
    // BULK OPERATIONS
    // ===============================

    @PostMapping("/bulk-status-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkStatusUpdate(
            @RequestParam List<Long> flightIds,
            @RequestParam FlightStatus status,
            @RequestParam(required = false) String reason) {

        log.info("Bulk status update for {} flights to {}", flightIds.size(), status);

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long flightId : flightIds) {
            try {
                flightService.updateFlightStatus(flightId, status);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Flight " + flightId + ": " + e.getMessage());
            }
        }

        result.put("totalRequested", flightIds.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("errors", errors);
        result.put("status", failureCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL"));

        if (failureCount > 0) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @DeleteMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkDeleteFlights(
            @RequestParam List<Long> flightIds,
            @RequestParam(defaultValue = "false") boolean force) {

        log.info("Bulk delete for {} flights, force: {}", flightIds.size(), force);

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long flightId : flightIds) {
            try {
                // Check if flight can be deleted (not departed unless forced)
                if (!force) {
                    Flight flight = flightRepository.findById(flightId)
                            .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
                    if (flight.isDeparted()) {
                        throw new RuntimeException("Cannot delete departed flight without force flag");
                    }
                }

                flightService.deleteFlight(flightId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Flight " + flightId + ": " + e.getMessage());
            }
        }

        result.put("totalRequested", flightIds.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("errors", errors);
        result.put("status", failureCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL"));

        if (failureCount > 0) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    // ===============================
    // SEARCH ve FILTERING
    // ===============================

    @GetMapping("/search")
    public ResponseEntity<Page<FlightResponse>> searchFlights(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long airlineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) FlightStatus status,
            @RequestParam(required = false) FlightType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledDeparture") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Advanced flight search - query: {}, routeId: {}, airlineId: {}", query, routeId, airlineId);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        // For now, use basic filtering. Can be enhanced with advanced search logic
        Page<FlightResponse> flights = flightService.getAllFlightsWithFilters(
                pageable, query, airlineId, startDate);

        return ResponseEntity.ok(flights);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<FlightResponse>> filterFlights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) FlightStatus status,
            @RequestParam(required = false) FlightType type,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Boolean delayed,
            @RequestParam(required = false) Integer minDelayMinutes) {

        log.info("Filtering flights - date: {}, status: {}, routeId: {}", date, status, routeId);

        List<FlightResponse> flights;

        if (date != null) {
            flights = flightService.getFlightsByDate(date);
        } else if (status != null) {
            flights = flightService.getFlightsByStatus(status);
        } else if (routeId != null) {
            flights = flightService.getFlightsByRoute(routeId, null);
        } else if (Boolean.TRUE.equals(delayed)) {
            flights = flightService.getDelayedFlights(minDelayMinutes != null ? minDelayMinutes : 15);
        } else {
            // Default: return today's flights
            flights = flightService.getFlightsByDate(LocalDate.now());
        }

        // Apply additional filters if needed
        if (type != null) {
            flights = flights.stream()
                    .filter(f -> type.equals(f.getType()))
                    .collect(java.util.stream.Collectors.toList());
        }

        return ResponseEntity.ok(flights);
    }

    // ===============================
    // HELPER ENDPOİNTLER
    // ===============================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getFlightCounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();

        Map<String, Object> counts = new HashMap<>();
        counts.put("date", targetDate);
        counts.put("total", flightService.getFlightCountByDate(targetDate));
        counts.put("scheduled", flightService.getFlightCountByStatus(FlightStatus.SCHEDULED, targetDate));
        counts.put("departed", flightService.getFlightCountByStatus(FlightStatus.DEPARTED, targetDate));
        counts.put("arrived", flightService.getFlightCountByStatus(FlightStatus.ARRIVED, targetDate));
        counts.put("delayed", flightService.getFlightCountByStatus(FlightStatus.DELAYED, targetDate));
        counts.put("cancelled", flightService.getFlightCountByStatus(FlightStatus.CANCELLED, targetDate));

        return ResponseEntity.ok(counts);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Flight Service");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }


    // ===============================
    // VALIDATION HELPER METHODS
    // ===============================

    private void validateRouteBasedCreation(FlightRequest request, List<String> errors, List<String> warnings) {
        if (request.getRouteId() == null) {
            errors.add("Route ID is required for route-based creation");
            return;
        }

        try {
            RouteCache route = referenceDataService.getRoute(request.getRouteId());
            if (route == null) {
                errors.add("Selected route does not exist");
            } else if (!route.isActive()) {
                errors.add("Selected route is inactive");
            } else {
                // Add route info to warnings for user information
                warnings.add("Using route: " + route.getRouteCode() + " (" + route.getRouteName() + ")");

                if (route.isMultiSegmentRoute()) {
                    warnings.add("Selected route is multi-segment with " + route.getSegmentCount() + " segments");
                }

                // Distance/time warnings
                if (route.getDistance() != null && route.getDistance() > 5000) {
                    warnings.add("This is a long-haul route (" + route.getDistance() + " km)");
                }
            }
        } catch (Exception e) {
            errors.add("Failed to validate selected route: " + e.getMessage());
        }
    }

    private void validateAirportBasedCreation(FlightRequest request, List<String> errors, List<String> warnings) {
        if (request.getOriginAirportId() == null) {
            errors.add("Origin airport is required for airport-based creation");
        }
        if (request.getDestinationAirportId() == null) {
            errors.add("Destination airport is required for airport-based creation");
        }

        if (request.getOriginAirportId() != null && request.getDestinationAirportId() != null) {
            if (request.getOriginAirportId().equals(request.getDestinationAirportId())) {
                errors.add("Origin and destination airports cannot be the same");
                return;
            }

            try {
                var originAirport = referenceDataService.getAirport(request.getOriginAirportId());
                var destAirport = referenceDataService.getAirport(request.getDestinationAirportId());

                if (originAirport == null) {
                    errors.add("Origin airport does not exist");
                } else if (!originAirport.getActive()) {
                    errors.add("Origin airport is inactive");
                }

                if (destAirport == null) {
                    errors.add("Destination airport does not exist");
                } else if (!destAirport.getActive()) {
                    errors.add("Destination airport is inactive");
                }

                if (originAirport != null && destAirport != null &&
                        originAirport.getActive() && destAirport.getActive()) {

                    // Country-based warnings
                    String originCountry = originAirport.getCountry();
                    String destCountry = destAirport.getCountry();

                    if (originCountry != null && destCountry != null) {
                        if (!originCountry.equals(destCountry)) {
                            warnings.add("This appears to be an international flight");
                        } else {
                            warnings.add("This appears to be a domestic flight");
                        }
                    }

                    // Check if route already exists
                    try {
                        RouteCache existingRoute = autoRouteService.findExistingDirectRoute(
                                request.getOriginAirportId(), request.getDestinationAirportId());

                        if (existingRoute != null) {
                            warnings.add("Route already exists: " + existingRoute.getRouteCode());
                            warnings.add("Will use existing route instead of creating new one");
                        } else {
                            warnings.add("Will create new route for this airport pair");
                        }
                    } catch (Exception e) {
                        warnings.add("Could not check for existing route");
                    }
                }

            } catch (Exception e) {
                errors.add("Failed to validate airports: " + e.getMessage());
            }
        }
    }

    private void validateMultiSegmentCreation(FlightRequest request, List<String> errors, List<String> warnings) {
        if (request.getAirportSegments() == null || request.getAirportSegments().isEmpty()) {
            errors.add("Airport segments are required for multi-segment creation");
            return;
        }

        List<AirportSegmentRequest> segments = request.getAirportSegments();

        if (segments.size() < 2) {
            errors.add("Multi-segment creation requires at least 2 segments");
        }

        if (segments.size() > 10) {
            errors.add("Multi-segment creation cannot have more than 10 segments");
        }

        // Complexity warnings
        if (segments.size() > 5) {
            warnings.add("This is a complex multi-segment flight with " + segments.size() + " segments");
        }

        if (segments.size() > 3) {
            warnings.add("Consider using fewer segments for better operational efficiency");
        }

        // Validate each segment
        for (int i = 0; i < segments.size(); i++) {
            AirportSegmentRequest segment = segments.get(i);

            try {
                var originAirport = referenceDataService.getAirport(segment.getOriginAirportId());
                var destAirport = referenceDataService.getAirport(segment.getDestinationAirportId());

                if (originAirport == null) {
                    errors.add("Segment " + (i + 1) + ": Origin airport does not exist");
                } else if (!originAirport.getActive()) {
                    errors.add("Segment " + (i + 1) + ": Origin airport is inactive");
                }

                if (destAirport == null) {
                    errors.add("Segment " + (i + 1) + ": Destination airport does not exist");
                } else if (!destAirport.getActive()) {
                    errors.add("Segment " + (i + 1) + ": Destination airport is inactive");
                }

                // Connection time warnings
                if (segment.getConnectionTimeMinutes() != null) {
                    if (segment.getConnectionTimeMinutes() < 45) {
                        warnings.add("Segment " + (i + 1) + ": Tight connection time (" +
                                segment.getConnectionTimeMinutes() + " minutes)");
                    } else if (segment.getConnectionTimeMinutes() > 480) {
                        warnings.add("Segment " + (i + 1) + ": Very long connection time (" +
                                segment.getConnectionTimeMinutes() + " minutes)");
                    }
                }

            } catch (Exception e) {
                errors.add("Segment " + (i + 1) + ": Failed to validate airports");
            }
        }

        // Check if multi-segment route already exists
        try {
            RouteCache existingRoute = autoRouteService.findExistingMultiSegmentRoute(segments);
            if (existingRoute != null) {
                warnings.add("Multi-segment route already exists: " + existingRoute.getRouteCode());
                warnings.add("Will use existing route instead of creating new one");
            } else {
                warnings.add("Will create new multi-segment route");
            }
        } catch (Exception e) {
            warnings.add("Could not check for existing multi-segment route");
        }
    }

    private String getComplexityLevel(FlightRequest request) {
        if (request.getCreationMode() == null) {
            return "UNKNOWN";
        }

        switch (request.getCreationMode()) {
            case "ROUTE":
                return "SIMPLE";
            case "AIRPORTS":
                return "MODERATE";
            case "MULTI_AIRPORTS":
                int segmentCount = request.getAirportSegments() != null ? request.getAirportSegments().size() : 0;
                if (segmentCount <= 3) {
                    return "MODERATE";
                } else if (segmentCount <= 6) {
                    return "COMPLEX";
                } else {
                    return "VERY_COMPLEX";
                }
            default:
                return "UNKNOWN";
        }
    }
}