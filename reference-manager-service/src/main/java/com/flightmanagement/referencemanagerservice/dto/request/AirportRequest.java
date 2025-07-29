package com.flightmanagement.referencemanagerservice.dto.request;

import com.flightmanagement.referencemanagerservice.entity.enums.AirportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirportRequest {
    @NotBlank(message = "IATA code is required")
    @Size(min = 3, max = 3, message = "IATA code must be 3 characters")
    private String iataCode;

    @NotBlank(message = "ICAO code is required")
    @Size(min = 4, max = 4, message = "ICAO code must be 4 characters")
    private String icaoCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Airport type is required")
    private AirportType type;

    private Boolean active = true;
}