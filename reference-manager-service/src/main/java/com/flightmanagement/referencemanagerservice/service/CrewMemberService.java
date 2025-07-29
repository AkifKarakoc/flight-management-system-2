package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.dto.request.CrewMemberRequest;
import com.flightmanagement.referencemanagerservice.dto.response.CrewMemberResponse;
import com.flightmanagement.referencemanagerservice.entity.Airline;
import com.flightmanagement.referencemanagerservice.entity.CrewMember;
import com.flightmanagement.referencemanagerservice.exception.ResourceNotFoundException;
import com.flightmanagement.referencemanagerservice.exception.DuplicateResourceException;
import com.flightmanagement.referencemanagerservice.mapper.CrewMemberMapper;
import com.flightmanagement.referencemanagerservice.repository.AirlineRepository;
import com.flightmanagement.referencemanagerservice.repository.CrewMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CrewMemberService {
    private final CrewMemberRepository crewMemberRepository;
    private final AirlineRepository airlineRepository;
    private final CrewMemberMapper crewMemberMapper;
    private final KafkaProducerService kafkaProducerService;

    public List<CrewMemberResponse> getAllCrewMembers() {
        log.debug("Fetching all crew members");
        return crewMemberRepository.findAll().stream()
                .map(crewMemberMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CrewMemberResponse getCrewMemberById(Long id) {
        log.debug("Fetching crew member with id: {}", id);
        CrewMember crewMember = crewMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crew member not found with id: " + id));
        return crewMemberMapper.toResponse(crewMember);
    }

    public CrewMemberResponse createCrewMember(CrewMemberRequest request) {
        log.debug("Creating new crew member with employee number: {}", request.getEmployeeNumber());

        if (crewMemberRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException("Crew member already exists with employee number: " + request.getEmployeeNumber());
        }

        Airline airline = airlineRepository.findById(request.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + request.getAirlineId()));

        CrewMember crewMember = crewMemberMapper.toEntity(request);
        crewMember.setAirline(airline);

        crewMember = crewMemberRepository.save(crewMember);

        kafkaProducerService.sendCrewMemberEvent("CREW_MEMBER_CREATED", crewMember);

        return crewMemberMapper.toResponse(crewMember);
    }

    public CrewMemberResponse updateCrewMember(Long id, CrewMemberRequest request) {
        log.debug("Updating crew member with id: {}", id);

        CrewMember crewMember = crewMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crew member not found with id: " + id));

        if (!crewMember.getEmployeeNumber().equals(request.getEmployeeNumber()) &&
                crewMemberRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException("Crew member already exists with employee number: " + request.getEmployeeNumber());
        }

        if (!crewMember.getAirline().getId().equals(request.getAirlineId())) {
            Airline airline = airlineRepository.findById(request.getAirlineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + request.getAirlineId()));
            crewMember.setAirline(airline);
        }

        crewMemberMapper.updateEntity(crewMember, request);
        crewMember = crewMemberRepository.save(crewMember);

        kafkaProducerService.sendCrewMemberEvent("CREW_MEMBER_UPDATED", crewMember);

        return crewMemberMapper.toResponse(crewMember);
    }

    public void deleteCrewMember(Long id) {
        log.debug("Deleting crew member with id: {}", id);

        CrewMember crewMember = crewMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crew member not found with id: " + id));

        crewMemberRepository.delete(crewMember);

        kafkaProducerService.sendCrewMemberEvent("CREW_MEMBER_DELETED", crewMember);
    }
}