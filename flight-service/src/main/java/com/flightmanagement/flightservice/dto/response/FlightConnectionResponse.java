package com.flightmanagement.flightservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightConnectionResponse {

    private Long id;
    private Long mainFlightId;
    private Long segmentFlightId;
    private Integer segmentOrder;
    private Integer connectionTimeMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    // Main flight info
    private String mainFlightNumber;

    // Segment flight info
    private String segmentFlightNumber;
    private FlightResponse segmentFlight;

    // Airport information
    private String originAirportCode;
    private String destinationAirportCode;
    private String originAirportName;
    private String destinationAirportName;

    // Route information
    private String routeCode;
    private String routePath;
    private Integer routeDistance;
    private Integer routeEstimatedTime;

    // Connection details
    private String connectionType; // "NORMAL", "TIGHT", "LONG", "OVERNIGHT"
    private Boolean isValidConnection;
    private String connectionWarning;

    // Timing information
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime segmentDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime segmentArrival;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime previousSegmentArrival;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime nextSegmentDeparture;

    // Helper methods
    public boolean hasConnectionTime() {
        return connectionTimeMinutes != null && connectionTimeMinutes > 0;
    }

    public boolean isTightConnection() {
        return connectionTimeMinutes != null && connectionTimeMinutes < 60; // < 1 saat
    }

    public boolean isLongConnection() {
        return connectionTimeMinutes != null && connectionTimeMinutes > 240; // > 4 saat
    }

    public boolean isOvernightConnection() {
        return connectionTimeMinutes != null && connectionTimeMinutes > 720; // > 12 saat
    }

    public String getConnectionCategory() {
        if (connectionTimeMinutes == null) return "UNKNOWN";

        if (connectionTimeMinutes < 30) return "TOO_TIGHT";
        else if (connectionTimeMinutes < 60) return "TIGHT";
        else if (connectionTimeMinutes <= 240) return "NORMAL";
        else if (connectionTimeMinutes <= 720) return "LONG";
        else return "OVERNIGHT";
    }

    public String getFormattedConnectionTime() {
        if (connectionTimeMinutes == null) return "No connection time";

        int hours = connectionTimeMinutes / 60;
        int minutes = connectionTimeMinutes % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    public String getConnectionDescription() {
        String category = getConnectionCategory();
        String time = getFormattedConnectionTime();

        switch (category) {
            case "TOO_TIGHT":
                return time + " (Very tight connection - risk of missing)";
            case "TIGHT":
                return time + " (Tight connection)";
            case "NORMAL":
                return time + " (Normal connection)";
            case "LONG":
                return time + " (Long connection)";
            case "OVERNIGHT":
                return time + " (Overnight connection)";
            default:
                return time;
        }
    }

    public Boolean isValidForPassengers() {
        // Yolcular için minimum 30 dakika connection time gerekli
        return connectionTimeMinutes == null || connectionTimeMinutes >= 30;
    }

    public Boolean isValidForBaggage() {
        // Bagaj transferi için minimum 45 dakika gerekli
        return connectionTimeMinutes == null || connectionTimeMinutes >= 45;
    }

    public String getRoute() {
        if (originAirportCode != null && destinationAirportCode != null) {
            return originAirportCode + " → " + destinationAirportCode;
        }
        return routePath;
    }

    public String getSegmentIdentifier() {
        return "Segment " + segmentOrder;
    }

    public String getFullSegmentInfo() {
        StringBuilder info = new StringBuilder();
        info.append(getSegmentIdentifier());

        if (segmentFlightNumber != null) {
            info.append(" (").append(segmentFlightNumber).append(")");
        }

        String route = getRoute();
        if (route != null) {
            info.append(": ").append(route);
        }

        return info.toString();
    }

    // Risk assessment
    public String getConnectionRisk() {
        if (connectionTimeMinutes == null) return "UNKNOWN";

        if (connectionTimeMinutes < 30) return "HIGH";
        else if (connectionTimeMinutes < 60) return "MEDIUM";
        else if (connectionTimeMinutes <= 240) return "LOW";
        else return "NONE";
    }

    public Boolean needsSpecialHandling() {
        return isTightConnection() || isOvernightConnection();
    }

    // Status helpers
    public Boolean isFirstSegment() {
        return segmentOrder != null && segmentOrder == 1;
    }

    public Boolean isLastSegment() {
        // Bu bilgi parent'tan gelmeli, şimdilik false
        return false;
    }

    public Boolean isIntermediateSegment() {
        return !isFirstSegment() && !isLastSegment();
    }
}