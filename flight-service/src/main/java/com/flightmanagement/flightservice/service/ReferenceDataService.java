package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataService {

    private final RestTemplate restTemplate;
    private final ServiceTokenManager serviceTokenManager;

    @Value("${reference-manager.base-url:http://localhost:8081}")
    private String referenceServiceUrl;

    @Cacheable("airlines")
    public AirlineCache getAirline(Long airlineId) {
        log.debug("Fetching airline data for ID: {}", airlineId);
        try {
            String url = referenceServiceUrl + "/api/v1/airlines/" + airlineId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<AirlineCache> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AirlineCache.class);

            if (response.getBody() != null) {
                log.debug("Airline {} retrieved successfully", airlineId);
                return response.getBody();
            }

            log.warn("Empty response for airline: {}", airlineId);
            return null;

        } catch (Exception e) {
            log.error("Error fetching airline {}: {}", airlineId, e.getMessage());
            throw new RuntimeException("Failed to fetch airline data: " + airlineId, e);
        }
    }

    @Cacheable("airports")
    public AirportCache getAirport(Long airportId) {
        log.debug("Fetching airport data for ID: {}", airportId);
        try {
            String url = referenceServiceUrl + "/api/v1/airports/" + airportId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, AirportCache.class).getBody();
        } catch (Exception e) {
            log.error("Error fetching airport {}: {}", airportId, e.getMessage());
            throw new RuntimeException("Failed to fetch airport data", e);
        }
    }

    /**
     * IATA koduna göre airport bulur
     */
    public AirportCache getAirportByIataCode(String iataCode) {
        log.debug("Getting airport by IATA code: {}", iataCode);

        if (iataCode == null || iataCode.trim().isEmpty()) {
            return null;
        }

        try {
            String url = referenceServiceUrl + "/api/v1/airports/iata/" + iataCode.trim().toUpperCase();
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<AirportCache> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AirportCache.class);

            AirportCache airport = response.getBody();
            log.debug("Found airport: {} for IATA code: {}",
                    airport != null ? airport.getName() : "null", iataCode);

            return airport;

        } catch (Exception e) {
            log.warn("Failed to get airport by IATA code {}: {}", iataCode, e.getMessage());
            return null;
        }
    }

    @Cacheable("aircraft")
    public AircraftCache getAircraft(Long aircraftId) {
        log.debug("Fetching aircraft data for ID: {}", aircraftId);
        try {
            String url = referenceServiceUrl + "/api/v1/aircrafts/" + aircraftId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, AircraftCache.class).getBody();
        } catch (Exception e) {
            log.error("Error fetching aircraft {}: {}", aircraftId, e.getMessage());
            throw new RuntimeException("Failed to fetch aircraft data", e);
        }
    }

    @Cacheable("routes")
    public RouteCache getRoute(Long routeId) {
        log.debug("Fetching route data for ID: {}", routeId);
        try {
            String url = referenceServiceUrl + "/api/v1/routes/" + routeId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, RouteCache.class).getBody();
        } catch (Exception e) {
            log.error("Error fetching route {}: {}", routeId, e.getMessage());
            throw new RuntimeException("Failed to fetch route data", e);
        }
    }

    // Batch operations for performance
    public RouteCache[] getRoutesByIds(Long[] routeIds) {
        log.debug("Fetching multiple routes: {}", routeIds.length);
        try {
            String url = referenceServiceUrl + "/api/v1/routes/batch";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Long[]> entity = new HttpEntity<>(routeIds, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, RouteCache[].class).getBody();
        } catch (Exception e) {
            log.error("Error fetching multiple routes: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch batch route data", e);
        }
    }

    public AirlineCache[] getAirlinesByIds(Long[] airlineIds) {
        log.debug("Fetching multiple airlines: {}", airlineIds.length);
        try {
            String url = referenceServiceUrl + "/api/v1/airlines/batch";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Long[]> entity = new HttpEntity<>(airlineIds, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, AirlineCache[].class).getBody();
        } catch (Exception e) {
            log.error("Error fetching multiple airlines: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch batch airline data", e);
        }
    }

    public AirportCache[] getAirportsByIds(Long[] airportIds) {
        log.debug("Fetching multiple airports: {}", airportIds.length);
        try {
            String url = referenceServiceUrl + "/api/v1/airports/batch";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Long[]> entity = new HttpEntity<>(airportIds, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, AirportCache[].class).getBody();
        } catch (Exception e) {
            log.error("Error fetching multiple airports: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch batch airport data", e);
        }
    }

    // Route specific methods
    public RouteCache[] getRoutesByAirline(Long airlineId) {
        log.debug("Fetching routes for airline: {}", airlineId);
        try {
            String url = referenceServiceUrl + "/api/v1/routes/airline/" + airlineId;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, RouteCache[].class).getBody();
        } catch (Exception e) {
            log.error("Error fetching routes for airline {}: {}", airlineId, e.getMessage());
            throw new RuntimeException("Failed to fetch airline routes", e);
        }
    }

    public RouteCache[] getActiveRoutes() {
        log.debug("Fetching all active routes");
        try {
            String url = referenceServiceUrl + "/api/v1/routes?active=true";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, RouteCache[].class).getBody();
        } catch (Exception e) {
            log.error("Error fetching active routes: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch active routes", e);
        }
    }

    // Cache invalidation methods
    public void invalidateAirlineCache(Long airlineId) {
        log.debug("Invalidating airline cache for ID: {}", airlineId);
        // Spring Cache eviction burada yapılacak
    }

    public void invalidateAirportCache(Long airportId) {
        log.debug("Invalidating airport cache for ID: {}", airportId);
        // Spring Cache eviction burada yapılacak
    }

    public void invalidateAircraftCache(Long aircraftId) {
        log.debug("Invalidating aircraft cache for ID: {}", aircraftId);
        // Spring Cache eviction burada yapılacak
    }

    public void invalidateRouteCache(Long routeId) {
        log.debug("Invalidating route cache for ID: {}", routeId);
        // Spring Cache eviction burada yapılacak
    }

    public void invalidateAllCaches() {
        log.info("Invalidating all reference data caches");
        // Tüm cache'leri temizle
    }

    // Health check
    public boolean isReferenceServiceHealthy() {
        try {
            String url = referenceServiceUrl + "/actuator/health";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Reference service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // Service discovery ve connection management
    public String getReferenceServiceInfo() {
        try {
            String url = referenceServiceUrl + "/actuator/info";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        } catch (Exception e) {
            log.warn("Could not get reference service info: {}", e.getMessage());
            return "Service info not available";
        }
    }

    // Private helper methods
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String token = serviceTokenManager.getServiceToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.setBearerAuth(token);
            log.debug("Added bearer token to request headers");
        } else {
            log.warn("No service token available for request");
        }

        return headers;
    }

    // Fallback methods for circuit breaker pattern
    public AirlineCache getAirlineFallback(Long airlineId) {
        log.warn("Using fallback for airline: {}", airlineId);
        AirlineCache fallback = new AirlineCache();
        fallback.setId(airlineId);
        fallback.setName("Unknown Airline");
        fallback.setIataCode("??");
        fallback.setActive(false);
        return fallback;
    }

    public AirportCache getAirportFallback(Long airportId) {
        log.warn("Using fallback for airport: {}", airportId);
        AirportCache fallback = new AirportCache();
        fallback.setId(airportId);
        fallback.setName("Unknown Airport");
        fallback.setIataCode("???");
        fallback.setActive(false);
        return fallback;
    }

    public RouteCache getRouteFallback(Long routeId) {
        log.warn("Using fallback for route: {}", routeId);
        RouteCache fallback = new RouteCache();
        fallback.setId(routeId);
        fallback.setRouteName("Unknown Route");
        fallback.setRouteCode("UNKNOWN");
        return fallback;
    }

    public AircraftCache getAircraftFallback(Long aircraftId) {
        log.warn("Using fallback for aircraft: {}", aircraftId);
        AircraftCache fallback = new AircraftCache();
        fallback.setId(aircraftId);
        fallback.setRegistrationNumber("UNKNOWN");
        fallback.setStatus("INACTIVE");
        return fallback;
    }
}