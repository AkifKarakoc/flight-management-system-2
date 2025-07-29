package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.request.FlightRequest;
import com.flightmanagement.flightservice.dto.response.CsvPreviewResponse;
import com.flightmanagement.flightservice.dto.response.CsvUploadResult;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.entity.enums.FlightType;
import com.flightmanagement.flightservice.exception.BusinessException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvProcessingService {

    private final FlightService flightService;
    private final ReferenceDataService referenceDataService;
    private final AutoRouteService autoRouteService;

    private static final String[] EXPECTED_HEADERS = {
            "flightNumber", "airlineId", "aircraftId", "route", "flightDate",
            "scheduledDeparture", "scheduledArrival", "type", "delayMinutes",
            "status", "passengerCount", "cargoWeight"
    };

    private static final Pattern IATA_ROUTE_PATTERN = Pattern.compile("^[A-Z]{3}-[A-Z]{3}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * CSV dosyasını preview için validate eder (DB'ye kaydetmez)
     */
    public CsvPreviewResponse previewCsvFile(MultipartFile file) {
        log.info("Starting CSV preview for file: {}", file.getOriginalFilename());

        CsvPreviewResponse response = new CsvPreviewResponse();
        List<CsvPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
        List<String> globalErrors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();

            if (records.isEmpty()) {
                throw new BusinessException("CSV file is empty");
            }

            // Header validation
            validateCsvHeaders(records.get(0));

            // Process each data row
            for (int i = 1; i < records.size(); i++) {
                String[] row = records.get(i);
                CsvPreviewResponse.PreviewRow previewRow = processRowForPreview(row, i + 1);
                previewRows.add(previewRow);
            }

        } catch (IOException | CsvException e) {
            throw new BusinessException("Error reading CSV file: " + e.getMessage());
        }

        // Calculate summary
        long validCount = previewRows.stream().mapToLong(row -> row.isValid() ? 1 : 0).sum();
        long invalidCount = previewRows.size() - validCount;

        response.setTotalRows(previewRows.size());
        response.setValidRows((int) validCount);
        response.setInvalidRows((int) invalidCount);
        response.setPreviewData(previewRows);
        response.setGlobalErrors(globalErrors);
        response.setReadyForImport(invalidCount == 0);

        log.info("CSV preview completed: {} total, {} valid, {} invalid",
                previewRows.size(), validCount, invalidCount);

        return response;
    }

    /**
     * Preview'dan onaylanan CSV'yi gerçekten import eder
     */
    public CsvUploadResult confirmCsvUpload(List<CsvPreviewResponse.PreviewRow> validRows) {
        log.info("Starting confirmed CSV upload for {} rows", validRows.size());

        CsvUploadResult result = new CsvUploadResult();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (CsvPreviewResponse.PreviewRow previewRow : validRows) {
            if (!previewRow.isValid()) {
                continue; // Skip invalid rows
            }

            try {
                FlightRequest flightRequest = convertPreviewRowToFlightRequest(previewRow);
                flightService.createFlight(flightRequest);
                successCount++;
                log.debug("Successfully imported flight: {}", flightRequest.getFlightNumber());
            } catch (Exception e) {
                String errorMsg = "Row " + previewRow.getRowNumber() + " (" +
                        previewRow.getParsedData().getFlightNumber() + "): " + e.getMessage();
                errors.add(errorMsg);
                log.warn("Failed to import row {}: {}", previewRow.getRowNumber(), e.getMessage());
            }
        }

        result.setTotalRows(validRows.size());
        result.setSuccessCount(successCount);
        result.setFailureCount(validRows.size() - successCount);
        result.setErrors(errors);

        log.info("CSV upload completed: {} total, {} success, {} failed",
                validRows.size(), successCount, result.getFailureCount());

        return result;
    }

    /**
     * CSV template generator with examples for both route types
     */
    public String generateCsvTemplate() {
        StringBuilder template = new StringBuilder();

        // Header
        template.append(String.join(",", EXPECTED_HEADERS)).append("\n");

        // Examples
        template.append("TK100,1,1,IST-ANK,2025-07-28,2025-07-28 08:00,2025-07-28 10:30,PASSENGER,0,SCHEDULED,180,\n");
        template.append("CG100,2,2,5,2025-07-28,2025-07-28 20:00,2025-07-29 02:30,CARGO,0,SCHEDULED,,2500\n");

        return template.toString();
    }

    // ============ PRIVATE HELPER METHODS ============

    private void validateCsvHeaders(String[] headers) {
        if (headers.length < EXPECTED_HEADERS.length) {
            throw new BusinessException("CSV file must have at least " + EXPECTED_HEADERS.length + " columns");
        }

        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                throw new BusinessException("Invalid header at column " + (i + 1) +
                        ". Expected: " + EXPECTED_HEADERS[i] + ", Found: " + headers[i]);
            }
        }
    }

    private CsvPreviewResponse.PreviewRow processRowForPreview(String[] row, int rowNumber) {
        CsvPreviewResponse.PreviewRow previewRow = new CsvPreviewResponse.PreviewRow();
        previewRow.setRowNumber(rowNumber);

        Map<String, String> fieldErrors = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        try {
            // Parse row data
            CsvPreviewResponse.ParsedFlightData parsedData = parseRowData(row, fieldErrors, warnings);

            // Route processing (key logic)
            processRouteField(parsedData, fieldErrors, warnings);

            previewRow.setParsedData(parsedData);
            previewRow.setFieldErrors(fieldErrors);
            previewRow.setWarnings(warnings);
            previewRow.setValid(fieldErrors.isEmpty());

        } catch (Exception e) {
            fieldErrors.put("general", e.getMessage());
            previewRow.setFieldErrors(fieldErrors);
            previewRow.setValid(false);
        }

        return previewRow;
    }

    private CsvPreviewResponse.ParsedFlightData parseRowData(String[] row,
                                                             Map<String, String> fieldErrors, List<String> warnings) {

        CsvPreviewResponse.ParsedFlightData data = new CsvPreviewResponse.ParsedFlightData();

        try {
            // Flight Number
            data.setFlightNumber(parseString(row[0], "Flight Number"));

            // Airline ID
            data.setAirlineId(parseLong(row[1], "Airline ID"));

            // Aircraft ID
            data.setAircraftId(parseLong(row[2], "Aircraft ID"));

            // Route (will be processed separately)
            data.setRouteInput(parseString(row[3], "Route"));

            // Flight Date
            data.setFlightDate(parseDate(row[4], "Flight Date"));

            // Scheduled times
            data.setScheduledDeparture(parseDateTime(row[5], "Scheduled Departure"));
            data.setScheduledArrival(parseDateTime(row[6], "Scheduled Arrival"));

            // Flight Type
            data.setType(parseFlightType(row[7], "Flight Type"));

            // Delay Minutes
            data.setDelayMinutes(parseInteger(row[8], "Delay Minutes"));

            // Status
            data.setStatus(parseFlightStatus(row[9], "Status"));

            // Passenger Count
            data.setPassengerCount(parseInteger(row[10], "Passenger Count"));

            // Cargo Weight
            data.setCargoWeight(parseInteger(row[11], "Cargo Weight"));

            // Validate time logic
            if (data.getScheduledDeparture() != null && data.getScheduledArrival() != null) {
                if (!data.getScheduledArrival().isAfter(data.getScheduledDeparture())) {
                    fieldErrors.put("scheduledArrival", "Arrival time must be after departure time");
                }
            }

            // Check for past dates
            if (data.getFlightDate() != null && data.getFlightDate().isBefore(LocalDate.now())) {
                warnings.add("Flight date is in the past");
            }

        } catch (Exception e) {
            fieldErrors.put("parsing", "Row parsing failed: " + e.getMessage());
        }

        return data;
    }

    private Integer parseInteger(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(fieldName + " must be a valid number");
        }
    }

    private FlightStatus parseFlightStatus(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return FlightStatus.SCHEDULED; // Default
        }
        try {
            return FlightStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(fieldName + " must be one of: SCHEDULED, DEPARTED, ARRIVED, CANCELLED, DELAYED");
        }
    }

    private void processRouteField(CsvPreviewResponse.ParsedFlightData data,
                                   Map<String, String> fieldErrors, List<String> warnings) {

        String routeInput = data.getRouteInput();
        if (routeInput == null || routeInput.trim().isEmpty()) {
            fieldErrors.put("route", "Route field is required");
            return;
        }

        routeInput = routeInput.trim();

        // Check if it's a numeric route ID
        if (isNumeric(routeInput)) {
            Long routeId = Long.parseLong(routeInput);

            // Validate route exists in database
            try {
                var route = referenceDataService.getRoute(routeId);
                if (route == null) {
                    fieldErrors.put("route", "Route ID " + routeId + " not found in database");
                    return;
                }

                data.setCreationMode("ROUTE");
                data.setRouteId(routeId);
                data.setRouteInfo(route.getRouteName());

                if (!route.getActive()) {
                    warnings.add("Selected route is inactive");
                }

            } catch (Exception e) {
                fieldErrors.put("route", "Error validating route ID: " + e.getMessage());
            }

        } else if (IATA_ROUTE_PATTERN.matcher(routeInput).matches()) {
            // IATA code format: IST-ANK
            String[] iataCodesPair = routeInput.split("-");
            String originIata = iataCodesPair[0];
            String destIata = iataCodesPair[1];

            // Validate IATA codes
            try {
                var originAirport = referenceDataService.getAirportByIataCode(originIata);
                var destAirport = referenceDataService.getAirportByIataCode(destIata);

                if (originAirport == null) {
                    fieldErrors.put("route", "Origin airport " + originIata + " not found");
                    return;
                }

                if (destAirport == null) {
                    fieldErrors.put("route", "Destination airport " + destIata + " not found");
                    return;
                }

                if (originIata.equals(destIata)) {
                    fieldErrors.put("route", "Origin and destination airports cannot be the same");
                    return;
                }

                data.setCreationMode("AIRPORTS");
                data.setOriginAirportId(originAirport.getId());
                data.setDestinationAirportId(destAirport.getId());
                data.setRouteInfo(originIata + " → " + destIata);

                if (!originAirport.getActive() || !destAirport.getActive()) {
                    warnings.add("One or more airports are inactive");
                }

            } catch (Exception e) {
                fieldErrors.put("route", "Error validating IATA codes: " + e.getMessage());
            }

        } else {
            fieldErrors.put("route", "Invalid route format. Use route ID (e.g., 5) or IATA codes (e.g., IST-ANK)");
        }
    }

    private FlightRequest convertPreviewRowToFlightRequest(CsvPreviewResponse.PreviewRow previewRow) {
        CsvPreviewResponse.ParsedFlightData data = previewRow.getParsedData();

        FlightRequest request = new FlightRequest();
        request.setFlightNumber(data.getFlightNumber());
        request.setAirlineId(data.getAirlineId());
        request.setAircraftId(data.getAircraftId());
        request.setFlightDate(data.getFlightDate());
        request.setScheduledDeparture(data.getScheduledDeparture());
        request.setScheduledArrival(data.getScheduledArrival());
        request.setType(data.getType());
        request.setStatus(FlightStatus.SCHEDULED);
        request.setActive(true);

        // Set creation mode and route info
        request.setCreationMode(data.getCreationMode());
        if ("ROUTE".equals(data.getCreationMode())) {
            request.setRouteId(data.getRouteId());
        } else if ("AIRPORTS".equals(data.getCreationMode())) {
            request.setOriginAirportId(data.getOriginAirportId());
            request.setDestinationAirportId(data.getDestinationAirportId());
        }

        return request;
    }

    // ============ PARSING UTILITIES ============

    private boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String parseString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in format YYYY-MM-DD");
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in format YYYY-MM-DD HH:mm");
        }
    }

    private FlightType parseFlightType(String value, String fieldName) {
        try {
            return FlightType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must be one of: PASSENGER, CARGO, MIXED");
        }
    }
}