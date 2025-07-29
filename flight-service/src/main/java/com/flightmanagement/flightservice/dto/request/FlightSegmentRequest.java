package com.flightmanagement.flightservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightSegmentRequest {

    @NotNull(message = "Segment number is required")
    @Min(value = 1, message = "Segment number must be at least 1")
    @Max(value = 10, message = "Segment number cannot exceed 10")
    private Integer segmentNumber;

    @NotNull(message = "Origin airport ID is required")
    private Long originAirportId;

    @NotNull(message = "Destination airport ID is required")
    private Long destinationAirportId;

    @NotNull(message = "Scheduled departure is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledDeparture;

    @NotNull(message = "Scheduled arrival is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledArrival;

    @Size(max = 10, message = "Gate number cannot exceed 10 characters")
    private String gateNumber;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @Min(value = 0, message = "Connection time cannot be negative")
    @Max(value = 1440, message = "Connection time cannot exceed 24 hours (1440 minutes)")
    private Integer connectionTimeMinutes;

    // Validation helper methods
    public boolean isTimingValid() {
        if (scheduledDeparture == null || scheduledArrival == null) {
            return false;
        }
        return scheduledArrival.isAfter(scheduledDeparture);
    }

    public boolean isAirportValid() {
        if (originAirportId == null || destinationAirportId == null) {
            return false;
        }
        return !originAirportId.equals(destinationAirportId);
    }

    public boolean hasValidConnectionTime() {
        return connectionTimeMinutes == null ||
                (connectionTimeMinutes >= 0 && connectionTimeMinutes <= 1440);
    }

    // Business logic helpers
    public Integer getFlightDurationMinutes() {
        if (scheduledDeparture != null && scheduledArrival != null) {
            return (int) java.time.Duration.between(scheduledDeparture, scheduledArrival).toMinutes();
        }
        return null;
    }

    public boolean isShortSegment() {
        Integer duration = getFlightDurationMinutes();
        return duration != null && duration <= 90; // 1.5 saat veya daha kısa
    }

    public boolean isLongSegment() {
        Integer duration = getFlightDurationMinutes();
        return duration != null && duration > 300; // 5 saatten uzun
    }

    public boolean isDomesticSegment() {
        // Bu metod airport bilgilerine göre implement edilebilir
        // Şimdilik false döndürüyoruz
        return false;
    }

    public boolean hasMinimumConnectionTime() {
        return connectionTimeMinutes == null || connectionTimeMinutes >= 30; // 30 dakika minimum
    }

    public boolean hasMaximumConnectionTime() {
        return connectionTimeMinutes == null || connectionTimeMinutes <= 1440; // 24 saat maksimum
    }

    // Display helpers
    public String getFormattedDuration() {
        Integer duration = getFlightDurationMinutes();
        if (duration == null) return "Unknown";

        int hours = duration / 60;
        int minutes = duration % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    public String getFormattedConnectionTime() {
        if (connectionTimeMinutes == null) return "No connection time";

        int hours = connectionTimeMinutes / 60;
        int minutes = connectionTimeMinutes % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m connection";
        } else {
            return minutes + "m connection";
        }
    }

    // Validation for business rules
    public boolean isValidForConnectingFlight() {
        return isTimingValid() &&
                isAirportValid() &&
                hasValidConnectionTime() &&
                hasMinimumConnectionTime() &&
                hasMaximumConnectionTime();
    }
}