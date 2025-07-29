package com.flightmanagement.flightarchiveservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_archives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightArchive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    // Flight specific fields
    @Column
    private String flightNumber;

    @Column
    private Long flightId;

    @Column
    private Long airlineId;

    @Column
    private String airlineName;

    @Column
    private String airlineIataCode;

    @Column
    private Long aircraftId;

    @Column
    private String aircraftRegistration;

    @Column
    private String aircraftType;

    @Column
    private Long originAirportId;

    @Column
    private String originAirportIata;

    @Column
    private String originAirportName;

    @Column
    private Long destinationAirportId;

    @Column
    private String destinationAirportIata;

    @Column
    private String destinationAirportName;

    @Column
    private LocalDate flightDate;

    @Column
    private LocalDateTime scheduledDeparture;

    @Column
    private LocalDateTime scheduledArrival;

    @Column
    private LocalDateTime actualDeparture;

    @Column
    private LocalDateTime actualArrival;

    @Column
    private String status;

    @Column
    private String flightType;

    @Column
    private Integer passengerCount;

    @Column
    private Integer cargoWeight;

    @Column
    private String gateNumber;

    @Column
    private Integer delayMinutes;

    @Column
    private String delayReason;

    @Column
    private Boolean active;

    // JSON payload for complete event data
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column
    private String version;

    @CreationTimestamp
    private LocalDateTime archivedAt;

    // Helper methods
    public boolean isDelayed() {
        return delayMinutes != null && delayMinutes > 0;
    }

    public boolean isCompleted() {
        return "ARRIVED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public Integer getFlightDuration() {
        if (actualDeparture != null && actualArrival != null) {
            return (int) java.time.Duration.between(actualDeparture, actualArrival).toMinutes();
        } else if (scheduledDeparture != null && scheduledArrival != null) {
            return (int) java.time.Duration.between(scheduledDeparture, scheduledArrival).toMinutes();
        }
        return null;
    }
}