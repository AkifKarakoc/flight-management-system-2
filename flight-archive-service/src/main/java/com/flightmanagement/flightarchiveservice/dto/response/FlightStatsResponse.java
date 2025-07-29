package com.flightmanagement.flightarchiveservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightStatsResponse {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private Long totalFlights;
    private Long arrivedFlights;
    private Long departedFlights;
    private Long cancelledFlights;
    private Long delayedFlights;
    private Double averageDelayMinutes;

    // Computed metrics
    public Double getOnTimePerformance() {
        if (totalFlights == 0) return 0.0;
        return (double) (totalFlights - delayedFlights) / totalFlights * 100;
    }

    public Double getCancellationRate() {
        if (totalFlights == 0) return 0.0;
        return (double) cancelledFlights / totalFlights * 100;
    }

    public Double getCompletionRate() {
        if (totalFlights == 0) return 0.0;
        return (double) arrivedFlights / totalFlights * 100;
    }
}