package com.flightmanagement.referencemanagerservice.dto.request;

import com.flightmanagement.referencemanagerservice.entity.enums.AirlineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineRequest {
    @NotBlank(message = "IATA code is required")
    @Size(min = 2, max = 3, message = "IATA code must be 2-3 characters")
    private String iataCode;

    @NotBlank(message = "ICAO code is required")
    @Size(min = 3, max = 4, message = "ICAO code must be 3-4 characters")
    private String icaoCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Airline type is required")
    private AirlineType type;

    private Boolean active = true;
}