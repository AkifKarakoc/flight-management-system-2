package com.flightmanagement.flightarchiveservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class KpiResponse {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private Long totalFlights;
    private Double onTimePerformance;  // Percentage
    private Double averageDelay;       // Minutes
    private Double cancellationRate;   // Percentage
    private Double completionRate;     // Percentage

    private Map<String, Object> additionalMetrics;
}