package com.flightmanagement.flightarchiveservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FlightArchiveResponse {
    private Long id;
    private String eventId;
    private String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    private String entityType;
    private String entityId;

    // Flight details
    private String flightNumber;
    private Long flightId;
    private Long airlineId;
    private String airlineName;
    private String airlineIataCode;
    private Long aircraftId;
    private String aircraftRegistration;
    private String aircraftType;
    private Long originAirportId;
    private String originAirportIata;
    private String originAirportName;
    private Long destinationAirportId;
    private String destinationAirportIata;
    private String destinationAirportName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate flightDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledArrival;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime actualDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime actualArrival;

    private String status;
    private String flightType;
    private Integer passengerCount;
    private Integer cargoWeight;
    private String gateNumber;
    private Integer delayMinutes;
    private String delayReason;
    private Boolean active;
    private String version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archivedAt;

    // Computed fields
    private Boolean isDelayed;
    private Boolean isCompleted;
    private Boolean isCancelled;
    private Integer flightDuration;
}