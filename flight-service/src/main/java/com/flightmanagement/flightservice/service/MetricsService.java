package com.flightmanagement.flightservice.service;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final FlightService flightService;

    // Counters
    private Counter flightCreatedCounter;
    private Counter flightUpdatedCounter;
    private Counter flightDeletedCounter;
    private Counter csvUploadCounter;
    private Counter csvUploadErrorCounter;
    private Counter connectingFlightCreatedCounter;
    private Counter archiveOperationCounter;
    private Counter archiveOperationErrorCounter;
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Counter referenceServiceCallCounter;
    private Counter referenceServiceErrorCounter;

    // Timers
    private Timer flightCreationTimer;
    private Timer flightQueryTimer;
    private Timer csvProcessingTimer;
    private Timer referenceDataFetchTimer;
    private Timer archiveOperationTimer;

    // Gauges (for real-time values)
    private final AtomicLong activeFlightsGauge = new AtomicLong(0);
    private final AtomicLong delayedFlightsGauge = new AtomicLong(0);
    private final AtomicLong todayFlightsGauge = new AtomicLong(0);

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing custom metrics for Flight Service");

        // Initialize Counters
        flightCreatedCounter = Counter.builder("flights_created_total")
                .description("Total number of flights created")
                .tag("service", "flight-service")
                .register(meterRegistry);

        flightUpdatedCounter = Counter.builder("flights_updated_total")
                .description("Total number of flights updated")
                .tag("service", "flight-service")
                .register(meterRegistry);

        flightDeletedCounter = Counter.builder("flights_deleted_total")
                .description("Total number of flights deleted")
                .tag("service", "flight-service")
                .register(meterRegistry);

        csvUploadCounter = Counter.builder("csv_uploads_total")
                .description("Total number of CSV uploads")
                .tag("service", "flight-service")
                .register(meterRegistry);

        csvUploadErrorCounter = Counter.builder("csv_upload_errors_total")
                .description("Total number of CSV upload errors")
                .tag("service", "flight-service")
                .register(meterRegistry);

        connectingFlightCreatedCounter = Counter.builder("connecting_flights_created_total")
                .description("Total number of connecting flights created")
                .tag("service", "flight-service")
                .register(meterRegistry);

        archiveOperationCounter = Counter.builder("archive_operations_total")
                .description("Total number of archive operations")
                .tag("service", "flight-service")
                .register(meterRegistry);

        archiveOperationErrorCounter = Counter.builder("archive_operations_errors_total")
                .description("Total number of archive operation errors")
                .tag("service", "flight-service")
                .register(meterRegistry);

        cacheHitCounter = Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .tag("service", "flight-service")
                .register(meterRegistry);

        cacheMissCounter = Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .tag("service", "flight-service")
                .register(meterRegistry);

        referenceServiceCallCounter = Counter.builder("reference_service_calls_total")
                .description("Total number of reference service calls")
                .tag("service", "flight-service")
                .register(meterRegistry);

        referenceServiceErrorCounter = Counter.builder("reference_service_errors_total")
                .description("Total number of reference service errors")
                .tag("service", "flight-service")
                .register(meterRegistry);

        // Initialize Timers
        flightCreationTimer = Timer.builder("flights_creation_duration")
                .description("Time taken to create a flight")
                .tag("service", "flight-service")
                .register(meterRegistry);

        flightQueryTimer = Timer.builder("flights_query_duration")
                .description("Time taken to query flights")
                .tag("service", "flight-service")
                .register(meterRegistry);

        csvProcessingTimer = Timer.builder("csv_processing_duration")
                .description("Time taken to process CSV files")
                .tag("service", "flight-service")
                .register(meterRegistry);

        referenceDataFetchTimer = Timer.builder("reference_data_fetch_duration")
                .description("Time taken to fetch reference data")
                .tag("service", "flight-service")
                .register(meterRegistry);

        archiveOperationTimer = Timer.builder("archive_operation_duration")
                .description("Time taken for archive operations")
                .tag("service", "flight-service")
                .register(meterRegistry);

        // Initialize Gauges - DÜZELTME: Doğru sözdizimi
        Gauge.builder("flights_active_current", this, MetricsService::getCurrentActiveFlights)
                .description("Current number of active flights")
                .tag("service", "flight-service")
                .register(meterRegistry);

        Gauge.builder("flights_delayed_current", this, MetricsService::getCurrentDelayedFlights)
                .description("Current number of delayed flights")
                .tag("service", "flight-service")
                .register(meterRegistry);

        Gauge.builder("flights_today_total", this, MetricsService::getTodayFlightsCount)
                .description("Total flights for today")
                .tag("service", "flight-service")
                .register(meterRegistry);

        // JVM and System Gauges
        Gauge.builder("jvm_memory_usage_percent", this, MetricsService::getMemoryUsagePercent)
                .description("JVM memory usage percentage")
                .tag("service", "flight-service")
                .register(meterRegistry);

        log.info("Custom metrics initialized successfully");
    }

    // Counter increment methods
    public void incrementFlightCreated() {
        flightCreatedCounter.increment();
    }

    public void incrementFlightUpdated() {
        flightUpdatedCounter.increment();
    }

    public void incrementFlightDeleted() {
        flightDeletedCounter.increment();
    }

    public void incrementCsvUpload() {
        csvUploadCounter.increment();
    }

    public void incrementCsvUploadError() {
        csvUploadErrorCounter.increment();
    }

    public void incrementConnectingFlightCreated() {
        connectingFlightCreatedCounter.increment();
    }

    public void incrementArchiveOperation() {
        archiveOperationCounter.increment();
    }

    public void incrementArchiveOperationError() {
        archiveOperationErrorCounter.increment();
    }

    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    public void incrementReferenceServiceCall() {
        referenceServiceCallCounter.increment();
    }

    public void incrementReferenceServiceError() {
        referenceServiceErrorCounter.increment();
    }

    // Timer methods
    public Timer.Sample startFlightCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopFlightCreationTimer(Timer.Sample sample) {
        sample.stop(flightCreationTimer);
    }

    public Timer.Sample startFlightQueryTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopFlightQueryTimer(Timer.Sample sample) {
        sample.stop(flightQueryTimer);
    }

    public Timer.Sample startCsvProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopCsvProcessingTimer(Timer.Sample sample) {
        sample.stop(csvProcessingTimer);
    }

    public Timer.Sample startReferenceDataFetchTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopReferenceDataFetchTimer(Timer.Sample sample) {
        sample.stop(referenceDataFetchTimer);
    }

    public Timer.Sample startArchiveOperationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopArchiveOperationTimer(Timer.Sample sample) {
        sample.stop(archiveOperationTimer);
    }

    // Gauge value methods
    public double getCurrentActiveFlights() {
        try {
            // Count flights that are not arrived/cancelled
            return activeFlightsGauge.get();
        } catch (Exception e) {
            log.warn("Failed to get active flights count: {}", e.getMessage());
            return 0;
        }
    }

    public double getCurrentDelayedFlights() {
        try {
            return delayedFlightsGauge.get();
        } catch (Exception e) {
            log.warn("Failed to get delayed flights count: {}", e.getMessage());
            return 0;
        }
    }

    public double getTodayFlightsCount() {
        try {
            return todayFlightsGauge.get();
        } catch (Exception e) {
            log.warn("Failed to get today's flights count: {}", e.getMessage());
            return 0;
        }
    }

    public double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return (double) usedMemory / maxMemory * 100;
    }

    // Methods to update gauge values
    public void updateActiveFlightsGauge(long count) {
        activeFlightsGauge.set(count);
    }

    public void updateDelayedFlightsGauge(long count) {
        delayedFlightsGauge.set(count);
    }

    public void updateTodayFlightsGauge(long count) {
        todayFlightsGauge.set(count);
    }

    // Periodic gauge update method (should be called by scheduler)
    public void updateAllGauges() {
        try {
            // Update active flights
            long activeFlights = getCurrentActiveFlightsFromDB();
            updateActiveFlightsGauge(activeFlights);

            // Update delayed flights
            long delayedFlights = getCurrentDelayedFlightsFromDB();
            updateDelayedFlightsGauge(delayedFlights);

            // Update today's flights
            long todayFlights = getTodayFlightsFromDB();
            updateTodayFlightsGauge(todayFlights);

            log.debug("Updated gauge metrics - Active: {}, Delayed: {}, Today: {}",
                    activeFlights, delayedFlights, todayFlights);
        } catch (Exception e) {
            log.error("Failed to update gauge metrics: {}", e.getMessage());
        }
    }

    private long getCurrentActiveFlightsFromDB() {
        try {
            // Implementation to count active flights from DB
            return 0; // Placeholder
        } catch (Exception e) {
            log.warn("Failed to count active flights from DB: {}", e.getMessage());
            return 0;
        }
    }

    private long getCurrentDelayedFlightsFromDB() {
        try {
            // Implementation to count delayed flights from DB
            return 0; // Placeholder
        } catch (Exception e) {
            log.warn("Failed to count delayed flights from DB: {}", e.getMessage());
            return 0;
        }
    }

    private long getTodayFlightsFromDB() {
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            return flightService.getFlightCountByDate(today);
        } catch (Exception e) {
            log.warn("Failed to count today's flights from DB: {}", e.getMessage());
            return 0;
        }
    }

    // Custom business metrics
    public void recordFlightDelay(int delayMinutes) {
        Timer timer = Timer.builder("flight_delay_duration")
                .description("Flight delay duration in minutes")
                .tag("service", "flight-service")
                .register(meterRegistry);
        timer.record(delayMinutes, java.util.concurrent.TimeUnit.MINUTES);
    }

    public void recordCsvProcessingResult(int totalRows, int successCount, int errorCount) {
        // Register gauges dynamically using MeterRegistry directly
        meterRegistry.gauge("csv_last_processing_total_rows",
                Tags.of("service", "flight-service"),
                totalRows);

        meterRegistry.gauge("csv_last_processing_success_count",
                Tags.of("service", "flight-service"),
                successCount);

        meterRegistry.gauge("csv_last_processing_error_count",
                Tags.of("service", "flight-service"),
                errorCount);
    }
}