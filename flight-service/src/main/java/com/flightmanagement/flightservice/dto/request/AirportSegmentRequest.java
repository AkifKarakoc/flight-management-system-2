package com.flightmanagement.flightservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AirportSegmentRequest {

    @NotNull(message = "Segment order is required")
    @Min(value = 1, message = "Segment order must be at least 1")
    private Integer segmentOrder;

    @NotNull(message = "Origin airport ID is required")
    private Long originAirportId;

    @NotNull(message = "Destination airport ID is required")
    private Long destinationAirportId;

    @Min(value = 0, message = "Connection time cannot be negative")
    @Max(value = 1440, message = "Connection time cannot exceed 24 hours")
    private Integer connectionTimeMinutes;

    // Business validation
    public boolean isValidSegment() {
        return originAirportId != null &&
                destinationAirportId != null &&
                !originAirportId.equals(destinationAirportId);
    }

    public boolean hasValidConnectionTime() {
        return connectionTimeMinutes == null ||
                (connectionTimeMinutes >= 30 && connectionTimeMinutes <= 1440);
    }
}