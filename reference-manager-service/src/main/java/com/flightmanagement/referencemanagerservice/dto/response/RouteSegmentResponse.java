package com.flightmanagement.referencemanagerservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteSegmentResponse {
    private Long id;
    private Integer segmentOrder;

    private AirportResponse originAirport;
    private AirportResponse destinationAirport;

    private Integer distance;
    private Integer estimatedFlightTime;
    private Boolean active;

    // Hesaplanan alanlar
    private String segmentPath; // "IST → ANK" şeklinde

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}