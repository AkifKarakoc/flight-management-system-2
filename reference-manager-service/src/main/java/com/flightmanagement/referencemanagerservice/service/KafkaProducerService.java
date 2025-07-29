package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.entity.*;
import com.flightmanagement.referencemanagerservice.event.ReferenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, ReferenceEvent> kafkaTemplate;
    private static final String TOPIC = "reference.events";

    public void sendAirlineEvent(String eventType, Airline airline) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", airline.getId());
        payload.put("iataCode", airline.getIataCode());
        payload.put("icaoCode", airline.getIcaoCode());
        payload.put("name", airline.getName());
        payload.put("country", airline.getCountry());
        payload.put("type", airline.getType());
        payload.put("active", airline.getActive());

        ReferenceEvent event = ReferenceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .entityType("AIRLINE")
                .entityId(airline.getId().toString())
                .payload(payload)
                .version("1.0")
                .build();

        log.info("Sending airline event: {} for airline: {}", eventType, airline.getIataCode());
        kafkaTemplate.send(TOPIC, event);
    }

    public void sendAirportEvent(String eventType, Airport airport) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", airport.getId());
        payload.put("iataCode", airport.getIataCode());
        payload.put("icaoCode", airport.getIcaoCode());
        payload.put("name", airport.getName());
        payload.put("city", airport.getCity());
        payload.put("country", airport.getCountry());
        payload.put("type", airport.getType());
        payload.put("active", airport.getActive());

        ReferenceEvent event = ReferenceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .entityType("AIRPORT")
                .entityId(airport.getId().toString())
                .payload(payload)
                .version("1.0")
                .build();

        log.info("Sending airport event: {} for airport: {}", eventType, airport.getIataCode());
        kafkaTemplate.send(TOPIC, event);
    }

    public void sendAircraftEvent(String eventType, Aircraft aircraft) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", aircraft.getId());
        payload.put("registrationNumber", aircraft.getRegistrationNumber());
        payload.put("aircraftType", aircraft.getAircraftType());
        payload.put("manufacturer", aircraft.getManufacturer());
        payload.put("model", aircraft.getModel());
        payload.put("seatCapacity", aircraft.getSeatCapacity());
        payload.put("status", aircraft.getStatus());

        // Airline bilgisi için sadece ID ve IATA code gönder
        if (aircraft.getAirline() != null) {
            Map<String, Object> airlineInfo = new HashMap<>();
            airlineInfo.put("id", aircraft.getAirline().getId());
            airlineInfo.put("iataCode", aircraft.getAirline().getIataCode());
            airlineInfo.put("name", aircraft.getAirline().getName());
            payload.put("airline", airlineInfo);
        }

        ReferenceEvent event = ReferenceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .entityType("AIRCRAFT")
                .entityId(aircraft.getId().toString())
                .payload(payload)
                .version("1.0")
                .build();

        log.info("Sending aircraft event: {} for aircraft: {}", eventType, aircraft.getRegistrationNumber());
        kafkaTemplate.send(TOPIC, event);
    }

    public void sendRouteEvent(String eventType, Route route) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", route.getId());
        payload.put("distance", route.getDistance());
        payload.put("estimatedFlightTime", route.getEstimatedFlightTime());
        payload.put("routeType", route.getRouteType());
        payload.put("active", route.getActive());

        ReferenceEvent event = ReferenceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .entityType("ROUTE")
                .entityId(route.getId().toString())
                .payload(payload)
                .version("1.0")
                .build();

        log.info("Sending route event: {} for route: {}", eventType, route.getId());
        kafkaTemplate.send(TOPIC, event);
    }

    public void sendCrewMemberEvent(String eventType, CrewMember crewMember) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", crewMember.getId());
        payload.put("firstName", crewMember.getFirstName());
        payload.put("lastName", crewMember.getLastName());
        payload.put("employeeNumber", crewMember.getEmployeeNumber());
        payload.put("crewType", crewMember.getCrewType());
        payload.put("status", crewMember.getStatus());

        // Airline bilgisi
        if (crewMember.getAirline() != null) {
            Map<String, Object> airlineInfo = new HashMap<>();
            airlineInfo.put("id", crewMember.getAirline().getId());
            airlineInfo.put("iataCode", crewMember.getAirline().getIataCode());
            airlineInfo.put("name", crewMember.getAirline().getName());
            payload.put("airline", airlineInfo);
        }

        ReferenceEvent event = ReferenceEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .entityType("CREW_MEMBER")
                .entityId(crewMember.getId().toString())
                .payload(payload)
                .version("1.0")
                .build();

        log.info("Sending crew member event: {} for crew: {}", eventType, crewMember.getEmployeeNumber());
        kafkaTemplate.send(TOPIC, event);
    }
}