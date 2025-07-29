package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.dto.request.RouteRequest;
import com.flightmanagement.referencemanagerservice.dto.request.RouteSegmentRequest;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.dto.response.RouteResponse;
import com.flightmanagement.referencemanagerservice.entity.Airport;
import com.flightmanagement.referencemanagerservice.entity.Route;
import com.flightmanagement.referencemanagerservice.entity.RouteSegment;
import com.flightmanagement.referencemanagerservice.entity.enums.RouteVisibility;
import com.flightmanagement.referencemanagerservice.exception.ResourceNotFoundException;
import com.flightmanagement.referencemanagerservice.exception.BusinessException;
import com.flightmanagement.referencemanagerservice.exception.DuplicateResourceException;
import com.flightmanagement.referencemanagerservice.mapper.RouteMapper;
import com.flightmanagement.referencemanagerservice.repository.AirportRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteSegmentRepository;
import com.flightmanagement.referencemanagerservice.validator.RouteDeletionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RouteService {
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final AirportRepository airportRepository;
    private final RouteMapper routeMapper;
    private final KafkaProducerService kafkaProducerService;
    private final RouteDeletionValidator deletionValidator;

    // YENİ: Paginated metodlar
    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        log.debug("Admin fetching all routes with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<Route> routePage = routeRepository.findAllForAdmin(pageable);
        return routePage.map(routeMapper::toResponse);
    }

    public Page<RouteResponse> getRoutesForUser(Long userId, boolean isAdmin, Pageable pageable) {
        log.debug("Fetching routes for user: {}, isAdmin: {}, page={}, size={}",
                userId, isAdmin, pageable.getPageNumber(), pageable.getPageSize());

        Page<Route> routePage;
        if (isAdmin) {
            routePage = routeRepository.findAllForAdmin(pageable);
        } else {
            routePage = routeRepository.findVisibleRoutesForUser(userId, pageable);
        }

        return routePage.map(routeMapper::toResponse);
    }

    public Page<RouteResponse> getUserRoutes(Long userId, Pageable pageable) {
        log.debug("Fetching user's own routes for user: {}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<Route> routePage = routeRepository.findByCreatedByUserId(userId, pageable);
        return routePage.map(routeMapper::toResponse);
    }

    public Page<RouteResponse> getSharedRoutesForAirline(Long airlineId, Pageable pageable) {
        log.debug("Fetching shared routes for airline: {}, page={}, size={}",
                airlineId, pageable.getPageNumber(), pageable.getPageSize());
        Page<Route> routePage = routeRepository.findSharedRoutesForAirline(airlineId, pageable);
        return routePage.map(routeMapper::toResponse);
    }

    // ESKİ: Non-paginated metodlar (backward compatibility için)
    public List<RouteResponse> getAllRoutes() {
        log.debug("Admin fetching all routes");
        return routeRepository.findAllForAdmin().stream()
                .map(routeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getRoutesForUser(Long userId, boolean isAdmin) {
        log.debug("Fetching routes for user: {}, isAdmin: {}", userId, isAdmin);

        List<Route> routes;
        if (isAdmin) {
            routes = routeRepository.findAllForAdmin();
        } else {
            routes = routeRepository.findVisibleRoutesForUser(userId);
        }

        return routes.stream()
                .map(routeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getUserRoutes(Long userId) {
        log.debug("Fetching user's own routes for user: {}", userId);
        return routeRepository.findByCreatedByUserId(userId).stream()
                .map(routeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getSharedRoutesForAirline(Long airlineId) {
        log.debug("Fetching shared routes for airline: {}", airlineId);
        return routeRepository.findSharedRoutesForAirline(airlineId).stream()
                .map(routeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RouteResponse getRouteById(Long id) {
        log.debug("Fetching route with id: {}", id);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        return routeMapper.toResponse(route);
    }

    public DeletionCheckResult checkRouteDeletion(Long id) {
        log.debug("Checking deletion dependencies for route with id: {}", id);

        routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        return deletionValidator.checkDependencies(id);
    }

    public RouteResponse createRoute(RouteRequest request, Long currentUserId, boolean isAdmin) {
        log.debug("Creating new route: {} by user: {}", request.getRouteCode(), currentUserId);

        // Validasyonlar
        validateRouteRequest(request);

        // Route code unique kontrolü
        if (routeRepository.existsByRouteCode(request.getRouteCode())) {
            throw new DuplicateResourceException("Route with code '" + request.getRouteCode() + "' already exists");
        }

        // Route oluştur
        Route route = new Route();
        route.setRouteCode(request.getRouteCode());
        route.setRouteName(request.getRouteName());
        route.setRouteType(request.getRouteType());
        route.setActive(request.getActive());
        route.setCreatedByUserId(currentUserId);
        route.setVisibility(request.getVisibility());
        route.setAirlineId(request.getAirlineId());

        // Segmentler üzerinden güzergah oluştur
        createRouteSegments(route, request.getSegments());

        // Toplam mesafe ve süreyi hesapla
        int totalDistance = 0;
        int totalTime = 0;
        for (RouteSegment segment : route.getSegments()) {
            if (segment.getDistance() != null) totalDistance += segment.getDistance();
            if (segment.getEstimatedFlightTime() != null) totalTime += segment.getEstimatedFlightTime();
        }
        route.setDistance(totalDistance);
        route.setEstimatedFlightTime(totalTime);

        route = routeRepository.save(route);
        kafkaProducerService.sendRouteEvent("ROUTE_CREATED", route);
        return routeMapper.toResponse(route);
    }

    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest request, Long currentUserId, boolean isAdmin) {
        log.debug("Updating route with id: {} by user: {}", id, currentUserId);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        // Authorization kontrolü
        if (!isAdmin && !route.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("You can only update routes you created");
        }

        // Route code unique kontrolü (değişmişse)
        if (!route.getRouteCode().equals(request.getRouteCode()) &&
                routeRepository.existsByRouteCode(request.getRouteCode())) {
            throw new DuplicateResourceException("Route with code '" + request.getRouteCode() + "' already exists");
        }

        // 1. Route temel bilgilerini güncelle (ID korunur!)
        route.setRouteCode(request.getRouteCode());
        route.setRouteName(request.getRouteName());
        route.setRouteType(request.getRouteType());
        route.setActive(request.getActive());
        route.setVisibility(request.getVisibility());
        route.setAirlineId(request.getAirlineId());

        // 2. MANUEL SEGMENT TEMİZLEME (Constraint violation'u önler)
        log.debug("Manually deleting segments for route ID: {}", id);

        // 2a. Entity koleksiyonunu temizle
        route.getSegments().clear();

        // 2b. Veritabanından manuel sil
        routeSegmentRepository.deleteByRouteId(id);

        // 2c. Flush işlemini zorla - veritabanından silinmesini garanti et
        routeRepository.flush();

        // 3. Yeni segment'leri ekle
        if (request.getSegments() != null && !request.getSegments().isEmpty()) {
            log.debug("Adding {} new segments for route {}", request.getSegments().size(), id);
            createRouteSegments(route, request.getSegments());
        }

        // 4. Toplam mesafe ve süreyi yeniden hesapla
        recalculateRouteMetrics(route);

        // 5. Route'u kaydet (aynı ID ile)
        route = routeRepository.save(route);

        // 6. Event gönder
        kafkaProducerService.sendRouteEvent("ROUTE_UPDATED", route);

        log.debug("Successfully updated route with ID: {} (ID preserved)", route.getId());
        return routeMapper.toResponse(route);
    }

    public void deleteRoute(Long id, Long currentUserId, boolean isAdmin) {
        log.debug("Deleting route with id: {} by user: {}", id, currentUserId);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        // Ownership kontrolü
        if (!isAdmin && !route.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("You can only delete your own routes");
        }

        // Bağımlılık kontrolü
        DeletionCheckResult deletionCheck = deletionValidator.checkDependencies(id);
        if (!deletionCheck.isCanDelete()) {
            throw new BusinessException("Cannot delete route. " + deletionCheck.getReason());
        }

        routeRepository.delete(route);

        kafkaProducerService.sendRouteEvent("ROUTE_DELETED", route);
    }

    // Backward compatibility için eski metod (system user olarak çalışır)
    public void deleteRoute(Long id) {
        log.debug("Deleting route with id: {} (system operation)", id);
        deleteRoute(id, 0L, true); // System user ID=0, admin=true
    }

    // Route code generate etme (auto-generation için)
    public String generateRouteCode(String prefix) {
        int counter = 1;
        String code;
        do {
            code = String.format("%s-%03d", prefix, counter);
            counter++;
        } while (routeRepository.existsByRouteCode(code));

        return code;
    }

    // Private helper methods
    private void validateRouteRequest(RouteRequest request) {
        if (request.getSegments() == null || request.getSegments().size() < 1) {
            throw new BusinessException("Route must have at least 1 segment");
        }
        // Segmentlerin sıralı ve tutarlı olup olmadığını kontrol et (isteğe bağlı)
    }

    private void createRouteSegments(Route route, List<RouteSegmentRequest> segmentRequests) {
        int order = 1;
        for (RouteSegmentRequest segmentRequest : segmentRequests) {
            Airport originAirport = airportRepository.findById(segmentRequest.getOriginAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Origin airport not found for segment"));
            Airport destinationAirport = airportRepository.findById(segmentRequest.getDestinationAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found for segment"));

            RouteSegment segment = new RouteSegment();
            segment.setRoute(route);
            segment.setSegmentOrder(order++);
            segment.setOriginAirport(originAirport);
            segment.setDestinationAirport(destinationAirport);
            segment.setDistance(segmentRequest.getDistance());
            segment.setEstimatedFlightTime(segmentRequest.getEstimatedFlightTime());
            segment.setActive(segmentRequest.getActive() != null ? segmentRequest.getActive() : true);

            route.addSegment(segment);
        }
    }

    private void recalculateRouteMetrics(Route route) {
        int totalDistance = 0;
        int totalTime = 0;

        if (route.getSegments() != null) {
            for (RouteSegment segment : route.getSegments()) {
                if (segment.getDistance() != null) totalDistance += segment.getDistance();
                if (segment.getEstimatedFlightTime() != null) totalTime += segment.getEstimatedFlightTime();
            }
        }

        route.setDistance(totalDistance);
        route.setEstimatedFlightTime(totalTime);

        log.debug("Recalculated metrics - Distance: {}, Time: {}", totalDistance, totalTime);
    }

    // Eski createRoute signature için wrapper
    public RouteResponse createRoute(RouteRequest request) {
        log.debug("Creating route with legacy signature");
        Long systemUserId = 0L; // System user
        boolean isAdmin = true;  // System operation
        return createRoute(request, systemUserId, isAdmin);
    }

    // Eski updateRoute signature için wrapper
    public RouteResponse updateRoute(Long id, RouteRequest request) {
        log.debug("Updating route with legacy signature");
        Long systemUserId = 0L; // System user
        boolean isAdmin = true;  // System operation
        return updateRoute(id, request, systemUserId, isAdmin);
    }

}