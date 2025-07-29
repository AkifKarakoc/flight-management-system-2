package com.flightmanagement.flightarchiveservice.service;

import com.flightmanagement.flightarchiveservice.dto.response.FlightStatsResponse;
import com.flightmanagement.flightarchiveservice.dto.response.KpiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KpiCalculationService {

    private final FlightArchiveService flightArchiveService;

    @Scheduled(fixedRateString = "${archive.kpi.calculation.interval:3600000}") // 1 hour
    public void calculateDailyKpis() {
        log.info("Starting daily KPI calculation...");

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        try {
            calculateKpisForDate(yesterday);
            log.info("Daily KPI calculation completed for {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to calculate daily KPIs for {}", yesterday, e);
        }
    }

    public KpiResponse calculateKpisForDate(LocalDate date) {
        log.debug("Calculating KPIs for date: {}", date);

        FlightStatsResponse stats = flightArchiveService.getFlightStatistics(date);

        KpiResponse kpi = new KpiResponse();
        kpi.setDate(date);
        kpi.setTotalFlights(stats.getTotalFlights());
        kpi.setOnTimePerformance(calculateOnTimePerformance(stats));
        kpi.setAverageDelay(stats.getAverageDelayMinutes());
        kpi.setCancellationRate(calculateCancellationRate(stats));
        kpi.setCompletionRate(calculateCompletionRate(stats));

        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("delayedFlights", stats.getDelayedFlights());
        additionalMetrics.put("departedFlights", stats.getDepartedFlights());
        additionalMetrics.put("arrivedFlights", stats.getArrivedFlights());
        kpi.setAdditionalMetrics(additionalMetrics);

        return kpi;
    }

    private Double calculateOnTimePerformance(FlightStatsResponse stats) {
        if (stats.getTotalFlights() == 0) return 0.0;

        long onTimeFlights = stats.getTotalFlights() - stats.getDelayedFlights();
        return (double) onTimeFlights / stats.getTotalFlights() * 100;
    }

    private Double calculateCancellationRate(FlightStatsResponse stats) {
        if (stats.getTotalFlights() == 0) return 0.0;

        return (double) stats.getCancelledFlights() / stats.getTotalFlights() * 100;
    }

    private Double calculateCompletionRate(FlightStatsResponse stats) {
        if (stats.getTotalFlights() == 0) return 0.0;

        return (double) stats.getArrivedFlights() / stats.getTotalFlights() * 100;
    }
}