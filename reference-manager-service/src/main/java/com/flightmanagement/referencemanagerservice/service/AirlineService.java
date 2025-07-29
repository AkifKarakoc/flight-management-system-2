package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.dto.request.AirlineRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirlineResponse;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.entity.Airline;
import com.flightmanagement.referencemanagerservice.entity.Aircraft;
import com.flightmanagement.referencemanagerservice.entity.CrewMember;
import com.flightmanagement.referencemanagerservice.exception.ResourceNotFoundException;
import com.flightmanagement.referencemanagerservice.exception.DuplicateResourceException;
import com.flightmanagement.referencemanagerservice.mapper.AirlineMapper;
import com.flightmanagement.referencemanagerservice.repository.AirlineRepository;
import com.flightmanagement.referencemanagerservice.repository.AircraftRepository;
import com.flightmanagement.referencemanagerservice.repository.CrewMemberRepository;
import com.flightmanagement.referencemanagerservice.validator.AirlineDeletionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AirlineService {
    private final AirlineRepository airlineRepository;
    private final AircraftRepository aircraftRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final AirlineMapper airlineMapper;
    private final KafkaProducerService kafkaProducerService;
    private final AirlineDeletionValidator deletionValidator;
    private final AircraftService aircraftService;
    private final CrewMemberService crewMemberService;
    private final WebSocketMessageService webSocketMessageService;


    public Page<AirlineResponse> getAllAirlines(Pageable pageable) {
        log.debug("Fetching all airlines with pagination");
        Page<Airline> airlinePage = airlineRepository.findAll(pageable);
        return airlinePage.map(airlineMapper::toResponse);
    }

    public AirlineResponse getAirlineById(Long id) {
        log.debug("Fetching airline with id: {}", id);
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + id));
        return airlineMapper.toResponse(airline);
    }

    public AirlineResponse getAirlineByIataCode(String iataCode) {
        log.debug("Fetching airline with IATA code: {}", iataCode);
        Airline airline = airlineRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with IATA code: " + iataCode));
        return airlineMapper.toResponse(airline);
    }

    public AirlineResponse createAirline(AirlineRequest request) {
        log.debug("Creating new airline with IATA code: {}", request.getIataCode());

        if (airlineRepository.existsByIataCode(request.getIataCode())) {
            throw new DuplicateResourceException("Airline already exists with IATA code: " + request.getIataCode());
        }

        if (airlineRepository.existsByIcaoCode(request.getIcaoCode())) {
            throw new DuplicateResourceException("Airline already exists with ICAO code: " + request.getIcaoCode());
        }

        Airline airline = airlineMapper.toEntity(request);
        airline = airlineRepository.save(airline);

        // Kafka event publish
        kafkaProducerService.sendAirlineEvent("AIRLINE_CREATED", airline);
        webSocketMessageService.sendAirlineUpdate("CREATE", airlineMapper.toResponse(airline), airlineMapper.toResponse(airline).getId());


        return airlineMapper.toResponse(airline);
    }

    public AirlineResponse updateAirline(Long id, AirlineRequest request) {
        log.debug("Updating airline with id: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + id));

        // Check for duplicates only if codes are being changed
        if (!airline.getIataCode().equals(request.getIataCode()) &&
                airlineRepository.existsByIataCode(request.getIataCode())) {
            throw new DuplicateResourceException("Airline already exists with IATA code: " + request.getIataCode());
        }

        if (!airline.getIcaoCode().equals(request.getIcaoCode()) &&
                airlineRepository.existsByIcaoCode(request.getIcaoCode())) {
            throw new DuplicateResourceException("Airline already exists with ICAO code: " + request.getIcaoCode());
        }

        airlineMapper.updateEntity(airline, request);
        airline = airlineRepository.save(airline);

        // Kafka event publish
        kafkaProducerService.sendAirlineEvent("AIRLINE_UPDATED", airline);
        webSocketMessageService.sendAirlineUpdate("UPDATE", airlineMapper.toResponse(airline), id);


        return airlineMapper.toResponse(airline);
    }

    public DeletionCheckResult checkAirlineDeletion(Long id) {
        log.debug("Checking deletion dependencies for airline with id: {}", id);

        // Airline var mı kontrol et
        airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + id));

        return deletionValidator.checkDependencies(id);
    }

    public void deleteAirline(Long id) {
        log.debug("Deleting airline with id: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + id));

        // Dependency validation
        deletionValidator.validateDeletion(id);

        airlineRepository.delete(airline);

        // Kafka event publish
        kafkaProducerService.sendAirlineEvent("AIRLINE_DELETED", airline);
        webSocketMessageService.sendAirlineUpdate("DELETE", null, id);
    }

    @Transactional
    public void forceDeleteAirline(Long id) {
        log.debug("Force deleting airline with id: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + id));

        // İlişkili kayıtları önce sil
        List<Aircraft> aircrafts = aircraftRepository.findByAirlineId(id);
        for (Aircraft aircraft : aircrafts) {
            aircraftService.forceDeleteAircraft(aircraft.getId());
        }

        List<CrewMember> crewMembers = crewMemberRepository.findByAirlineId(id);
        for (CrewMember crew : crewMembers) {
            crewMemberService.deleteCrewMember(crew.getId());
        }

        airlineRepository.delete(airline);

        // Kafka event publish
        kafkaProducerService.sendAirlineEvent("AIRLINE_FORCE_DELETED", airline);
    }
}