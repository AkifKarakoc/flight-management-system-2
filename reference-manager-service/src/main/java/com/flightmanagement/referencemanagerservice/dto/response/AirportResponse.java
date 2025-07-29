package com.flightmanagement.referencemanagerservice.dto.response;

import com.flightmanagement.referencemanagerservice.entity.enums.AirportType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AirportResponse {
    private Long id;
    private String iataCode;
    private String icaoCode;
    private String name;
    private String city;
    private String country;
    private AirportType type;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}