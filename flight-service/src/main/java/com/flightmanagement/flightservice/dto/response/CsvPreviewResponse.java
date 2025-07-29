package com.flightmanagement.flightservice.dto.response;


import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import com.flightmanagement.flightservice.entity.enums.FlightType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class CsvPreviewResponse {

    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<PreviewRow> previewData;
    private List<String> globalErrors;
    private boolean readyForImport;

    @Data
    @NoArgsConstructor
    public static class PreviewRow {
        private int rowNumber;
        private ParsedFlightData parsedData;
        private Map<String, String> fieldErrors;
        private List<String> warnings;
        private boolean valid;
    }

    @Data
    @NoArgsConstructor
    public static class ParsedFlightData {
        private String flightNumber;
        private Long airlineId;
        private Long aircraftId;
        private String routeInput;
        private LocalDate flightDate;
        private LocalDateTime scheduledDeparture;
        private LocalDateTime scheduledArrival;
        private FlightType type;

        // NEW FIELDS - Add these
        private Integer delayMinutes;
        private FlightStatus status;
        private Integer passengerCount;
        private Integer cargoWeight;

        // Route processing results
        private String creationMode; // "ROUTE" or "AIRPORTS"
        private Long routeId; // for ROUTE mode
        private Long originAirportId; // for AIRPORTS mode
        private Long destinationAirportId; // for AIRPORTS mode
        private String routeInfo; // Display string for frontend
    }
}