package com.flightmanagement.referencemanagerservice.dto.response;

import com.flightmanagement.referencemanagerservice.entity.enums.AirlineType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AirlineResponse {
    private Long id;
    private String iataCode;
    private String icaoCode;
    private String name;
    private String country;
    private AirlineType type;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}