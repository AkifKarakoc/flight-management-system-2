package com.flightmanagement.flightservice.dto.request;

import com.flightmanagement.flightservice.entity.enums.FlightType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ConnectingFlightRequest {

    @NotBlank(message = "Main flight number is required")
    @Pattern(regexp = "^[A-Z]{2}\\d{1,4}$", message = "Flight number must be in format: TK123")
    private String mainFlightNumber;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    @NotNull(message = "Flight type is required")
    private FlightType type;

    @Min(value = 0, message = "Passenger count cannot be negative")
    @Max(value = 1000, message = "Passenger count cannot exceed 1000")
    private Integer passengerCount;

    @Min(value = 0, message = "Cargo weight cannot be negative")
    private Integer cargoWeight;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private Boolean active = true;

    @NotNull(message = "Segments are required")
    @Size(min = 2, max = 10, message = "Connecting flight must have between 2 and 10 segments")
    @Valid
    private List<FlightSegmentRequest> segments;

    // Validation helper methods
    public boolean hasValidSegmentCount() {
        return segments != null && segments.size() >= 2 && segments.size() <= 10;
    }

    public boolean isFlightTypeConsistent() {
        switch (type) {
            case CARGO:
                return (passengerCount == null || passengerCount == 0) &&
                        (cargoWeight != null && cargoWeight > 0);
            case PASSENGER:
                return passengerCount != null && passengerCount > 0;
            case POSITIONING:
            case FERRY:
            case TRAINING:
                return passengerCount == null || passengerCount == 0;
            default:
                return true;
        }
    }

    public boolean hasValidSegmentSequence() {
        if (segments == null || segments.size() < 2) {
            return false;
        }

        for (int i = 0; i < segments.size(); i++) {
            FlightSegmentRequest segment = segments.get(i);

            // Segment number kontrolü
            if (segment.getSegmentNumber() == null || segment.getSegmentNumber() != (i + 1)) {
                return false;
            }

            // Son segment değilse, sonraki segment ile connection kontrolü
            if (i < segments.size() - 1) {
                FlightSegmentRequest nextSegment = segments.get(i + 1);

                // Destination = Next Origin kontrolü
                if (!segment.getDestinationAirportId().equals(nextSegment.getOriginAirportId())) {
                    return false;
                }

                // Timing kontrolü
                if (!segment.getScheduledArrival().isBefore(nextSegment.getScheduledDeparture())) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getTotalSegments() {
        return segments != null ? segments.size() : 0;
    }

    public FlightSegmentRequest getFirstSegment() {
        return segments != null && !segments.isEmpty() ? segments.get(0) : null;
    }

    public FlightSegmentRequest getLastSegment() {
        return segments != null && !segments.isEmpty() ?
                segments.get(segments.size() - 1) : null;
    }

    public Long getOriginAirportId() {
        FlightSegmentRequest firstSegment = getFirstSegment();
        return firstSegment != null ? firstSegment.getOriginAirportId() : null;
    }

    public Long getDestinationAirportId() {
        FlightSegmentRequest lastSegment = getLastSegment();
        return lastSegment != null ? lastSegment.getDestinationAirportId() : null;
    }

    // Business logic helpers
    public boolean isCargoFlight() {
        return FlightType.CARGO.equals(type);
    }

    public boolean isPassengerFlight() {
        return FlightType.PASSENGER.equals(type);
    }

    public boolean isSpecialFlight() {
        return FlightType.POSITIONING.equals(type) ||
                FlightType.FERRY.equals(type) ||
                FlightType.TRAINING.equals(type);
    }
}