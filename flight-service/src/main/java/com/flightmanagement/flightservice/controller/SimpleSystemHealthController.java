package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.health.SimpleDependencyHealthIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
public class SimpleSystemHealthController {

    private final SimpleDependencyHealthIndicator dependencyHealthIndicator;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        log.debug("System health check requested");

        SimpleDependencyHealthIndicator.SimpleHealthResponse health = dependencyHealthIndicator.checkHealth();

        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus());
        response.put("healthy", health.isHealthy());
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "flight-service");
        response.put("version", "2.0-ROUTE-BASED");
        response.put("dependencies", health.getDetails());

        // HTTP status code'u health durumuna göre belirle
        HttpStatus httpStatus = health.isHealthy() ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return new ResponseEntity<>(response, httpStatus);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // Service information
        info.put("service", "flight-service");
        info.put("version", "2.0-ROUTE-BASED");
        info.put("description", "Route-based Flight Management System");
        info.put("architecture", "Microservice");
        info.put("database", "MySQL");
        info.put("cache", "Redis");
        info.put("messaging", "Apache Kafka");

        // Runtime information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("processors", runtime.availableProcessors());
        info.put("jvm", jvm);

        // Build information
        Map<String, Object> build = new HashMap<>();
        build.put("timestamp", LocalDateTime.now());
        build.put("javaVersion", System.getProperty("java.version"));
        build.put("springBootVersion", "3.5.3");
        info.put("build", build);

        // Features
        Map<String, Boolean> features = new HashMap<>();
        features.put("routeBasedFlights", true);
        features.put("connectingFlights", true);
        features.put("csvUpload", true);
        features.put("realTimeUpdates", true);
        features.put("caching", true);
        features.put("eventStreaming", true);
        info.put("features", features);

        // Dependencies
        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("referenceManager", "http://localhost:8081");
        dependencies.put("archiveService", "http://localhost:8083");
        dependencies.put("database", "mysql://localhost:3308");
        dependencies.put("redis", "redis://localhost:6379");
        dependencies.put("kafka", "kafka://localhost:9092");
        info.put("dependencies", dependencies);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        SimpleDependencyHealthIndicator.SimpleHealthResponse health = dependencyHealthIndicator.checkHealth();

        Map<String, Object> readiness = new HashMap<>();
        readiness.put("ready", health.isHealthy());
        readiness.put("status", health.getStatus());
        readiness.put("timestamp", LocalDateTime.now());
        readiness.put("checks", health.getDetails());

        HttpStatus httpStatus = health.isHealthy() ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return new ResponseEntity<>(readiness, httpStatus);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        // Liveness probe - sadece servisin çalışıp çalışmadığını kontrol eder
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", LocalDateTime.now());
        liveness.put("uptime", getUptime());

        return ResponseEntity.ok(liveness);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getBasicMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM Metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());

        double memoryUsagePercent = ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100;
        memory.put("usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);

        metrics.put("memory", memory);
        metrics.put("processors", runtime.availableProcessors());
        metrics.put("timestamp", LocalDateTime.now());

        // Thread metrics
        Map<String, Object> threads = new HashMap<>();
        threads.put("active", Thread.activeCount());
        threads.put("peak", java.lang.management.ManagementFactory.getThreadMXBean().getPeakThreadCount());
        metrics.put("threads", threads);

        return ResponseEntity.ok(metrics);
    }

    private String getUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMs / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}