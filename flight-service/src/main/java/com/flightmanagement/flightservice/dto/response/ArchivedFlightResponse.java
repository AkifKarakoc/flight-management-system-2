package com.flightmanagement.flightservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivedFlightResponse {

    private Long id;
    private String flightNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate flightDate;

    private String status;
    private String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archivedAt;

    // Basic flight info extracted from payload
    private String airlineName;
    private String originAirportCode;
    private String destinationAirportCode;
    private String routeCode;

    // Full payload (optional, for detailed views)
    private Map<String, Object> payload;

    // Archive metadata
    private String archivedBy;
    private String archiveReason;
    private String sourceService;
    private String dataVersion;

    // Helper methods
    public String getRoute() {
        if (originAirportCode != null && destinationAirportCode != null) {
            return originAirportCode + " → " + destinationAirportCode;
        }
        return routeCode;
    }

    public boolean hasPayload() {
        return payload != null && !payload.isEmpty();
    }

    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        name.append(flightNumber);

        if (airlineName != null) {
            name.append(" (").append(airlineName).append(")");
        }

        String route = getRoute();
        if (route != null) {
            name.append(" - ").append(route);
        }

        return name.toString();
    }
}