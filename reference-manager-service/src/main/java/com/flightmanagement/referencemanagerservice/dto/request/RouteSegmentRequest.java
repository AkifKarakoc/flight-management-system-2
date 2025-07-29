package com.flightmanagement.referencemanagerservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RouteSegmentRequest {

    @NotNull(message = "Segment order is required")
    @Positive(message = "Segment order must be positive")
    private Integer segmentOrder;

    @NotNull(message = "Origin airport ID is required")
    private Long originAirportId;

    @NotNull(message = "Destination airport ID is required")
    private Long destinationAirportId;

    @Positive(message = "Distance must be positive")
    private Integer distance;

    @Positive(message = "Flight time must be positive")
    private Integer estimatedFlightTime;

    private Boolean active = true;
}