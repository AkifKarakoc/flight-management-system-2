package com.flightmanagement.flightservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flightmanagement.flightservice.config.FlightTimeDeserializer;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.entity.enums.FlightType;
import com.flightmanagement.flightservice.validator.ValidFlightCreation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidFlightCreation
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightRequest {

    @NotBlank(message = "Flight number is required")
    @Pattern(regexp = "^[A-Z]{2}\\d{1,4}$", message = "Flight number must be in format: TK123")
    @JsonProperty("flightNumber")
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    @JsonProperty("airlineId")
    private Long airlineId;

    @NotNull(message = "Aircraft ID is required")
    @JsonProperty("aircraftId")
    private Long aircraftId;

    // YENİ ROUTE SİSTEMİ - Route ID zorunlu alan
    @JsonProperty("routeId")
    private Long routeId;

    // Creation mode - ZORUNLU ALAN
    @NotBlank(message = "Creation mode is required")
    @Pattern(regexp = "ROUTE|AIRPORTS|MULTI_AIRPORTS",
            message = "Creation mode must be ROUTE, AIRPORTS, or MULTI_AIRPORTS")
    @JsonProperty("creationMode")
    private String creationMode;

    // Direct airport creation için (AIRPORTS mode)
    @JsonProperty("originAirportId")
    private Long originAirportId;

    @JsonProperty("destinationAirportId")
    private Long destinationAirportId;

    // Multi-segment airport creation için (MULTI_AIRPORTS mode)
    @Valid
    @Size(min = 2, max = 10, message = "Multi-segment flight must have 2-10 segments")
    @JsonProperty("airportSegments")
    private List<AirportSegmentRequest> airportSegments;

    @JsonProperty("isMultiSegmentAirportFlight")
    @Builder.Default
    private Boolean isMultiSegmentAirportFlight = false;

    // Flight Timing
    @NotNull(message = "Flight date is required")
    @FutureOrPresent(message = "Flight date cannot be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("flightDate")
    private LocalDate flightDate;

    @NotNull(message = "Scheduled departure is required")
    @JsonDeserialize(using = FlightTimeDeserializer.class)
    @JsonProperty("scheduledDeparture")
    private LocalDateTime scheduledDeparture;

    @NotNull(message = "Scheduled arrival is required")
    @JsonDeserialize(using = FlightTimeDeserializer.class)
    @JsonProperty("scheduledArrival")
    private LocalDateTime scheduledArrival;

    @JsonDeserialize(using = FlightTimeDeserializer.class)
    @JsonProperty("actualDeparture")
    private LocalDateTime actualDeparture;

    @JsonDeserialize(using = FlightTimeDeserializer.class)
    @JsonProperty("actualArrival")
    private LocalDateTime actualArrival;

    // Flight Status and Type
    @JsonProperty("status")
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @NotNull(message = "Flight type is required")
    @JsonProperty("type")
    private FlightType type;

    // Operational Details
    @Min(value = 0, message = "Passenger count cannot be negative")
    @Max(value = 1000, message = "Passenger count cannot exceed 1000")
    @JsonProperty("passengerCount")
    private Integer passengerCount;

    @Min(value = 0, message = "Cargo weight cannot be negative")
    @JsonProperty("cargoWeight")
    private Integer cargoWeight;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @JsonProperty("notes")
    private String notes;

    @Size(max = 10, message = "Gate number cannot exceed 10 characters")
    @JsonProperty("gateNumber")
    private String gateNumber;

    @Min(value = 0, message = "Delay minutes cannot be negative")
    @JsonProperty("delayMinutes")
    private Integer delayMinutes;

    @Size(max = 200, message = "Delay reason cannot exceed 200 characters")
    @JsonProperty("delayReason")
    private String delayReason;

    @JsonProperty("active")
    @Builder.Default
    private Boolean active = true;

    // Connecting Flight Fields (for future use)
    @JsonProperty("parentFlightId")
    private Long parentFlightId;

    @Min(value = 1, message = "Segment number must be at least 1")
    @JsonProperty("segmentNumber")
    @Builder.Default
    private Integer segmentNumber = 1;

    @JsonProperty("isConnectingFlight")
    @Builder.Default
    private Boolean isConnectingFlight = false;

    @Min(value = 0, message = "Connection time cannot be negative")
    @Max(value = 1440, message = "Connection time cannot exceed 24 hours (1440 minutes)")
    @JsonProperty("connectionTimeMinutes")
    private Integer connectionTimeMinutes;

    @Valid
    @JsonProperty("segments")
    private List<FlightSegmentRequest> segments;

    // ===========================================
    // HELPER METHODS
    // ===========================================

    /**
     * Route-based creation kontrolü
     */
    public boolean isRouteBasedCreation() {
        return "ROUTE".equals(creationMode);
    }

    /**
     * Direct airport creation kontrolü
     */
    public boolean isAirportBasedCreation() {
        return "AIRPORTS".equals(creationMode);
    }

    /**
     * Multi-segment airport creation kontrolü
     */
    public boolean isMultiSegmentAirportCreation() {
        return "MULTI_AIRPORTS".equals(creationMode);
    }

    /**
     * Connecting flight request kontrolü
     */
    public boolean isConnectingFlightRequest() {
        return Boolean.TRUE.equals(isConnectingFlight) && segments != null && !segments.isEmpty();
    }

    /**
     * Flight timing validation
     */
    public boolean isFlightTimeValid() {
        if (scheduledDeparture != null && scheduledArrival != null) {
            return scheduledArrival.isAfter(scheduledDeparture);
        }
        return true;
    }

    /**
     * Actual timing validation
     */
    public boolean areActualTimesValid() {
        if (actualDeparture != null && actualArrival != null) {
            return actualArrival.isAfter(actualDeparture);
        }
        return true;
    }

    /**
     * Flight type consistency kontrolü
     */
    public boolean isFlightTypeConsistent() {
        if (type == null) return true;

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

    /**
     * Creation mode validation
     */
    public boolean hasValidCreationMode() {
        if (creationMode == null || creationMode.trim().isEmpty()) {
            return false;
        }

        switch (creationMode) {
            case "ROUTE":
                return routeId != null;

            case "AIRPORTS":
                return originAirportId != null &&
                        destinationAirportId != null &&
                        !originAirportId.equals(destinationAirportId);

            case "MULTI_AIRPORTS":
                return airportSegments != null &&
                        airportSegments.size() >= 2 &&
                        airportSegments.size() <= 10 &&
                        validateAirportSegmentsSequence();

            default:
                return false;
        }
    }

    /**
     * Airport segments sequence validation
     */
    private boolean validateAirportSegmentsSequence() {
        if (airportSegments == null || airportSegments.size() < 2) {
            return false;
        }

        // Segment order validation
        for (int i = 0; i < airportSegments.size(); i++) {
            AirportSegmentRequest segment = airportSegments.get(i);

            if (segment.getSegmentOrder() == null || segment.getSegmentOrder() != (i + 1)) {
                return false;
            }

            if (segment.getOriginAirportId() == null ||
                    segment.getDestinationAirportId() == null ||
                    segment.getOriginAirportId().equals(segment.getDestinationAirportId())) {
                return false;
            }
        }

        // Continuity validation
        for (int i = 0; i < airportSegments.size() - 1; i++) {
            AirportSegmentRequest current = airportSegments.get(i);
            AirportSegmentRequest next = airportSegments.get(i + 1);

            if (!current.getDestinationAirportId().equals(next.getOriginAirportId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validation errors listesi
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (creationMode == null || creationMode.trim().isEmpty()) {
            errors.add("Creation mode is required");
            return errors;
        }

        switch (creationMode) {
            case "ROUTE":
                if (routeId == null) {
                    errors.add("Route ID is required for route-based creation");
                }
                break;

            case "AIRPORTS":
                if (originAirportId == null) {
                    errors.add("Origin airport ID is required for airport-based creation");
                }
                if (destinationAirportId == null) {
                    errors.add("Destination airport ID is required for airport-based creation");
                }
                if (originAirportId != null && destinationAirportId != null &&
                        originAirportId.equals(destinationAirportId)) {
                    errors.add("Origin and destination airports cannot be the same");
                }
                break;

            case "MULTI_AIRPORTS":
                if (airportSegments == null || airportSegments.isEmpty()) {
                    errors.add("Airport segments are required for multi-segment creation");
                } else {
                    if (airportSegments.size() < 2) {
                        errors.add("Multi-segment creation requires at least 2 segments");
                    }
                    if (airportSegments.size() > 10) {
                        errors.add("Multi-segment creation cannot have more than 10 segments");
                    }
                    errors.addAll(validateSegmentDetails());
                }
                break;

            default:
                errors.add("Invalid creation mode: " + creationMode);
        }

        return errors;
    }

    /**
     * Segment detail validation
     */
    private List<String> validateSegmentDetails() {
        List<String> errors = new ArrayList<>();

        if (airportSegments == null) {
            return errors;
        }

        for (int i = 0; i < airportSegments.size(); i++) {
            AirportSegmentRequest segment = airportSegments.get(i);

            if (segment.getSegmentOrder() == null) {
                errors.add("Segment " + (i + 1) + " is missing segment order");
            } else if (segment.getSegmentOrder() != (i + 1)) {
                errors.add("Segment " + (i + 1) + " has incorrect segment order");
            }

            if (segment.getOriginAirportId() == null) {
                errors.add("Segment " + (i + 1) + " is missing origin airport ID");
            }

            if (segment.getDestinationAirportId() == null) {
                errors.add("Segment " + (i + 1) + " is missing destination airport ID");
            }

            if (segment.getOriginAirportId() != null && segment.getDestinationAirportId() != null &&
                    segment.getOriginAirportId().equals(segment.getDestinationAirportId())) {
                errors.add("Segment " + (i + 1) + " origin and destination airports cannot be the same");
            }
        }

        // Continuity check
        for (int i = 0; i < airportSegments.size() - 1; i++) {
            AirportSegmentRequest current = airportSegments.get(i);
            AirportSegmentRequest next = airportSegments.get(i + 1);

            if (current.getDestinationAirportId() != null && next.getOriginAirportId() != null &&
                    !current.getDestinationAirportId().equals(next.getOriginAirportId())) {
                errors.add("Route continuity broken: Segment " + (i + 1) + " destination must match Segment " +
                        (i + 2) + " origin");
            }
        }

        return errors;
    }

    /**
     * Conflicting data kontrolü
     */
    public boolean hasConflictingData() {
        int modeCount = 0;

        if (isRouteBasedCreation() && routeId != null) modeCount++;
        if (isAirportBasedCreation() && originAirportId != null && destinationAirportId != null) modeCount++;
        if (isMultiSegmentAirportCreation() && airportSegments != null && !airportSegments.isEmpty()) modeCount++;

        return modeCount > 1;
    }

    /**
     * Valid flight data kontrolü
     */
    public boolean hasValidFlightData() {
        return hasValidCreationMode() && !hasConflictingData();
    }

    /**
     * Flight duration hesaplama
     */
    public Integer getEstimatedDurationMinutes() {
        if (scheduledDeparture != null && scheduledArrival != null) {
            return (int) java.time.Duration.between(scheduledDeparture, scheduledArrival).toMinutes();
        }
        return null;
    }

    /**
     * Business logic helpers
     */
    public boolean isCargoFlight() {
        return FlightType.CARGO.equals(type);
    }

    public boolean isPassengerFlight() {
        return FlightType.PASSENGER.equals(type);
    }

    /**
     * Multi-segment helpers
     */
    public Long getFirstOriginAirportId() {
        if (isMultiSegmentAirportCreation() && airportSegments != null && !airportSegments.isEmpty()) {
            return airportSegments.get(0).getOriginAirportId();
        }
        return originAirportId;
    }

    public Long getLastDestinationAirportId() {
        if (isMultiSegmentAirportCreation() && airportSegments != null && !airportSegments.isEmpty()) {
            return airportSegments.get(airportSegments.size() - 1).getDestinationAirportId();
        }
        return destinationAirportId;
    }

    /**
     * Route/mode description
     */
    public String getCreationModeDescription() {
        switch (creationMode != null ? creationMode : "UNKNOWN") {
            case "ROUTE":
                return "Using existing route (ID: " + routeId + ")";
            case "AIRPORTS":
                return "Direct flight between airports (ID: " + originAirportId + " → " + destinationAirportId + ")";
            case "MULTI_AIRPORTS":
                return "Multi-segment flight with " + (airportSegments != null ? airportSegments.size() : 0) + " segments";
            default:
                return "Unknown creation mode: " + creationMode;
        }
    }

    public int getSegmentCount() {
        if (isMultiSegmentAirportCreation() && airportSegments != null) {
            return airportSegments.size();
        }
        return 1;
    }

    public int getStopCount() {
        return getSegmentCount() - 1;
    }

    public boolean isComplexRoute() {
        return isMultiSegmentAirportCreation() && getSegmentCount() > 3;
    }

    /**
     * Request type detection for debugging
     */
    public String getRequestType() {
        if (isRouteBasedCreation()) {
            return "ROUTE_BASED";
        } else if (isAirportBasedCreation()) {
            return "AIRPORT_BASED";
        } else if (isMultiSegmentAirportCreation()) {
            return "MULTI_SEGMENT";
        } else if (isConnectingFlightRequest()) {
            return "CONNECTING_FLIGHT";
        } else {
            return "UNKNOWN";
        }
    }

    // ===========================================
    // VALIDATION GROUPS
    // ===========================================
    public interface RouteBasedValidation {}
    public interface AirportBasedValidation {}
    public interface MultiAirportValidation {}
}