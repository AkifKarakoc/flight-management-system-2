package com.flightmanagement.flightservice.dto.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteCache implements Serializable {
    private Long id;
    private String routeCode;           // "IST-ANK-001"
    private String routeName;           // "Istanbul-Ankara Route"
    private String routePath;           // "IST → ANK → IZM"
    private String routeType;           // DOMESTIC, INTERNATIONAL
    private Integer distance;           // km
    private Integer estimatedFlightTime; // dakika
    private Boolean isMultiSegment;     // Tek mi, çoklu segment mi?
    private Integer segmentCount;       // Segment sayısı
    private Boolean active;             // Route aktif mi?

    // Origin/Destination info (from route)
    private Long originAirportId;
    private String originAirportCode;   // "IST"
    private String originAirportName;   // "Istanbul Airport"

    private Long destinationAirportId;
    private String destinationAirportCode; // "ANK"
    private String destinationAirportName; // "Ankara Airport"

    // Route visibility and ownership
    private String visibility;          // PRIVATE, SHARED, PUBLIC
    private Long airlineId;            // Hangi havayolu için
    private Long createdByUserId;      // Route sahibi

    // Performance metrics
    private Double averageDelayMinutes; // Bu route'da ortalama gecikme
    private Integer totalFlights;       // Bu route'da toplam uçuş sayısı
    private Double onTimePerformance;   // On-time performance %

    // Weather and operational factors
    private String weatherRisk;         // LOW, MEDIUM, HIGH
    private String operationalComplexity; // SIMPLE, MODERATE, COMPLEX
    private String trafficDensity;      // LOW, MEDIUM, HIGH

    // Helper methods
    public String getSimpleRoute() {
        if (originAirportCode != null && destinationAirportCode != null) {
            return originAirportCode + " → " + destinationAirportCode;
        }
        return routePath != null ? routePath : routeCode;
    }

    public String getRouteDescription() {
        if (routeName != null && !routeName.isEmpty()) {
            return routeName;
        }
        return getSimpleRoute();
    }

    public boolean isDomestic() {
        return "DOMESTIC".equals(routeType);
    }

    public boolean isInternational() {
        return "INTERNATIONAL".equals(routeType);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public boolean isMultiSegmentRoute() {
        return Boolean.TRUE.equals(isMultiSegment);
    }

    public boolean isPrivateRoute() {
        return "PRIVATE".equals(visibility);
    }

    public boolean isSharedRoute() {
        return "SHARED".equals(visibility);
    }

    public boolean isPublicRoute() {
        return "PUBLIC".equals(visibility);
    }

    // Distance categories
    public boolean isShortHaul() {
        return distance != null && distance <= 1500; // <= 1500 km
    }

    public boolean isMediumHaul() {
        return distance != null && distance > 1500 && distance <= 4000; // 1500-4000 km
    }

    public boolean isLongHaul() {
        return distance != null && distance > 4000; // > 4000 km
    }

    // Duration categories
    public boolean isShortFlight() {
        return estimatedFlightTime != null && estimatedFlightTime <= 180; // <= 3 hours
    }

    public boolean isMediumFlight() {
        return estimatedFlightTime != null && estimatedFlightTime > 180 && estimatedFlightTime <= 480; // 3-8 hours
    }

    public boolean isLongFlight() {
        return estimatedFlightTime != null && estimatedFlightTime > 480; // > 8 hours
    }

    // Performance indicators
    public String getPerformanceRating() {
        if (onTimePerformance == null) return "UNKNOWN";

        if (onTimePerformance >= 90) return "EXCELLENT";
        else if (onTimePerformance >= 80) return "GOOD";
        else if (onTimePerformance >= 70) return "AVERAGE";
        else return "POOR";
    }

    public String getDelayRisk() {
        if (averageDelayMinutes == null) return "UNKNOWN";

        if (averageDelayMinutes <= 5) return "LOW";
        else if (averageDelayMinutes <= 15) return "MEDIUM";
        else return "HIGH";
    }

    // Operational complexity
    public String getComplexityLevel() {
        if (operationalComplexity != null) {
            return operationalComplexity;
        }

        // Calculate based on route characteristics
        int complexityScore = 0;

        if (isInternational()) complexityScore += 2;
        if (isLongHaul()) complexityScore += 2;
        if (isMultiSegmentRoute()) complexityScore += 3;
        if ("HIGH".equals(trafficDensity)) complexityScore += 1;
        if ("HIGH".equals(weatherRisk)) complexityScore += 1;

        if (complexityScore <= 2) return "SIMPLE";
        else if (complexityScore <= 5) return "MODERATE";
        else return "COMPLEX";
    }

    // Route efficiency
    public Double getEstimatedSpeed() {
        if (distance != null && estimatedFlightTime != null && estimatedFlightTime > 0) {
            return (double) distance / (estimatedFlightTime / 60.0); // km/h
        }
        return null;
    }

    // Display helpers
    public String getFormattedDistance() {
        if (distance == null) return "Unknown";
        return distance + " km";
    }

    public String getFormattedDuration() {
        if (estimatedFlightTime == null) return "Unknown";

        int hours = estimatedFlightTime / 60;
        int minutes = estimatedFlightTime % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    public String getRouteIdentifier() {
        return routeCode != null ? routeCode : (id != null ? "ROUTE-" + id : "UNKNOWN");
    }

    // Validation helpers
    public boolean isValid() {
        return id != null &&
                routeCode != null && !routeCode.isEmpty() &&
                originAirportId != null &&
                destinationAirportId != null &&
                !originAirportId.equals(destinationAirportId);
    }

    public boolean hasValidTiming() {
        return estimatedFlightTime != null && estimatedFlightTime > 0;
    }

    public boolean hasValidDistance() {
        return distance != null && distance > 0;
    }
}