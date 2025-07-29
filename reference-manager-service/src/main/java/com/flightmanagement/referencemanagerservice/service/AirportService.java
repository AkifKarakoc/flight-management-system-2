package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.dto.request.AirportRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirportResponse;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.entity.Airport;
import com.flightmanagement.referencemanagerservice.entity.Route;
import com.flightmanagement.referencemanagerservice.entity.RouteSegment;
import com.flightmanagement.referencemanagerservice.exception.ResourceNotFoundException;
import com.flightmanagement.referencemanagerservice.exception.DuplicateResourceException;
import com.flightmanagement.referencemanagerservice.mapper.AirportMapper;
import com.flightmanagement.referencemanagerservice.repository.AirportRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteRepository;
import com.flightmanagement.referencemanagerservice.repository.RouteSegmentRepository;
import com.flightmanagement.referencemanagerservice.validator.AirportDeletionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AirportService {
    private final AirportRepository airportRepository;
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final AirportMapper airportMapper;
    private final KafkaProducerService kafkaProducerService;
    private final AirportDeletionValidator deletionValidator;
    private final RouteService routeService;
    private final WebSocketMessageService webSocketMessageService;


    public Page<AirportResponse> getAllAirports(Pageable pageable) {
        log.debug("Fetching all airports with pagination");
        Page<Airport> airportPage = airportRepository.findAll(pageable);
        return airportPage.map(airportMapper::toResponse);
    }

    public AirportResponse getAirportById(Long id) {
        log.debug("Fetching airport with id: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + id));
        return airportMapper.toResponse(airport);
    }

    public AirportResponse getAirportByIataCode(String iataCode) {
        log.debug("Fetching airport with IATA code: {}", iataCode);
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with IATA code: " + iataCode));
        return airportMapper.toResponse(airport);
    }

    public AirportResponse createAirport(AirportRequest request) {
        log.debug("Creating new airport with IATA code: {}", request.getIataCode());

        if (airportRepository.existsByIataCode(request.getIataCode())) {
            throw new DuplicateResourceException("Airport already exists with IATA code: " + request.getIataCode());
        }

        if (airportRepository.existsByIcaoCode(request.getIcaoCode())) {
            throw new DuplicateResourceException("Airport already exists with ICAO code: " + request.getIcaoCode());
        }

        Airport airport = airportMapper.toEntity(request);


        airport = airportRepository.save(airport);

        // Kafka event publish
        kafkaProducerService.sendAirportEvent("AIRPORT_CREATED", airport);
        webSocketMessageService.sendAirportUpdate("CREATE", airportMapper.toResponse(airport), airportMapper.toResponse(airport).getId());


        return airportMapper.toResponse(airport);
    }

    public AirportResponse updateAirport(Long id, AirportRequest request) {
        log.debug("Updating airport with id: {}", id);

        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + id));

        // Check for duplicates only if codes are being changed
        if (!airport.getIataCode().equals(request.getIataCode()) &&
                airportRepository.existsByIataCode(request.getIataCode())) {
            throw new DuplicateResourceException("Airport already exists with IATA code: " + request.getIataCode());
        }

        if (!airport.getIcaoCode().equals(request.getIcaoCode()) &&
                airportRepository.existsByIcaoCode(request.getIcaoCode())) {
            throw new DuplicateResourceException("Airport already exists with ICAO code: " + request.getIcaoCode());
        }

        airportMapper.updateEntity(airport, request);

        airport = airportRepository.save(airport);

        // Kafka event publish
        kafkaProducerService.sendAirportEvent("AIRPORT_UPDATED", airport);
        webSocketMessageService.sendAirportUpdate("UPDATE", airportMapper.toResponse(airport), id);


        return airportMapper.toResponse(airport);
    }

    public DeletionCheckResult checkAirportDeletion(Long id) {
        log.debug("Checking deletion dependencies for airport with id: {}", id);

        // Airport var mı kontrol et
        airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + id));

        return deletionValidator.checkDependencies(id);
    }

    public void deleteAirport(Long id) {
        log.debug("Deleting airport with id: {}", id);

        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + id));

        // Dependency validation
        deletionValidator.validateDeletion(id);

        airportRepository.delete(airport);

        // Kafka event publish
        kafkaProducerService.sendAirportEvent("AIRPORT_DELETED", airport);
        webSocketMessageService.sendAirportUpdate("DELETE", null, id);
    }

    // AirportService.java forceDeleteAirport metodundaki düzeltme:

    @Transactional
    public void forceDeleteAirport(Long id) {
        log.debug("Force deleting airport with id: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + id));
        // İlişkili segmentleri bul
        List<RouteSegment> originSegments = routeSegmentRepository.findByOriginAirportId(id);
        List<RouteSegment> destSegments = routeSegmentRepository.findByDestinationAirportId(id);
        // İlişkili route'ları sil (her route'u bir kez silmek için id'leri toplayıp uniq yap)
        Set<Long> routeIds = new HashSet<>();
        for (RouteSegment seg : originSegments) routeIds.add(seg.getRoute().getId());
        for (RouteSegment seg : destSegments) routeIds.add(seg.getRoute().getId());
        Long systemUserId = 0L; boolean isAdmin = true;
        for (Long routeId : routeIds) {
            routeService.deleteRoute(routeId, systemUserId, isAdmin);
        }
        airportRepository.delete(airport);
        kafkaProducerService.sendAirportEvent("AIRPORT_FORCE_DELETED", airport);
    }
}