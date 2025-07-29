package com.flightmanagement.flightservice.mapper;

import com.flightmanagement.flightservice.dto.request.ConnectingFlightRequest;
import com.flightmanagement.flightservice.dto.request.FlightRequest;
import com.flightmanagement.flightservice.dto.request.FlightSegmentRequest;
import com.flightmanagement.flightservice.dto.response.FlightConnectionResponse;
import com.flightmanagement.flightservice.dto.response.FlightResponse;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.entity.FlightConnection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    Flight toEntity(FlightRequest request);

    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "aircraft", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "routePath", ignore = true)
    @Mapping(target = "originAirport", ignore = true)
    @Mapping(target = "destinationAirport", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "totalSegments", ignore = true)
    @Mapping(target = "fullRoute", ignore = true)
    @Mapping(target = "routeDistance", ignore = true)
    @Mapping(target = "routeEstimatedTime", ignore = true)
    @Mapping(target = "isMultiSegmentRoute", ignore = true)
    @Mapping(target = "delayStatus", ignore = true)
    @Mapping(target = "onTimePerformance", ignore = true)
    @Mapping(target = "flightPhase", ignore = true)
    @Mapping(target = "punctualityScore", ignore = true)
    @Mapping(target = "weatherImpact", ignore = true)
    @Mapping(target = "operationalStatus", ignore = true)
    @Mapping(target = "routeEfficiency", ignore = true)
    @Mapping(target = "actualDistance", ignore = true)
    @Mapping(target = "alternateRoute", ignore = true)
    @Mapping(expression = "java(flight.getFlightDuration())", target = "flightDuration")
    @Mapping(expression = "java(flight.isDelayed())", target = "isDelayed")
    FlightResponse toResponse(Flight flight);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    void updateEntity(@MappingTarget Flight flight, FlightRequest request);

    // Aktarmalı uçuş için mapping metodları

    /**
     * ConnectingFlightRequest'ten Flight entity'sine map eder
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "routeId", ignore = true)  // Route ID farklı şekilde set edilecek
    @Mapping(target = "flightDate", ignore = true)
    @Mapping(target = "scheduledDeparture", ignore = true)
    @Mapping(target = "scheduledArrival", ignore = true)
    @Mapping(target = "actualDeparture", ignore = true)
    @Mapping(target = "actualArrival", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "delayMinutes", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(target = "gateNumber", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "parentFlightId", ignore = true)
    @Mapping(target = "segmentNumber", ignore = true)
    @Mapping(target = "isConnectingFlight", ignore = true)
    @Mapping(target = "connectionTimeMinutes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    @Mapping(source = "mainFlightNumber", target = "flightNumber")
    Flight connectingRequestToEntity(ConnectingFlightRequest request);

    /**
     * FlightSegmentRequest'ten Flight entity'sine map eder
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flightNumber", ignore = true)
    @Mapping(target = "airlineId", ignore = true)
    @Mapping(target = "aircraftId", ignore = true)
    @Mapping(target = "routeId", ignore = true)  // Route ID farklı şekilde set edilecek
    @Mapping(target = "flightDate", ignore = true)
    @Mapping(target = "actualDeparture", ignore = true)
    @Mapping(target = "actualArrival", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "passengerCount", ignore = true)
    @Mapping(target = "cargoWeight", ignore = true)
    @Mapping(target = "delayMinutes", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "parentFlightId", ignore = true)
    @Mapping(target = "segmentNumber", ignore = true)
    @Mapping(target = "isConnectingFlight", ignore = true)
    @Mapping(target = "connectionTimeMinutes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    Flight segmentRequestToEntity(FlightSegmentRequest request);

    /**
     * FlightConnection entity'sini FlightConnectionResponse'a map eder
     */
    @Mapping(target = "mainFlightNumber", source = "mainFlight.flightNumber")
    @Mapping(target = "segmentFlightNumber", source = "segmentFlight.flightNumber")
    @Mapping(target = "originAirportCode", ignore = true)  // Service'te set edilecek
    @Mapping(target = "destinationAirportCode", ignore = true)  // Service'te set edilecek
    @Mapping(target = "segmentFlight", ignore = true)  // Service'te set edilecek
    FlightConnectionResponse toConnectionResponse(FlightConnection connection);

    // Route bazlı yeni sistem için additional mapping methods

    /**
     * Flight entity'den minimal response oluşturur (list view için)
     */
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "aircraft", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "routePath", ignore = true)
    @Mapping(target = "originAirport", ignore = true)
    @Mapping(target = "destinationAirport", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "totalSegments", ignore = true)
    @Mapping(target = "fullRoute", ignore = true)
    @Mapping(target = "routeDistance", ignore = true)
    @Mapping(target = "routeEstimatedTime", ignore = true)
    @Mapping(target = "isMultiSegmentRoute", ignore = true)
    @Mapping(target = "delayStatus", ignore = true)
    @Mapping(target = "onTimePerformance", ignore = true)
    @Mapping(target = "flightPhase", ignore = true)
    @Mapping(target = "punctualityScore", ignore = true)
    @Mapping(target = "weatherImpact", ignore = true)
    @Mapping(target = "operationalStatus", ignore = true)
    @Mapping(target = "routeEfficiency", ignore = true)
    @Mapping(target = "actualDistance", ignore = true)
    @Mapping(target = "alternateRoute", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(expression = "java(flight.getFlightDuration())", target = "flightDuration")
    @Mapping(expression = "java(flight.isDelayed())", target = "isDelayed")
    FlightResponse toMinimalResponse(Flight flight);

    /**
     * CSV import için özel mapping
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "SCHEDULED")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "isConnectingFlight", constant = "false")
    @Mapping(target = "segmentNumber", constant = "1")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    @Mapping(target = "actualDeparture", ignore = true)
    @Mapping(target = "actualArrival", ignore = true)
    @Mapping(target = "delayMinutes", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(target = "parentFlightId", ignore = true)
    @Mapping(target = "connectionTimeMinutes", ignore = true)
    Flight csvToEntity(FlightRequest request);

    /**
     * Update için partial mapping (sadece değişen alanlar)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flightNumber", ignore = true) // Flight number değiştirilemez
    @Mapping(target = "airlineId", ignore = true)    // Airline değiştirilemez
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    void updatePartialEntity(@MappingTarget Flight flight, FlightRequest request);

    /**
     * Status update için özel mapping
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flightNumber", ignore = true)
    @Mapping(target = "airlineId", ignore = true)
    @Mapping(target = "aircraftId", ignore = true)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "flightDate", ignore = true)
    @Mapping(target = "scheduledDeparture", ignore = true)
    @Mapping(target = "scheduledArrival", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "passengerCount", ignore = true)
    @Mapping(target = "cargoWeight", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "gateNumber", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "parentFlightId", ignore = true)
    @Mapping(target = "segmentNumber", ignore = true)
    @Mapping(target = "isConnectingFlight", ignore = true)
    @Mapping(target = "connectionTimeMinutes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "parentFlight", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "flightConnections", ignore = true)
    void updateStatusOnly(@MappingTarget Flight flight, FlightRequest request);

    // Helper mapping methods

    /**
     * Flight'tan sadece temel bilgileri içeren summary response
     */
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "aircraft", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "routePath", ignore = true)
    @Mapping(target = "originAirport", ignore = true)
    @Mapping(target = "destinationAirport", ignore = true)
    @Mapping(target = "connectingFlights", ignore = true)
    @Mapping(target = "totalSegments", ignore = true)
    @Mapping(target = "fullRoute", ignore = true)
    @Mapping(target = "routeDistance", ignore = true)
    @Mapping(target = "routeEstimatedTime", ignore = true)
    @Mapping(target = "isMultiSegmentRoute", ignore = true)
    @Mapping(target = "delayStatus", ignore = true)
    @Mapping(target = "onTimePerformance", ignore = true)
    @Mapping(target = "flightPhase", ignore = true)
    @Mapping(target = "punctualityScore", ignore = true)
    @Mapping(target = "weatherImpact", ignore = true)
    @Mapping(target = "operationalStatus", ignore = true)
    @Mapping(target = "routeEfficiency", ignore = true)
    @Mapping(target = "actualDistance", ignore = true)
    @Mapping(target = "alternateRoute", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(target = "cargoWeight", ignore = true)
    @Mapping(target = "gateNumber", ignore = true)
    @Mapping(target = "connectionTimeMinutes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(expression = "java(flight.isDelayed())", target = "isDelayed")
    FlightResponse toSummaryResponse(Flight flight);

    // Default methods for complex mapping logic

    /**
     * Flight duration calculation
     */
    default Integer mapFlightDuration(Flight flight) {
        return flight.getFlightDuration();
    }

    /**
     * Delay status mapping
     */
    default Boolean mapDelayStatus(Flight flight) {
        return flight.isDelayed();
    }

    /**
     * Route path building (will be overridden in service)
     */
    default String mapRoutePath(Flight flight) {
        return null; // Service will populate this
    }

    /**
     * Flight phase calculation
     */
    default String mapFlightPhase(Flight flight) {
        if (flight.getStatus() == null) return "UNKNOWN";

        switch (flight.getStatus()) {
            case SCHEDULED:
                return "PRE_FLIGHT";
            case BOARDING:
                return "BOARDING";
            case DEPARTED:
                return flight.getStatus().name().equals("ARRIVED") ? "COMPLETED" : "AIRBORNE";
            case ARRIVED:
                return "COMPLETED";
            case DELAYED:
                return "DELAYED";
            case CANCELLED:
                return "CANCELLED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Operational status mapping
     */
    default String mapOperationalStatus(Flight flight) {
        if (flight.getStatus() == null) return "UNKNOWN";

        switch (flight.getStatus()) {
            case SCHEDULED:
            case BOARDING:
            case DEPARTED:
            case ARRIVED:
                return flight.isDelayed() ? "DELAYED" : "NORMAL";
            case CANCELLED:
                return "CANCELLED";
            case DELAYED:
                return "DELAYED";
            default:
                return "UNKNOWN";
        }
    }
}