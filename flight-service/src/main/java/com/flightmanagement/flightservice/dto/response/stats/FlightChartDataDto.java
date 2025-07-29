package com.flightmanagement.flightservice.dto.response.stats;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightChartDataDto {
    private List<String> dates = new ArrayList<>();
    private List<Long> scheduled = new ArrayList<>();
    private List<Long> departed = new ArrayList<>();
    private List<Long> arrived = new ArrayList<>();
    private List<Long> cancelled = new ArrayList<>();
    private List<Long> delayed = new ArrayList<>();

    // Simple data point için inner class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String date;
        private Long count;
    }

    // Constructor for single date-count pair
    public FlightChartDataDto(LocalDate date, long count) {
        this.dates.add(date.toString());
        this.scheduled.add(count);
    }

    // Helper method to add data point
    public void addDataPoint(LocalDate date, long scheduled, long departed, long arrived, long cancelled, long delayed) {
        this.dates.add(date.toString());
        this.scheduled.add(scheduled);
        this.departed.add(departed);
        this.arrived.add(arrived);
        this.cancelled.add(cancelled);
        this.delayed.add(delayed);
    }
}