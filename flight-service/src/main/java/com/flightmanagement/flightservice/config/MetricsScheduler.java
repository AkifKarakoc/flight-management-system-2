package com.flightmanagement.flightservice.config;

import com.flightmanagement.flightservice.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "metrics.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsScheduler {

    private final MetricsService metricsService;

    @Scheduled(fixedRate = 30000) // Her 30 saniyede bir
    public void updateMetrics() {
        log.debug("Updating metrics gauges");
        try {
            metricsService.updateAllGauges();
        } catch (Exception e) {
            log.error("Error updating metrics: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 */5 * * * *") // Her 5 dakikada bir
    public void logMetricsSummary() {
        log.info("Metrics Summary - Active Flights: {}, Delayed Flights: {}, Today's Flights: {}, Memory Usage: {}%",
                metricsService.getCurrentActiveFlights(),
                metricsService.getCurrentDelayedFlights(),
                metricsService.getTodayFlightsCount(),
                Math.round(metricsService.getMemoryUsagePercent()));
    }
}