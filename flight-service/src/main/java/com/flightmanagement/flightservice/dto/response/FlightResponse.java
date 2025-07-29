package com.flightmanagement.flightservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.entity.enums.FlightType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightResponse {

    // Temel uçuş bilgileri
    private Long id;
    private String flightNumber;
    private AirlineCache airline;
    private AircraftCache aircraft;

    // YENİ: Route bilgisi (Ana sistem)
    private RouteCache route;
    private String routePath;           // "IST → ANK → IZM" şeklinde route path
    private Integer routeDistance;      // Route'un toplam mesafesi (km)
    private Integer routeEstimatedTime; // Route'un tahmini süresi (dakika)
    private Boolean isMultiSegmentRoute; // Route çoklu segment mi?

    // Backward compatibility için airport bilgileri
    private AirportCache originAirport;
    private AirportCache destinationAirport;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate flightDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledArrival;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime actualDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime actualArrival;

    private FlightStatus status;
    private FlightType type;

    // Operasyonel bilgiler
    private Integer passengerCount;
    private Integer cargoWeight;
    private String notes;
    private String gateNumber;
    private Integer delayMinutes;
    private String delayReason;
    private Boolean active;

    // Hesaplanan alanlar
    private Integer flightDuration;     // Gerçek veya tahmini uçuş süresi (dakika)
    private Boolean isDelayed;          // Gecikme durumu
    private String delayStatus;         // "ON_TIME", "DELAYED", "SEVERELY_DELAYED"
    private Double onTimePerformance;   // Bu uçuş için on-time performance

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;

    // Aktarmalı uçuş alanları
    private Long parentFlightId;
    private Integer segmentNumber;
    private Boolean isConnectingFlight;
    private Integer connectionTimeMinutes;
    private List<FlightResponse> connectingFlights;  // Segment'ler
    private Integer totalSegments;                   // Toplam segment sayısı
    private String fullRoute;                        // Tam route string'i

    // Performance ve analytics alanları
    private String flightPhase;         // "PRE_FLIGHT", "BOARDING", "AIRBORNE", "LANDED"
    private Integer punctualityScore;   // 0-100 arası punctuality skoru
    private String weatherImpact;       // "NONE", "MINOR", "MAJOR"
    private String operationalStatus;   // "NORMAL", "DELAYED", "CANCELLED", "DIVERTED"

    // Route performance
    private String routeEfficiency;     // "OPTIMAL", "GOOD", "SUBOPTIMAL"
    private Integer actualDistance;     // Gerçek uçulan mesafe (if different from route)
    private String alternateRoute;      // Eğer alternatif route kullanıldıysa

    // Helper methods
    public boolean isRouteBasedFlight() {
        return route != null;
    }

    public boolean isLegacyFlight() {
        return route == null && originAirport != null && destinationAirport != null;
    }

    public boolean hasConnections() {
        return Boolean.TRUE.equals(isConnectingFlight) &&
                connectingFlights != null && !connectingFlights.isEmpty();
    }

    public String getDisplayRoute() {
        if (routePath != null && !routePath.isEmpty()) {
            return routePath;
        } else if (originAirport != null && destinationAirport != null) {
            return originAirport.getIataCode() + " → " + destinationAirport.getIataCode();
        }
        return "Unknown Route";
    }

    public String getShortRoute() {
        if (route != null) {
            return route.getSimpleRoute();
        } else if (originAirport != null && destinationAirport != null) {
            return originAirport.getIataCode() + "-" + destinationAirport.getIataCode();
        }
        return "???";
    }

    // Flight status helpers
    public boolean isDeparted() {
        return FlightStatus.DEPARTED.equals(status) || FlightStatus.ARRIVED.equals(status);
    }

    public boolean isCompleted() {
        return FlightStatus.ARRIVED.equals(status);
    }

    public boolean isCancelled() {
        return FlightStatus.CANCELLED.equals(status);
    }

    public boolean isActive() {
        return FlightStatus.BOARDING.equals(status) ||
                FlightStatus.DEPARTED.equals(status);
    }

    // Delay analysis
    public String getDelayCategory() {
        if (delayMinutes == null || delayMinutes <= 0) {
            return "ON_TIME";
        } else if (delayMinutes <= 15) {
            return "MINOR_DELAY";
        } else if (delayMinutes <= 60) {
            return "MODERATE_DELAY";
        } else {
            return "MAJOR_DELAY";
        }
    }

    public boolean isSignificantlyDelayed() {
        return delayMinutes != null && delayMinutes > 30;
    }

    // Flight type helpers
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

    // Route analysis
    public boolean isDomesticFlight() {
        return route != null && route.isDomestic();
    }

    public boolean isInternationalFlight() {
        return route != null && route.isInternational();
    }

    public boolean isLongHaulFlight() {
        return routeDistance != null && routeDistance > 3000; // 3000+ km
    }

    public boolean isShortHaulFlight() {
        return routeDistance != null && routeDistance <= 1000; // <1000 km
    }

    // Performance metrics
    public Double getRouteUtilization() {
        if (routeDistance != null && actualDistance != null && routeDistance > 0) {
            return (double) actualDistance / routeDistance * 100;
        }
        return null;
    }

    public String getEfficiencyRating() {
        if (flightDuration != null && routeEstimatedTime != null && routeEstimatedTime > 0) {
            double ratio = (double) flightDuration / routeEstimatedTime;
            if (ratio <= 1.05) return "EXCELLENT";
            else if (ratio <= 1.15) return "GOOD";
            else if (ratio <= 1.30) return "AVERAGE";
            else return "POOR";
        }
        return "UNKNOWN";
    }

    // Connecting flight helpers
    public boolean isMainConnectingFlight() {
        return Boolean.TRUE.equals(isConnectingFlight) && parentFlightId == null;
    }

    public boolean isConnectingSegment() {
        return parentFlightId != null;
    }

    public String getSegmentInfo() {
        if (isConnectingSegment() && segmentNumber != null && totalSegments != null) {
            return "Segment " + segmentNumber + " of " + totalSegments;
        }
        return null;
    }

    // Display helpers
    public String getFormattedFlightNumber() {
        if (airline != null && airline.getIataCode() != null) {
            return airline.getIataCode() + flightNumber.substring(2);
        }
        return flightNumber;
    }

    public String getFlightDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(getFormattedFlightNumber());
        desc.append(" - ").append(getDisplayRoute());

        if (type != null) {
            desc.append(" (").append(type.name()).append(")");
        }

        return desc.toString();
    }

    public String getStatusBadgeColor() {
        if (status == null) return "gray";

        switch (status) {
            case SCHEDULED: return "blue";
            case BOARDING: return "orange";
            case DEPARTED: return "green";
            case ARRIVED: return "success";
            case DELAYED: return "yellow";
            case CANCELLED: return "red";
            default: return "gray";
        }
    }

    // Real-time information
    public String getCurrentPhase() {
        if (status == null) return "UNKNOWN";

        switch (status) {
            case SCHEDULED:
                return LocalDateTime.now().isBefore(scheduledDeparture.minusHours(2)) ?
                        "PRE_FLIGHT" : "PREPARING";
            case BOARDING:
                return "BOARDING";
            case DEPARTED:
                return isCompleted() ? "COMPLETED" : "AIRBORNE";
            case ARRIVED:
                return "COMPLETED";
            case DELAYED:
                return "DELAYED";
            case CANCELLED:
                return "CANCELLED";
            default:
                return "UNKNOWN";
        }
    }

    public Integer getMinutesUntilDeparture() {
        if (scheduledDeparture != null) {
            long minutes = java.time.Duration.between(LocalDateTime.now(), scheduledDeparture).toMinutes();
            return minutes > 0 ? (int) minutes : null;
        }
        return null;
    }

    public Integer getMinutesSinceDeparture() {
        if (actualDeparture != null) {
            long minutes = java.time.Duration.between(actualDeparture, LocalDateTime.now()).toMinutes();
            return minutes > 0 ? (int) minutes : null;
        }
        return null;
    }
}