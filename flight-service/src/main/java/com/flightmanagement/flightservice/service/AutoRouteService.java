package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.dto.request.AirportSegmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoRouteService {

    private final RestTemplate restTemplate;
    private final ReferenceDataService referenceDataService;
    private final ServiceTokenManager serviceTokenManager;

    @Value("${reference-manager.base-url}")
    private String referenceServiceUrl;

    /**
     * Single segment için route bulur veya oluşturur
     */
    public Long findOrCreateDirectRoute(Long originAirportId, Long destinationAirportId) {
        log.debug("Finding or creating direct route for {} -> {}", originAirportId, destinationAirportId);

        try {
            // 1. Önce exact match ara
            RouteCache existingRoute = findExactDirectRoute(originAirportId, destinationAirportId);
            if (existingRoute != null) {
                log.debug("Found existing direct route: {} ({})", existingRoute.getRouteCode(), existingRoute.getId());
                return existingRoute.getId();
            }

            // 2. Bulunamazsa yeni oluştur
            return createNewDirectRoute(originAirportId, destinationAirportId);

        } catch (Exception e) {
            log.error("Error finding/creating direct route for {} -> {}: {}",
                    originAirportId, destinationAirportId, e.getMessage());
            throw new RuntimeException("Failed to find or create direct route", e);
        }
    }

    /**
     * PUBLIC: Mevcut direct route bulma (FlightService için)
     */
    public RouteCache findExistingDirectRoute(Long originAirportId, Long destinationAirportId) {
        log.debug("Finding existing direct route for {} -> {}", originAirportId, destinationAirportId);
        return findExactDirectRoute(originAirportId, destinationAirportId);
    }

    /**
     * PUBLIC: Mevcut multi-segment route bulma (FlightService için)
     */
    public RouteCache findExistingMultiSegmentRoute(List<AirportSegmentRequest> segments) {
        log.debug("Finding existing multi-segment route for {} segments", segments.size());
        return findExactMultiSegmentRoute(segments);
    }

    /**
     * Multi-segment için route bulur veya oluşturur
     */
    public Long findOrCreateMultiSegmentRoute(List<AirportSegmentRequest> airportSegments, String mainFlightNumber) {
        log.debug("Finding or creating multi-segment route for {} segments", airportSegments.size());

        try {
            // 1. Önce exact match ara
            RouteCache existingRoute = findExactMultiSegmentRoute(airportSegments);
            if (existingRoute != null) {
                log.debug("Found existing multi-segment route: {} ({})", existingRoute.getRouteCode(), existingRoute.getId());
                return existingRoute.getId();
            }

            // 2. Bulunamazsa yeni oluştur
            return createNewMultiSegmentRoute(airportSegments, mainFlightNumber);

        } catch (Exception e) {
            log.error("Error finding/creating multi-segment route: {}", e.getMessage());
            throw new RuntimeException("Failed to find or create multi-segment route", e);
        }
    }

    private RouteCache findExactDirectRoute(Long originAirportId, Long destinationAirportId) {
        try {
            RouteCache[] existingRoutes = referenceDataService.getActiveRoutes();

            if (existingRoutes == null || existingRoutes.length == 0) {
                log.debug("No active routes found in system");
                return null;
            }

            for (RouteCache route : existingRoutes) {
                if (isExactDirectMatch(route, originAirportId, destinationAirportId)) {
                    log.debug("Found exact direct route match: {} ({})", route.getRouteCode(), route.getId());
                    return route;
                }
            }

            log.debug("No direct route found for {} -> {}", originAirportId, destinationAirportId);
            return null;

        } catch (Exception e) {
            log.warn("Error searching existing direct routes: {}", e.getMessage());
            return null;
        }
    }

    private RouteCache findExactMultiSegmentRoute(List<AirportSegmentRequest> segments) {
        try {
            RouteCache[] existingRoutes = referenceDataService.getActiveRoutes();

            if (existingRoutes == null || existingRoutes.length == 0) {
                log.debug("No active routes found in system");
                return null;
            }

            for (RouteCache route : existingRoutes) {
                if (isValidMultiSegmentRoute(route) && isExactMultiSegmentMatch(route, segments)) {
                    log.debug("Found exact multi-segment route match: {} ({})", route.getRouteCode(), route.getId());
                    return route;
                }
            }

            log.debug("No multi-segment route found for {} segments", segments.size());
            return null;

        } catch (Exception e) {
            log.warn("Error searching existing multi-segment routes: {}", e.getMessage());
            return null;
        }
    }

    private boolean isExactDirectMatch(RouteCache route, Long originId, Long destId) {
        return isValidAndActiveRoute(route) &&
                route.getOriginAirportId().equals(originId) &&
                route.getDestinationAirportId().equals(destId) &&
                !route.isMultiSegmentRoute(); // Tek segment olmalı
    }

    /**
     * Multi-segment route exact match kontrolü
     */
    private boolean isExactMultiSegmentMatch(RouteCache route, List<AirportSegmentRequest> segments) {
        if (!route.isMultiSegmentRoute() ||
                route.getSegmentCount() == null ||
                route.getSegmentCount() != segments.size()) {
            return false;
        }

        try {
            // Reference Manager'dan route segments'leri al ve karşılaştır
            Object[] routeSegments = getRouteSegmentsFromReferenceManager(route.getId());

            if (routeSegments.length != segments.size()) {
                return false;
            }

            // Her segment'i sırasıyla karşılaştır
            for (int i = 0; i < segments.size(); i++) {
                AirportSegmentRequest requestSegment = segments.get(i);
                Object routeSegment = routeSegments[i];

                // Origin ve destination airport ID'lerini karşılaştır
                Long routeOriginId = getSegmentOriginId(routeSegment);
                Long routeDestinationId = getSegmentDestinationId(routeSegment);

                if (routeOriginId == null || routeDestinationId == null) {
                    log.warn("Invalid route segment data for route {} segment {}", route.getId(), i + 1);
                    return false;
                }

                if (!requestSegment.getOriginAirportId().equals(routeOriginId) ||
                        !requestSegment.getDestinationAirportId().equals(routeDestinationId)) {
                    return false;
                }

                // Segment order kontrolü
                Integer segmentOrder = getSegmentOrder(routeSegment);
                if (segmentOrder == null || !segmentOrder.equals(requestSegment.getSegmentOrder())) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.warn("Error comparing route segments for route {}: {}", route.getId(), e.getMessage());
            return false;
        }
    }

    private Long createNewDirectRoute(Long originAirportId, Long destinationAirportId) {
        try {
            AirportCache originAirport = referenceDataService.getAirport(originAirportId);
            AirportCache destinationAirport = referenceDataService.getAirport(destinationAirportId);

            if (originAirport == null || destinationAirport == null) {
                throw new RuntimeException("Invalid airport IDs provided");
            }

            if (!originAirport.getActive() || !destinationAirport.getActive()) {
                throw new RuntimeException("One or both airports are inactive");
            }

            Map<String, Object> routeData = buildDirectRouteData(originAirport, destinationAirport);
            return createRouteWithErrorHandling(routeData, "direct");

        } catch (Exception e) {
            log.error("Error creating new direct route: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create new direct route", e);
        }
    }

    private Long createNewMultiSegmentRoute(List<AirportSegmentRequest> airportSegments, String mainFlightNumber) {
        try {
            // Validate all airports exist and are active
            for (int i = 0; i < airportSegments.size(); i++) {
                AirportSegmentRequest segment = airportSegments.get(i);

                AirportCache origin = referenceDataService.getAirport(segment.getOriginAirportId());
                AirportCache destination = referenceDataService.getAirport(segment.getDestinationAirportId());

                if (origin == null || destination == null) {
                    throw new RuntimeException("Invalid airport IDs in segment " + (i + 1));
                }

                if (!origin.getActive() || !destination.getActive()) {
                    throw new RuntimeException("Inactive airport in segment " + (i + 1));
                }
            }

            Map<String, Object> routeData = buildMultiSegmentRouteData(airportSegments, mainFlightNumber);
            return createRouteWithErrorHandling(routeData, "multi-segment");

        } catch (Exception e) {
            log.error("Error creating new multi-segment route: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create new multi-segment route", e);
        }
    }

    private Map<String, Object> buildDirectRouteData(AirportCache originAirport, AirportCache destinationAirport) {
        Map<String, Object> routeData = new HashMap<>();
        routeData.put("originAirportId", originAirport.getId());
        routeData.put("destinationAirportId", destinationAirport.getId());
        routeData.put("routeName", originAirport.getName() + " to " + destinationAirport.getName());
        routeData.put("routeCode", generateDirectRouteCode(originAirport.getIataCode(), destinationAirport.getIataCode()));
        routeData.put("routeType", determineRouteType(originAirport.getCountry(), destinationAirport.getCountry()));
        routeData.put("isMultiSegment", false);
        routeData.put("active", true);

        // Calculate distance and flight time
        Integer distance = calculateEstimatedDistance(originAirport, destinationAirport);
        routeData.put("distance", distance);
        routeData.put("estimatedFlightTime", calculateEstimatedFlightTime(distance));

        // Direct route için tek segment ekle - BURAYI EKLEDİM
        List<Map<String, Object>> segments = new ArrayList<>();
        Map<String, Object> segment = new HashMap<>();
        segment.put("segmentOrder", 1);
        segment.put("originAirportId", originAirport.getId());
        segment.put("destinationAirportId", destinationAirport.getId());
        segment.put("distance", distance);
        segment.put("estimatedFlightTime", calculateEstimatedFlightTime(distance));
        segment.put("active", true);
        segments.add(segment);

        routeData.put("segments", segments);

        return routeData;
    }

    private Map<String, Object> buildMultiSegmentRouteData(List<AirportSegmentRequest> airportSegments, String mainFlightNumber) {
        Map<String, Object> routeData = new HashMap<>();

        AirportCache firstSegmentOrigin = referenceDataService.getAirport(airportSegments.get(0).getOriginAirportId());
        AirportCache lastSegmentDestination = referenceDataService.getAirport(airportSegments.get(airportSegments.size() - 1).getDestinationAirportId());

        routeData.put("originAirportId", firstSegmentOrigin.getId());
        routeData.put("destinationAirportId", lastSegmentDestination.getId());
        routeData.put("routeName", mainFlightNumber + " Multi-Segment Route");
        routeData.put("routeCode", generateMultiSegmentRouteCode(firstSegmentOrigin.getIataCode(),
                lastSegmentDestination.getIataCode(), airportSegments.size()));
        routeData.put("routeType", determineMultiSegmentRouteType(airportSegments));
        routeData.put("isMultiSegment", true);
        routeData.put("segmentCount", airportSegments.size());
        routeData.put("active", true);

        // Total distance and flight time calculation
        routeData.put("distance", calculateTotalDistance(airportSegments));
        routeData.put("estimatedFlightTime", calculateTotalFlightTime(airportSegments));

        // Segments data
        List<Map<String, Object>> segmentsData = new ArrayList<>();
        for (AirportSegmentRequest segment : airportSegments) {
            Map<String, Object> segmentData = new HashMap<>();
            segmentData.put("originAirportId", segment.getOriginAirportId());
            segmentData.put("destinationAirportId", segment.getDestinationAirportId());
            segmentData.put("segmentOrder", segment.getSegmentOrder());
            segmentData.put("connectionTimeMinutes", segment.getConnectionTimeMinutes());
            segmentsData.add(segmentData);
        }
        routeData.put("segments", segmentsData);

        return routeData;
    }

    private Long createRouteInReferenceManager(Map<String, Object> routeData) {
        String url = referenceServiceUrl + "/api/v1/routes";
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(routeData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("id");
                if (id != null) {
                    return ((Number) id).longValue();
                }
            }
            throw new RuntimeException("Failed to get ID from reference manager response");
        } catch (Exception e) {
            log.error("Error creating route in reference manager: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create route in reference manager", e);
        }
    }

    private String generateDirectRouteCode(String originCode, String destCode) {
        long timestamp = System.currentTimeMillis() % 10000;
        return String.format("%s-%s-D%d", originCode, destCode, timestamp);
    }

    private String generateMultiSegmentRouteCode(String originCode, String destCode, int segmentCount) {
        long timestamp = System.currentTimeMillis() % 10000;
        return String.format("%s-%s-M%d-%d", originCode, destCode, segmentCount, timestamp);
    }

    private String determineRouteType(String originCountry, String destCountry) {
        if (originCountry != null && destCountry != null && originCountry.equals(destCountry)) {
            return "DOMESTIC";
        } else {
            return "INTERNATIONAL";
        }
    }

    private String determineMultiSegmentRouteType(List<AirportSegmentRequest> segments) {
        if (segments.isEmpty()) {
            return "UNKNOWN";
        }
        String firstCountry = referenceDataService.getAirport(segments.get(0).getOriginAirportId()).getCountry();
        String lastCountry = referenceDataService.getAirport(segments.get(segments.size() - 1).getDestinationAirportId()).getCountry();

        for (AirportSegmentRequest segment : segments) {
            String originCountry = referenceDataService.getAirport(segment.getOriginAirportId()).getCountry();
            String destCountry = referenceDataService.getAirport(segment.getDestinationAirportId()).getCountry();
            if (!originCountry.equals(destCountry)) {
                return "INTERNATIONAL";
            }
        }
        return firstCountry.equals(lastCountry) ? "DOMESTIC" : "INTERNATIONAL";
    }

    private Integer calculateTotalDistance(List<AirportSegmentRequest> segments) {
        int totalDistance = 0;
        for (AirportSegmentRequest segment : segments) {
            AirportCache origin = referenceDataService.getAirport(segment.getOriginAirportId());
            AirportCache dest = referenceDataService.getAirport(segment.getDestinationAirportId());
            if (origin != null && dest != null) {
                totalDistance += calculateEstimatedDistance(origin, dest);
            }
        }
        return totalDistance;
    }

    private Integer calculateTotalFlightTime(List<AirportSegmentRequest> segments) {
        int totalTime = 0;
        for (AirportSegmentRequest segment : segments) {
            AirportCache origin = referenceDataService.getAirport(segment.getOriginAirportId());
            AirportCache dest = referenceDataService.getAirport(segment.getDestinationAirportId());
            if (origin != null && dest != null) {
                totalTime += calculateEstimatedFlightTime(calculateEstimatedDistance(origin, dest));
            }
            if (segment.getConnectionTimeMinutes() != null) {
                totalTime += segment.getConnectionTimeMinutes();
            }
        }
        return totalTime;
    }

    private Integer calculateEstimatedDistance(AirportCache origin, AirportCache destination) {
        if (origin.getLatitude() != null && origin.getLongitude() != null &&
                destination.getLatitude() != null && destination.getLongitude() != null) {
            return calculateHaversineDistance(origin.getLatitude(), origin.getLongitude(),
                    destination.getLatitude(), destination.getLongitude());
        }
        return 500; // Fallback
    }

    private Integer calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return (int) distance;
    }

    private Integer calculateEstimatedFlightTime(Integer distance) {
        if (distance == null) return 0;
        return (int) Math.ceil(distance / 800.0 * 60); // 800 km/h avg speed
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceTokenManager.getServiceToken());
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public Map<String, Object> previewMultiSegmentRoute(List<AirportSegmentRequest> segments) {
        Map<String, Object> preview = new HashMap<>();
        try {
            List<Map<String, Object>> segmentPreviews = new ArrayList<>();
            int totalDistance = 0;
            int totalTime = 0;

            for (AirportSegmentRequest segment : segments) {
                AirportCache origin = referenceDataService.getAirport(segment.getOriginAirportId());
                AirportCache dest = referenceDataService.getAirport(segment.getDestinationAirportId());
                if (origin != null && dest != null) {
                    Map<String, Object> segmentPreview = new HashMap<>();
                    segmentPreview.put("origin", origin);
                    segmentPreview.put("destination", dest);
                    int distance = calculateEstimatedDistance(origin, dest);
                    int time = calculateEstimatedFlightTime(distance);
                    segmentPreview.put("distance", distance);
                    segmentPreview.put("estimatedTime", time);
                    segmentPreviews.add(segmentPreview);
                    totalDistance += distance;
                    totalTime += time + (segment.getConnectionTimeMinutes() != null ? segment.getConnectionTimeMinutes() : 0);
                }
            }
            preview.put("segments", segmentPreviews);
            preview.put("totalDistance", totalDistance);
            preview.put("totalEstimatedTime", totalTime);
            preview.put("routeType", determineMultiSegmentRouteType(segments));
        } catch (Exception e) {
            log.error("Error creating multi-segment route preview: {}", e.getMessage());
            preview.put("error", "Failed to create preview: " + e.getMessage());
        }
        return preview;
    }

    /**
     * Reference Manager'dan route segments'leri alır
     */
    private Object[] getRouteSegmentsFromReferenceManager(Long routeId) {
        try {
            String url = referenceServiceUrl + "/api/v1/routes/" + routeId + "/segments";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(url, HttpMethod.GET, request, Object[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Retrieved {} segments for route {}", response.getBody().length, routeId);
                return response.getBody();
            }

            log.warn("No segments found for route {}", routeId);
            return new Object[0];

        } catch (Exception e) {
            log.warn("Error fetching route segments for route {}: {}", routeId, e.getMessage());
            return new Object[0];
        }
    }

    /**
     * Route segment'ten origin airport ID'yi alır
     */
    private Long getSegmentOriginId(Object segment) {
        try {
            if (segment instanceof Map) {
                Map<String, Object> segmentMap = (Map<String, Object>) segment;
                Object originId = segmentMap.get("originAirportId");
                return originId != null ? ((Number) originId).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("Error extracting origin airport ID from segment: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Route segment'ten destination airport ID'yi alır
     */
    private Long getSegmentDestinationId(Object segment) {
        try {
            if (segment instanceof Map) {
                Map<String, Object> segmentMap = (Map<String, Object>) segment;
                Object destinationId = segmentMap.get("destinationAirportId");
                return destinationId != null ? ((Number) destinationId).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("Error extracting destination airport ID from segment: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Route segment'ten segment order'ı alır
     */
    private Integer getSegmentOrder(Object segment) {
        try {
            if (segment instanceof Map) {
                Map<String, Object> segmentMap = (Map<String, Object>) segment;
                Object segmentOrder = segmentMap.get("segmentOrder");
                return segmentOrder != null ? ((Number) segmentOrder).intValue() : null;
            }
        } catch (Exception e) {
            log.warn("Error extracting segment order from segment: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Route'un aktif ve geçerli olduğunu kontrol eder
     */
    private boolean isValidAndActiveRoute(RouteCache route) {
        return route != null &&
                route.isActive() &&
                route.isValid() &&
                route.getOriginAirportId() != null &&
                route.getDestinationAirportId() != null;
    }

    /**
     * Multi-segment route validation
     */
    private boolean isValidMultiSegmentRoute(RouteCache route) {
        return isValidAndActiveRoute(route) &&
                route.isMultiSegmentRoute() &&
                route.getSegmentCount() != null &&
                route.getSegmentCount() >= 2;
    }

    /**
     * Route creation error handling wrapper
     */
    private Long createRouteWithErrorHandling(Map<String, Object> routeData, String routeType) {
        try {
            Long routeId = createRouteInReferenceManager(routeData);
            log.info("Successfully created {} route with ID: {}", routeType, routeId);
            return routeId;

        } catch (Exception e) {
            log.error("Failed to create {} route: {}", routeType, e.getMessage());
            throw new RuntimeException("Failed to create " + routeType + " route: " + e.getMessage(), e);
        }
    }
}