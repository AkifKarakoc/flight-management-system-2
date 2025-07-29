package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.event.FlightEvent;
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

    private final KafkaTemplate<String, FlightEvent> kafkaTemplate;
    private final ReferenceDataService referenceDataService;
    private static final String TOPIC = "flight.events";

    public void sendFlightEvent(String eventType, Flight flight) {
        try {
            Map<String, Object> payload = buildFlightPayload(flight);

            FlightEvent event = FlightEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .eventTime(LocalDateTime.now())
                    .entityType("FLIGHT")
                    .entityId(flight.getId().toString())
                    .payload(payload)
                    .version("1.0")
                    .build();

            log.info("Sending flight event: {} for flight: {}", eventType, flight.getFlightNumber());
            kafkaTemplate.send(TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to send flight event: {} for flight: {}", eventType, flight.getFlightNumber(), e);
        }
    }

    private Map<String, Object> buildFlightPayload(Flight flight) {
        Map<String, Object> payload = new HashMap<>();

        // Basic flight info
        payload.put("id", flight.getId());
        payload.put("flightNumber", flight.getFlightNumber());
        payload.put("flightDate", flight.getFlightDate());
        payload.put("scheduledDeparture", flight.getScheduledDeparture());
        payload.put("scheduledArrival", flight.getScheduledArrival());
        payload.put("actualDeparture", flight.getActualDeparture());
        payload.put("actualArrival", flight.getActualArrival());
        payload.put("status", flight.getStatus());
        payload.put("type", flight.getType());
        payload.put("passengerCount", flight.getPassengerCount());
        payload.put("cargoWeight", flight.getCargoWeight());
        payload.put("gateNumber", flight.getGateNumber());
        payload.put("delayMinutes", flight.getDelayMinutes());
        payload.put("delayReason", flight.getDelayReason());
        payload.put("active", flight.getActive());

        // Add reference data
        try {
            AirlineCache airline = referenceDataService.getAirline(flight.getAirlineId());
            Map<String, Object> airlineInfo = new HashMap<>();
            airlineInfo.put("id", airline.getId());
            airlineInfo.put("iataCode", airline.getIataCode());
            airlineInfo.put("name", airline.getName());
            payload.put("airline", airlineInfo);
        } catch (Exception e) {
            log.warn("Could not fetch airline info for flight event: {}", flight.getAirlineId());
        }

        try {
            AircraftCache aircraft = referenceDataService.getAircraft(flight.getAircraftId());
            Map<String, Object> aircraftInfo = new HashMap<>();
            aircraftInfo.put("id", aircraft.getId());
            aircraftInfo.put("registrationNumber", aircraft.getRegistrationNumber());
            aircraftInfo.put("aircraftType", aircraft.getAircraftType());
            payload.put("aircraft", aircraftInfo);
        } catch (Exception e) {
            log.warn("Could not fetch aircraft info for flight event: {}", flight.getAircraftId());
        }

        try {
            if (flight.getRouteId() != null) {
                RouteCache route = referenceDataService.getRoute(flight.getRouteId());
                if (route != null) {
                    // Origin airport
                    if (route.getOriginAirportId() != null) {
                        AirportCache originAirport = referenceDataService.getAirport(route.getOriginAirportId());
                        Map<String, Object> originInfo = new HashMap<>();
                        originInfo.put("id", originAirport.getId());
                        originInfo.put("iataCode", originAirport.getIataCode());
                        originInfo.put("name", originAirport.getName());
                        payload.put("originAirport", originInfo);
                    }

                    // Destination airport
                    if (route.getDestinationAirportId() != null) {
                        AirportCache destinationAirport = referenceDataService.getAirport(route.getDestinationAirportId());
                        Map<String, Object> destInfo = new HashMap<>();
                        destInfo.put("id", destinationAirport.getId());
                        destInfo.put("iataCode", destinationAirport.getIataCode());
                        destInfo.put("name", destinationAirport.getName());
                        payload.put("destinationAirport", destInfo);
                    }

                    // Route bilgilerini de ekle
                    Map<String, Object> routeInfo = new HashMap<>();
                    routeInfo.put("id", route.getId());
                    routeInfo.put("routeCode", route.getRouteCode());
                    routeInfo.put("routePath", route.getRoutePath());
                    routeInfo.put("distance", route.getDistance());
                    routeInfo.put("estimatedTime", route.getEstimatedFlightTime());
                    payload.put("route", routeInfo);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch route/airport info for flight event: {}", e.getMessage());
            // Route bilgisi alınamazsa devam et, airport bilgileri olmadan event gönder
        }

        return payload;
    }
}