package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.dto.request.ConnectingFlightRequest;
import com.flightmanagement.flightservice.dto.request.FlightSegmentRequest;
import com.flightmanagement.flightservice.dto.response.FlightConnectionResponse;
import com.flightmanagement.flightservice.dto.response.FlightResponse;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.entity.FlightConnection;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.exception.InvalidRequestException;
import com.flightmanagement.flightservice.exception.ResourceNotFoundException;
import com.flightmanagement.flightservice.exception.BusinessException;
import com.flightmanagement.flightservice.mapper.FlightMapper;
import com.flightmanagement.flightservice.repository.FlightConnectionRepository;
import com.flightmanagement.flightservice.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConnectingFlightService {

    private final FlightRepository flightRepository;
    private final FlightConnectionRepository flightConnectionRepository;
    private final FlightMapper flightMapper;
    private final ReferenceDataService referenceDataService;
    private final KafkaProducerService kafkaProducerService;
    private final WebSocketMessageService webSocketMessageService;

    /**
     * Aktarmalı uçuş oluşturur
     */
    public FlightResponse createConnectingFlight(ConnectingFlightRequest request) {
        log.debug("Creating connecting flight: {}", request.getMainFlightNumber());

        // Validation
        validateConnectingFlightRequest(request);

        // Ana uçuşu oluştur
        Flight mainFlight = createMainFlight(request);
        mainFlight = flightRepository.save(mainFlight);

        // Segment'leri oluştur
        List<Flight> segments = createFlightSegments(mainFlight, request);

        // Connection'ları kaydet
        saveFlightConnections(mainFlight, segments);

        // Kafka event
        kafkaProducerService.sendFlightEvent("CONNECTING_FLIGHT_CREATED", mainFlight);

        // WebSocket notification
        FlightResponse response = buildConnectingFlightResponse(mainFlight, segments);
        webSocketMessageService.sendFlightUpdate("CONNECTING_FLIGHT_CREATED", response,
                mainFlight.getId(), mainFlight.getFlightNumber());

        return response;
    }

    /**
     * Aktarmalı uçuş validasyonu
     */
    public void validateConnectingFlightRequest(ConnectingFlightRequest request) {
        // Minimum 2 segment olmalı
        if (request.getSegments() == null || request.getSegments().size() < 2) {
            throw new InvalidRequestException("Connecting flight must have at least 2 segments");
        }

        // Maksimum 10 segment olabilir
        if (request.getSegments().size() > 10) {
            throw new InvalidRequestException("Connecting flight cannot have more than 10 segments");
        }

        // Uçuş numarası benzersizliği kontrolü
        LocalDate flightDate = request.getSegments().get(0).getScheduledDeparture().toLocalDate();
        boolean exists = flightRepository.existsMainFlightByFlightNumberAndDate(
                request.getMainFlightNumber(), flightDate);

        if (exists) {
            throw new InvalidRequestException(
                    "Flight number already exists for this date: " + request.getMainFlightNumber());
        }

        // Segment'lerin tutarlılığını kontrol et
        validateSegmentConsistency(request.getSegments());

        // Route bazlı validation
        validateSegmentRoutes(request.getSegments());

        // Reference data validation
        validateReferenceData(request);
    }

    private void validateSegmentConsistency(List<FlightSegmentRequest> segments) {
        for (int i = 0; i < segments.size() - 1; i++) {
            FlightSegmentRequest current = segments.get(i);
            FlightSegmentRequest next = segments.get(i + 1);

            // Segment order kontrolü
            if (current.getSegmentNumber() >= next.getSegmentNumber()) {
                throw new InvalidRequestException("Segment numbers must be in ascending order");
            }

            // Route continuity kontrolü
            validateRouteContinuity(current, next, i + 1);

            // Timing kontrolü
            validateSegmentTiming(current, next, i + 1);
        }
    }

    private void validateRouteContinuity(FlightSegmentRequest current, FlightSegmentRequest next, int segmentIndex) {
        try {
            RouteCache currentRoute = referenceDataService.getRoute(getRouteIdFromSegment(current));
            RouteCache nextRoute = referenceDataService.getRoute(getRouteIdFromSegment(next));

            if (currentRoute.getDestinationAirportId() == null || nextRoute.getOriginAirportId() == null) {
                throw new InvalidRequestException(
                        String.format("Route information incomplete for segments %d and %d", segmentIndex, segmentIndex + 1));
            }

            if (!currentRoute.getDestinationAirportId().equals(nextRoute.getOriginAirportId())) {
                throw new InvalidRequestException(
                        String.format("Route continuity broken: Segment %d destination (%s) must match segment %d origin (%s)",
                                segmentIndex, currentRoute.getDestinationAirportCode(),
                                segmentIndex + 1, nextRoute.getOriginAirportCode()));
            }

        } catch (Exception e) {
            if (e instanceof InvalidRequestException) {
                throw e;
            }
            throw new InvalidRequestException("Failed to validate route continuity: " + e.getMessage());
        }
    }

    private void validateSegmentTiming(FlightSegmentRequest current, FlightSegmentRequest next, int segmentIndex) {
        // Arrival before next departure
        if (!current.getScheduledArrival().isBefore(next.getScheduledDeparture())) {
            throw new InvalidRequestException(
                    String.format("Segment %d arrival time must be before segment %d departure time",
                            segmentIndex, segmentIndex + 1));
        }

        // Minimum connection time (30 dakika)
        long connectionMinutes = java.time.Duration.between(
                current.getScheduledArrival(), next.getScheduledDeparture()).toMinutes();

        if (connectionMinutes < 30) {
            throw new InvalidRequestException(
                    String.format("Minimum 30 minutes connection time required between segments %d and %d (found: %d minutes)",
                            segmentIndex, segmentIndex + 1, connectionMinutes));
        }

        // Maximum connection time (24 saat)
        if (connectionMinutes > 1440) {
            throw new InvalidRequestException(
                    String.format("Maximum 24 hours connection time allowed between segments %d and %d (found: %d minutes)",
                            segmentIndex, segmentIndex + 1, connectionMinutes));
        }
    }

    private void validateSegmentRoutes(List<FlightSegmentRequest> segments) {
        for (int i = 0; i < segments.size(); i++) {
            FlightSegmentRequest segment = segments.get(i);
            Long routeId = getRouteIdFromSegment(segment);

            if (routeId == null) {
                throw new InvalidRequestException(
                        String.format("Route ID is required for segment %d", i + 1));
            }

            try {
                RouteCache route = referenceDataService.getRoute(routeId);
                if (route == null || !route.isActive()) {
                    throw new InvalidRequestException(
                            String.format("Invalid or inactive route for segment %d: %d", i + 1, routeId));
                }
            } catch (Exception e) {
                throw new InvalidRequestException(
                        String.format("Failed to validate route for segment %d: %s", i + 1, e.getMessage()));
            }
        }
    }

    private void validateReferenceData(ConnectingFlightRequest request) {
        // Airline validation
        try {
            var airline = referenceDataService.getAirline(request.getAirlineId());
            if (airline == null || !airline.getActive()) {
                throw new InvalidRequestException("Invalid or inactive airline: " + request.getAirlineId());
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Airline validation failed: " + e.getMessage());
        }

        // Aircraft validation
        try {
            var aircraft = referenceDataService.getAircraft(request.getAircraftId());
            if (aircraft == null || !"ACTIVE".equals(aircraft.getStatus())) {
                throw new InvalidRequestException("Invalid or inactive aircraft: " + request.getAircraftId());
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Aircraft validation failed: " + e.getMessage());
        }
    }

    /**
     * Ana uçuş oluşturur
     */
    private Flight createMainFlight(ConnectingFlightRequest request) {
        Flight mainFlight = flightMapper.connectingRequestToEntity(request);

        // İlk ve son segment'ten bilgileri al
        FlightSegmentRequest firstSegment = request.getSegments().get(0);
        FlightSegmentRequest lastSegment = request.getSegments().get(request.getSegments().size() - 1);

        // Ana uçuş bilgilerini set et
        mainFlight.setFlightNumber(request.getMainFlightNumber());
        mainFlight.setAirlineId(request.getAirlineId());
        mainFlight.setAircraftId(request.getAircraftId());
        mainFlight.setType(request.getType());
        mainFlight.setPassengerCount(request.getPassengerCount());
        mainFlight.setCargoWeight(request.getCargoWeight());
        mainFlight.setNotes(request.getNotes());
        mainFlight.setActive(request.getActive());

        // İlk segment'ten route ve timing bilgileri
        Long firstRouteId = getRouteIdFromSegment(firstSegment);
        mainFlight.setRouteId(firstRouteId); // Ana uçuşa ilk route'u ata

        mainFlight.setFlightDate(firstSegment.getScheduledDeparture().toLocalDate());
        mainFlight.setScheduledDeparture(firstSegment.getScheduledDeparture());
        mainFlight.setScheduledArrival(lastSegment.getScheduledArrival());

        // Connecting flight özellikleri
        mainFlight.setIsConnectingFlight(true);
        mainFlight.setSegmentNumber(0); // Ana uçuş için 0
        mainFlight.setStatus(FlightStatus.SCHEDULED);

        return mainFlight;
    }

    /**
     * Uçuş segment'lerini oluşturur
     */
    private List<Flight> createFlightSegments(Flight mainFlight, ConnectingFlightRequest request) {
        List<Flight> segments = new java.util.ArrayList<>();

        for (int i = 0; i < request.getSegments().size(); i++) {
            FlightSegmentRequest segmentRequest = request.getSegments().get(i);
            Flight segment = createSegmentFromRequest(mainFlight, segmentRequest, i + 1);
            segment = flightRepository.save(segment);
            segments.add(segment);
        }

        return segments;
    }

    /**
     * Segment'ten Flight entity oluşturur
     */
    private Flight createSegmentFromRequest(Flight mainFlight, FlightSegmentRequest segmentRequest, int segmentNumber) {
        Flight segment = flightMapper.segmentRequestToEntity(segmentRequest);

        // Ana uçuştan kopyalanacak bilgiler
        segment.setFlightNumber(mainFlight.getFlightNumber() + "-S" + segmentNumber);
        segment.setAirlineId(mainFlight.getAirlineId());
        segment.setAircraftId(mainFlight.getAircraftId());
        segment.setType(mainFlight.getType());
        segment.setPassengerCount(mainFlight.getPassengerCount());
        segment.setCargoWeight(mainFlight.getCargoWeight());
        segment.setActive(mainFlight.getActive());

        // Segment'e özel bilgiler
        Long routeId = getRouteIdFromSegment(segmentRequest);
        segment.setRouteId(routeId);
        segment.setFlightDate(segmentRequest.getScheduledDeparture().toLocalDate());
        segment.setScheduledDeparture(segmentRequest.getScheduledDeparture());
        segment.setScheduledArrival(segmentRequest.getScheduledArrival());
        segment.setGateNumber(segmentRequest.getGateNumber());
        segment.setNotes(segmentRequest.getNotes());

        // Connecting flight ilişkisi
        segment.setParentFlightId(mainFlight.getId());
        segment.setSegmentNumber(segmentNumber);
        segment.setIsConnectingFlight(false); // Bu bir segment, ana uçuş değil
        segment.setConnectionTimeMinutes(segmentRequest.getConnectionTimeMinutes());
        segment.setStatus(FlightStatus.SCHEDULED);

        return segment;
    }

    /**
     * Flight connection'ları kaydeder
     */
    private void saveFlightConnections(Flight mainFlight, List<Flight> segments) {
        for (int i = 0; i < segments.size(); i++) {
            Flight segment = segments.get(i);

            FlightConnection connection = new FlightConnection();
            connection.setMainFlightId(mainFlight.getId());
            connection.setSegmentFlightId(segment.getId());
            connection.setSegmentOrder(i + 1);

            if (i < segments.size() - 1) {
                Flight currentSegment = segments.get(i);
                Flight nextSegment = segments.get(i + 1);
                int connectionTime = calculateConnectionTime(currentSegment, nextSegment);
                connection.setConnectionTimeMinutes(connectionTime);
            }

            flightConnectionRepository.save(connection);
        }
    }

    /**
     * Connection time hesaplama
     */
    private Integer calculateConnectionTime(Flight currentSegment, Flight nextSegment) {
        if (currentSegment == null || nextSegment == null) return null;

        return (int) java.time.Duration.between(
                currentSegment.getScheduledArrival(),
                nextSegment.getScheduledDeparture()
        ).toMinutes();
    }

    /**
     * Aktarmalı uçuş güncelleme
     */
    public FlightResponse updateConnectingFlight(Long mainFlightId, ConnectingFlightRequest request) {
        log.debug("Updating connecting flight with ID: {}", mainFlightId);

        // Mevcut ana uçuşu bul
        Flight mainFlight = flightRepository.findById(mainFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Main flight not found with ID: " + mainFlightId));

        if (!Boolean.TRUE.equals(mainFlight.getIsConnectingFlight())) {
            throw new BusinessException("Flight is not a connecting flight");
        }

        // Update kuralları kontrolü
        validateUpdateRules(mainFlight, request);

        // Validasyon
        validateConnectingFlightRequest(request);

        // Mevcut segment'leri ve connection'ları sil
        deleteExistingSegments(mainFlightId);

        // Ana uçuşu güncelle
        updateMainFlightFromRequest(mainFlight, request);
        mainFlight = flightRepository.save(mainFlight);

        // Yeni segment'leri oluştur
        List<Flight> newSegments = createFlightSegments(mainFlight, request);

        // Yeni connection'ları kaydet
        saveFlightConnections(mainFlight, newSegments);

        // Kafka event
        kafkaProducerService.sendFlightEvent("CONNECTING_FLIGHT_UPDATED", mainFlight);

        // Response oluştur
        FlightResponse response = buildConnectingFlightResponse(mainFlight, newSegments);
        webSocketMessageService.sendFlightUpdate("CONNECTING_FLIGHT_UPDATED", response,
                mainFlight.getId(), mainFlight.getFlightNumber());

        return response;
    }

    private void validateUpdateRules(Flight existingFlight, ConnectingFlightRequest request) {
        // Eğer ana uçuş departed ise güncelleme yapılamaz
        if (existingFlight.isDeparted()) {
            throw new BusinessException("Cannot update connecting flight after departure");
        }

        // Eğer herhangi bir segment departed ise ana uçuş güncellenemez
        List<Flight> segments = flightRepository.findByParentFlightIdOrderBySegmentNumber(existingFlight.getId());
        boolean anySegmentDeparted = segments.stream().anyMatch(Flight::isDeparted);

        if (anySegmentDeparted) {
            throw new BusinessException("Cannot update connecting flight when any segment has departed");
        }
    }

    private void deleteExistingSegments(Long mainFlightId) {
        // Segment'leri sil
        List<Flight> existingSegments = flightRepository.findByParentFlightIdOrderBySegmentNumber(mainFlightId);
        flightRepository.deleteAll(existingSegments);

        // Connection'ları sil
        List<FlightConnection> existingConnections = flightConnectionRepository.findByMainFlightIdOrderBySegmentOrder(mainFlightId);
        flightConnectionRepository.deleteAll(existingConnections);
    }

    /**
     * Aktarmalı uçuş silme
     */
    public void deleteConnectingFlight(Long mainFlightId) {
        log.debug("Deleting connecting flight with ID: {}", mainFlightId);

        Flight mainFlight = flightRepository.findById(mainFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Main flight not found with ID: " + mainFlightId));

        if (!Boolean.TRUE.equals(mainFlight.getIsConnectingFlight())) {
            throw new BusinessException("Flight is not a connecting flight");
        }

        // Silme kuralları kontrolü
        if (mainFlight.isDeparted()) {
            throw new BusinessException("Cannot delete connecting flight after departure");
        }

        // Segment'leri sil
        deleteExistingSegments(mainFlightId);

        // Ana uçuşu sil
        flightRepository.delete(mainFlight);

        // Kafka event
        kafkaProducerService.sendFlightEvent("CONNECTING_FLIGHT_DELETED", mainFlight);

        // WebSocket notification
        webSocketMessageService.sendFlightUpdate("CONNECTING_FLIGHT_DELETED", null,
                mainFlightId, mainFlight.getFlightNumber());
    }

    /**
     * Filtrelemeyle aktarmalı uçuşları getir
     */
    public Page<FlightResponse> getConnectingFlightsWithFilters(Pageable pageable, Long airlineId, LocalDate flightDate) {
        log.debug("Getting connecting flights with filters - airlineId: {}, date: {}", airlineId, flightDate);

        Page<Flight> flightsPage = flightRepository.findConnectingFlightsWithFilters(airlineId, flightDate, pageable);

        List<FlightResponse> responses = flightsPage.getContent().stream()
                .map(mainFlight -> {
                    List<Flight> segments = flightRepository.findByParentFlightIdOrderBySegmentNumber(mainFlight.getId());
                    return buildConnectingFlightResponse(mainFlight, segments);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, flightsPage.getTotalElements());
    }

    /**
     * Connection detaylarını getir
     */
    public List<FlightConnectionResponse> getConnectionDetails(Long mainFlightId) {
        List<FlightConnection> connections = flightConnectionRepository
                .findByMainFlightIdOrderBySegmentOrder(mainFlightId);

        return connections.stream()
                .map(connection -> {
                    FlightConnectionResponse response = flightMapper.toConnectionResponse(connection);

                    // Airport kodlarını route'lardan al
                    try {
                        if (connection.getSegmentFlight() != null && connection.getSegmentFlight().getRouteId() != null) {
                            RouteCache route = referenceDataService.getRoute(connection.getSegmentFlight().getRouteId());
                            response.setOriginAirportCode(route.getOriginAirportCode());
                            response.setDestinationAirportCode(route.getDestinationAirportCode());
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch route info for connection: {}", e.getMessage());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Segment status güncelleme mantığı
     */
    public void updateConnectingFlightStatus(Long mainFlightId) {
        Flight mainFlight = flightRepository.findById(mainFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Main flight not found"));

        List<Flight> segments = flightRepository.findByParentFlightIdOrderBySegmentNumber(mainFlightId);

        // Tüm segment'lerin statuslarına göre ana uçuş statusunu belirle
        FlightStatus newMainStatus = calculateMainFlightStatus(segments);

        if (!newMainStatus.equals(mainFlight.getStatus())) {
            FlightStatus oldStatus = mainFlight.getStatus();
            mainFlight.setStatus(newMainStatus);

            // Actual times'ı güncelle
            updateMainFlightActualTimes(mainFlight, segments);

            flightRepository.save(mainFlight);

            // Kafka event
            kafkaProducerService.sendFlightEvent("CONNECTING_FLIGHT_STATUS_UPDATED", mainFlight);

            // WebSocket notification
            webSocketMessageService.sendFlightStatusUpdate(mainFlight.getFlightNumber(),
                    oldStatus.name(), newMainStatus.name(),
                    null, mainFlight.getId());
        }
    }

    private FlightStatus calculateMainFlightStatus(List<Flight> segments) {
        if (segments.isEmpty()) return FlightStatus.SCHEDULED;

        boolean allCompleted = segments.stream().allMatch(s -> FlightStatus.ARRIVED.equals(s.getStatus()));
        boolean anyInProgress = segments.stream().anyMatch(s ->
                FlightStatus.DEPARTED.equals(s.getStatus()) || FlightStatus.BOARDING.equals(s.getStatus()));
        boolean anyCancelled = segments.stream().anyMatch(s -> FlightStatus.CANCELLED.equals(s.getStatus()));
        boolean anyDelayed = segments.stream().anyMatch(s -> FlightStatus.DELAYED.equals(s.getStatus()));
        boolean firstDeparted = !segments.isEmpty() && FlightStatus.DEPARTED.equals(segments.get(0).getStatus());

        if (anyCancelled) {
            return FlightStatus.CANCELLED;
        } else if (allCompleted) {
            return FlightStatus.ARRIVED;
        } else if (anyInProgress || firstDeparted) {
            return FlightStatus.DEPARTED;
        } else if (anyDelayed) {
            return FlightStatus.DELAYED;
        } else {
            return FlightStatus.SCHEDULED;
        }
    }

    private void updateMainFlightActualTimes(Flight mainFlight, List<Flight> segments) {
        if (segments.isEmpty()) return;

        Flight firstSegment = segments.get(0);
        Flight lastSegment = segments.get(segments.size() - 1);

        // İlk segment'in actual departure'ını ana uçuşa kopyala
        if (firstSegment.getActualDeparture() != null && mainFlight.getActualDeparture() == null) {
            mainFlight.setActualDeparture(firstSegment.getActualDeparture());
        }

        // Son segment'in actual arrival'ını ana uçuşa kopyala
        if (lastSegment.getActualArrival() != null && mainFlight.getActualArrival() == null) {
            mainFlight.setActualArrival(lastSegment.getActualArrival());
        }
    }

    // Helper metodlar
    private void updateMainFlightFromRequest(Flight mainFlight, ConnectingFlightRequest request) {
        mainFlight.setFlightNumber(request.getMainFlightNumber());
        mainFlight.setAirlineId(request.getAirlineId());
        mainFlight.setAircraftId(request.getAircraftId());
        mainFlight.setType(request.getType());
        mainFlight.setPassengerCount(request.getPassengerCount());
        mainFlight.setCargoWeight(request.getCargoWeight());
        mainFlight.setNotes(request.getNotes());
        mainFlight.setActive(request.getActive());

        // İlk ve son segment'lerden timing bilgilerini güncelle
        FlightSegmentRequest firstSegment = request.getSegments().get(0);
        FlightSegmentRequest lastSegment = request.getSegments().get(request.getSegments().size() - 1);

        Long firstRouteId = getRouteIdFromSegment(firstSegment);
        mainFlight.setRouteId(firstRouteId);
        mainFlight.setFlightDate(firstSegment.getScheduledDeparture().toLocalDate());
        mainFlight.setScheduledDeparture(firstSegment.getScheduledDeparture());
        mainFlight.setScheduledArrival(lastSegment.getScheduledArrival());
    }

    private FlightResponse buildConnectingFlightResponse(Flight mainFlight, List<Flight> segments) {
        FlightResponse response = flightMapper.toResponse(mainFlight);

        // Reference data'ları doldur
        try {
            response.setAirline(referenceDataService.getAirline(mainFlight.getAirlineId()));
            response.setAircraft(referenceDataService.getAircraft(mainFlight.getAircraftId()));

            if (mainFlight.getRouteId() != null) {
                response.setRoute(referenceDataService.getRoute(mainFlight.getRouteId()));
            }
        } catch (Exception e) {
            log.warn("Could not populate reference data: {}", e.getMessage());
        }

        // Segment bilgilerini ekle
        List<FlightResponse> segmentResponses = segments.stream()
                .map(flightMapper::toResponse)
                .collect(Collectors.toList());

        response.setConnectingFlights(segmentResponses);
        response.setTotalSegments(segments.size());
        response.setFullRoute(buildFullRouteFromSegments(segments));

        return response;
    }

    private String buildFullRouteFromSegments(List<Flight> segments) {
        if (segments.isEmpty()) return "";

        StringBuilder route = new StringBuilder();

        try {
            // İlk segment'in origin'i
            Flight firstSegment = segments.get(0);
            if (firstSegment.getRouteId() != null) {
                RouteCache firstRoute = referenceDataService.getRoute(firstSegment.getRouteId());
                route.append(firstRoute.getOriginAirportCode());
            }

            // Tüm segment'lerin destination'larını ekle
            for (Flight segment : segments) {
                if (segment.getRouteId() != null) {
                    RouteCache segmentRoute = referenceDataService.getRoute(segment.getRouteId());
                    route.append(" → ").append(segmentRoute.getDestinationAirportCode());
                }
            }

        } catch (Exception e) {
            log.warn("Error building full route from segments: {}", e.getMessage());
            return "Complex Route";
        }

        return route.toString();
    }

    private Long getRouteIdFromSegment(FlightSegmentRequest segment) {
        // Segment request'te route ID bulma logic'i
        // Bu segment'teki origin/destination'dan route ID'yi bul veya direkt route ID varsa onu kullan

        // Şimdilik origin/destination'dan route aramayı simüle edelim
        // Gerçek implementasyonda Reference Manager'dan route aranacak

        if (segment.getOriginAirportId() != null && segment.getDestinationAirportId() != null) {
            try {
                // Reference Manager'dan bu airport'lar arası route'u bul
                RouteCache[] routes = referenceDataService.getActiveRoutes();
                for (RouteCache route : routes) {
                    if (route.getOriginAirportId() != null && route.getDestinationAirportId() != null &&
                            route.getOriginAirportId().equals(segment.getOriginAirportId()) &&
                            route.getDestinationAirportId().equals(segment.getDestinationAirportId())) {
                        return route.getId();
                    }
                }

                // Route bulunamazsa hata
                throw new BusinessException(
                        String.format("No route found for airports %d -> %d",
                                segment.getOriginAirportId(), segment.getDestinationAirportId()));

            } catch (Exception e) {
                throw new BusinessException("Failed to find route for segment: " + e.getMessage());
            }
        }

        throw new BusinessException("Segment must have origin and destination airport IDs");
    }
}