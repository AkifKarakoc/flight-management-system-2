package com.flightmanagement.flightservice.dto.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AircraftCache implements Serializable {
    private Long id;
    private String registrationNumber;
    private String aircraftType;
    private String manufacturer;
    private String model;
    private Integer seatCapacity;
    private Integer cargoCapacity;
    private Integer maxRange;
    private LocalDate manufactureDate;
    private LocalDate lastMaintenance;
    private String status;
    private Long airlineId;

    // Airline nested object'ini handle et
    @JsonProperty("airline")
    @SuppressWarnings("unchecked")
    private void unpackAirline(Map<String, Object> airline) {
        if (airline != null && airline.get("id") != null) {
            Object idObj = airline.get("id");
            if (idObj instanceof Number) {
                this.airlineId = ((Number) idObj).longValue();
            }
        }
    }

    // FlightValidator için backward compatibility
    public Integer getPassengerCapacity() {
        return this.seatCapacity;
    }
}