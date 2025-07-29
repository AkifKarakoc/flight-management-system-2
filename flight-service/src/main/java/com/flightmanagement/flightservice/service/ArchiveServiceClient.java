package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.request.ArchiveSearchRequest;
import com.flightmanagement.flightservice.dto.response.ArchivedFlightResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveServiceClient {

    private final RestTemplate restTemplate;
    private final ServiceTokenManager serviceTokenManager;

    @Value("${archive-service.base-url:http://localhost:8083}")
    private String archiveServiceUrl;

    @Value("${archive-service.enabled:true}")
    private boolean archiveServiceEnabled;

    @CircuitBreaker(name = "archive-service", fallbackMethod = "archiveFlightFallback")
    @Retry(name = "archive-service")
    public boolean archiveFlight(Map<String, Object> flightData) {
        if (!archiveServiceEnabled) {
            log.debug("Archive service is disabled, skipping archive operation");
            return false;
        }

        log.debug("Archiving flight: {}", flightData.get("flightNumber"));

        try {
            String url = archiveServiceUrl + "/api/v1/archive/flights";
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> archiveRequest = createArchiveRequest(flightData);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(archiveRequest, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("Flight archived successfully: {}", flightData.get("flightNumber"));
            return success;

        } catch (Exception e) {
            log.error("Failed to archive flight {}: {}", flightData.get("flightNumber"), e.getMessage());
            throw new RuntimeException("Archive operation failed", e);
        }
    }

    @CircuitBreaker(name = "archive-service", fallbackMethod = "searchArchivedFlightsFallback")
    @Retry(name = "archive-service")
    public Page<ArchivedFlightResponse> searchArchivedFlights(ArchiveSearchRequest searchRequest, Pageable pageable) {
        if (!archiveServiceEnabled) {
            log.debug("Archive service is disabled, returning empty results");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        log.debug("Searching archived flights with criteria: {}", searchRequest);

        try {
            String url = archiveServiceUrl + "/api/v1/archive/search";
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = createSearchRequest(searchRequest, pageable);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseSearchResponse(response.getBody(), pageable);
            }

            return new PageImpl<>(Collections.emptyList(), pageable, 0);

        } catch (Exception e) {
            log.error("Failed to search archived flights: {}", e.getMessage());
            throw new RuntimeException("Archive search failed", e);
        }
    }

    @CircuitBreaker(name = "archive-service", fallbackMethod = "getArchivedFlightFallback")
    @Retry(name = "archive-service")
    public ArchivedFlightResponse getArchivedFlight(String flightNumber, LocalDate flightDate) {
        if (!archiveServiceEnabled) {
            log.debug("Archive service is disabled, returning null");
            return null;
        }

        log.debug("Getting archived flight: {} on {}", flightNumber, flightDate);

        try {
            String url = archiveServiceUrl + "/api/v1/archive/flights/" + flightNumber + "/" + flightDate;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseArchivedFlight(response.getBody());
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get archived flight {}: {}", flightNumber, e.getMessage());
            throw new RuntimeException("Get archived flight failed", e);
        }
    }

    @CircuitBreaker(name = "archive-service", fallbackMethod = "getArchiveStatsFallback")
    @Retry(name = "archive-service")
    public Map<String, Object> getArchiveStats(LocalDate startDate, LocalDate endDate) {
        if (!archiveServiceEnabled) {
            return Collections.emptyMap();
        }

        log.debug("Getting archive stats from {} to {}", startDate, endDate);

        try {
            String url = archiveServiceUrl + "/api/v1/archive/stats?startDate=" + startDate + "&endDate=" + endDate;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            return Collections.emptyMap();

        } catch (Exception e) {
            log.error("Failed to get archive stats: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isArchiveServiceHealthy() {
        if (!archiveServiceEnabled) {
            return false;
        }

        try {
            String url = archiveServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Archive service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // Fallback methods
    public boolean archiveFlightFallback(Map<String, Object> flightData, Exception ex) {
        log.warn("Archive fallback for flight {}: {}", flightData.get("flightNumber"), ex.getMessage());
        // Could implement local queueing or retry mechanism here
        return false;
    }

    public Page<ArchivedFlightResponse> searchArchivedFlightsFallback(ArchiveSearchRequest searchRequest, Pageable pageable, Exception ex) {
        log.warn("Search archived flights fallback: {}", ex.getMessage());
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    public ArchivedFlightResponse getArchivedFlightFallback(String flightNumber, LocalDate flightDate, Exception ex) {
        log.warn("Get archived flight fallback for {} on {}: {}", flightNumber, flightDate, ex.getMessage());
        return null;
    }

    public Map<String, Object> getArchiveStatsFallback(LocalDate startDate, LocalDate endDate, Exception ex) {
        log.warn("Archive stats fallback: {}", ex.getMessage());
        return Collections.emptyMap();
    }

    // Helper methods
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String token = serviceTokenManager.getServiceToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private Map<String, Object> createArchiveRequest(Map<String, Object> flightData) {
        Map<String, Object> archiveRequest = new HashMap<>();

        // Required fields for archive
        archiveRequest.put("flightId", flightData.get("id"));
        archiveRequest.put("flightNumber", flightData.get("flightNumber"));
        archiveRequest.put("flightDate", flightData.get("flightDate"));
        archiveRequest.put("eventType", "FLIGHT_COMPLETED");
        archiveRequest.put("eventTime", LocalDateTime.now());

        // Full flight data as payload
        archiveRequest.put("payload", flightData);

        // Archive metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("archivedBy", "flight-service");
        metadata.put("archiveReason", "Flight completed");
        metadata.put("sourceService", "flight-service");
        metadata.put("dataVersion", "2.0");
        archiveRequest.put("metadata", metadata);

        return archiveRequest;
    }

    private Map<String, Object> createSearchRequest(ArchiveSearchRequest searchRequest, Pageable pageable) {
        Map<String, Object> request = new HashMap<>();

        // Search criteria
        if (searchRequest.getFlightNumber() != null) {
            request.put("flightNumber", searchRequest.getFlightNumber());
        }
        if (searchRequest.getAirlineId() != null) {
            request.put("airlineId", searchRequest.getAirlineId());
        }
        if (searchRequest.getStartDate() != null) {
            request.put("startDate", searchRequest.getStartDate());
        }
        if (searchRequest.getEndDate() != null) {
            request.put("endDate", searchRequest.getEndDate());
        }
        if (searchRequest.getStatus() != null) {
            request.put("status", searchRequest.getStatus());
        }

        // Pagination
        request.put("page", pageable.getPageNumber());
        request.put("size", pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            List<String> sortOrders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                sortOrders.add(order.getProperty() + "," + order.getDirection().name().toLowerCase());
            });
            request.put("sort", sortOrders);
        }

        return request;
    }

    @SuppressWarnings("unchecked")
    private Page<ArchivedFlightResponse> parseSearchResponse(Map<String, Object> responseBody, Pageable pageable) {
        List<ArchivedFlightResponse> flights = new ArrayList<>();

        if (responseBody.containsKey("content")) {
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
            for (Map<String, Object> flightData : content) {
                ArchivedFlightResponse flight = parseArchivedFlight(flightData);
                if (flight != null) {
                    flights.add(flight);
                }
            }
        }

        long totalElements = 0;
        if (responseBody.containsKey("totalElements")) {
            totalElements = ((Number) responseBody.get("totalElements")).longValue();
        }

        return new PageImpl<>(flights, pageable, totalElements);
    }

    @SuppressWarnings("unchecked")
    private ArchivedFlightResponse parseArchivedFlight(Map<String, Object> flightData) {
        try {
            ArchivedFlightResponse flight = new ArchivedFlightResponse();

            flight.setId(getLongValue(flightData, "id"));
            flight.setFlightNumber(getStringValue(flightData, "flightNumber"));
            flight.setFlightDate(getDateValue(flightData, "flightDate"));
            flight.setStatus(getStringValue(flightData, "status"));
            flight.setArchivedAt(getDateTimeValue(flightData, "archivedAt"));
            flight.setEventType(getStringValue(flightData, "eventType"));

            // Parse payload if exists
            if (flightData.containsKey("payload")) {
                Map<String, Object> payload = (Map<String, Object>) flightData.get("payload");
                flight.setPayload(payload);

                // Extract additional fields from payload
                if (payload.containsKey("airline")) {
                    Map<String, Object> airline = (Map<String, Object>) payload.get("airline");
                    flight.setAirlineName(getStringValue(airline, "name"));
                }

                if (payload.containsKey("originAirport")) {
                    Map<String, Object> origin = (Map<String, Object>) payload.get("originAirport");
                    flight.setOriginAirportCode(getStringValue(origin, "iataCode"));
                }

                if (payload.containsKey("destinationAirport")) {
                    Map<String, Object> dest = (Map<String, Object>) payload.get("destinationAirport");
                    flight.setDestinationAirportCode(getStringValue(dest, "iataCode"));
                }
            }

            return flight;
        } catch (Exception e) {
            log.error("Failed to parse archived flight data: {}", e.getMessage());
            return null;
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private LocalDate getDateValue(Map<String, Object> map, String key) {
        String value = getStringValue(map, key);
        if (value != null) {
            try {
                return LocalDate.parse(value);
            } catch (Exception e) {
                log.warn("Failed to parse date value: {}", value);
            }
        }
        return null;
    }

    private LocalDateTime getDateTimeValue(Map<String, Object> map, String key) {
        String value = getStringValue(map, key);
        if (value != null) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception e) {
                log.warn("Failed to parse datetime value: {}", value);
            }
        }
        return null;
    }
}