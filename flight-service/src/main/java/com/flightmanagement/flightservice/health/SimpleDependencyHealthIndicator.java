package com.flightmanagement.flightservice.health;

import com.flightmanagement.flightservice.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleDependencyHealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReferenceDataService referenceDataService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SimpleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;
        LocalDateTime checkTime = LocalDateTime.now();

        // Database Health Check
        try {
            DatabaseHealth dbHealth = checkDatabaseHealth();
            details.put("database", dbHealth);
            if (!dbHealth.isHealthy()) {
                allHealthy = false;
            }
        } catch (Exception e) {
            details.put("database", createUnhealthyStatus("Database connection failed", e.getMessage()));
            allHealthy = false;
        }

        // Redis Health Check
        try {
            RedisHealth redisHealth = checkRedisHealth();
            details.put("redis", redisHealth);
            if (!redisHealth.isHealthy()) {
                allHealthy = false;
            }
        } catch (Exception e) {
            details.put("redis", createUnhealthyStatus("Redis connection failed", e.getMessage()));
            allHealthy = false;
        }

        // Reference Manager Health Check
        try {
            ServiceHealth refManagerHealth = checkReferenceManagerHealth();
            details.put("referenceManager", refManagerHealth);
            if (!refManagerHealth.isHealthy()) {
                allHealthy = false;
            }
        } catch (Exception e) {
            details.put("referenceManager", createUnhealthyStatus("Reference Manager unreachable", e.getMessage()));
            allHealthy = false;
        }

        // Kafka Health Check
        try {
            KafkaHealth kafkaHealth = checkKafkaHealth();
            details.put("kafka", kafkaHealth);
            if (!kafkaHealth.isHealthy()) {
                allHealthy = false;
            }
        } catch (Exception e) {
            details.put("kafka", createUnhealthyStatus("Kafka connection failed", e.getMessage()));
            allHealthy = false;
        }

        // Overall status
        details.put("checkTime", checkTime);
        details.put("serviceName", "flight-service");
        details.put("version", "2.0-ROUTE-BASED");

        return new SimpleHealthResponse(allHealthy ? "UP" : "DOWN", allHealthy, details);
    }

    private DatabaseHealth checkDatabaseHealth() {
        long startTime = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2); // 2 saniye timeout
            long responseTime = System.currentTimeMillis() - startTime;

            return DatabaseHealth.builder()
                    .healthy(isValid)
                    .responseTimeMs(responseTime)
                    .status(isValid ? "UP" : "DOWN")
                    .details("Connection validation: " + (isValid ? "SUCCESS" : "FAILED"))
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return DatabaseHealth.builder()
                    .healthy(false)
                    .responseTimeMs(responseTime)
                    .status("DOWN")
                    .details("Database error: " + e.getMessage())
                    .build();
        }
    }

    private RedisHealth checkRedisHealth() {
        long startTime = System.currentTimeMillis();
        try {
            // Ping Redis
            String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
            long responseTime = System.currentTimeMillis() - startTime;

            boolean isHealthy = "PONG".equals(pingResult);

            return RedisHealth.builder()
                    .healthy(isHealthy)
                    .responseTimeMs(responseTime)
                    .status(isHealthy ? "UP" : "DOWN")
                    .pingResult(pingResult)
                    .details("Redis ping: " + pingResult)
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return RedisHealth.builder()
                    .healthy(false)
                    .responseTimeMs(responseTime)
                    .status("DOWN")
                    .pingResult("ERROR")
                    .details("Redis error: " + e.getMessage())
                    .build();
        }
    }

    private ServiceHealth checkReferenceManagerHealth() {
        long startTime = System.currentTimeMillis();
        try {
            // Reference Manager service health check
            boolean isHealthy = referenceDataService.isReferenceServiceHealthy();
            long responseTime = System.currentTimeMillis() - startTime;

            return ServiceHealth.builder()
                    .healthy(isHealthy)
                    .responseTimeMs(responseTime)
                    .status(isHealthy ? "UP" : "DOWN")
                    .serviceName("reference-manager")
                    .details(isHealthy ? "Service responsive" : "Service unavailable")
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return ServiceHealth.builder()
                    .healthy(false)
                    .responseTimeMs(responseTime)
                    .status("DOWN")
                    .serviceName("reference-manager")
                    .details("Service error: " + e.getMessage())
                    .build();
        }
    }

    private KafkaHealth checkKafkaHealth() {
        long startTime = System.currentTimeMillis();
        try {
            // Kafka health check with timeout
            CompletableFuture<Boolean> kafkaCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    // Test Kafka connectivity by getting metadata
                    kafkaTemplate.getProducerFactory().createProducer().partitionsFor("test-topic");
                    return true;
                } catch (Exception e) {
                    log.debug("Kafka health check failed: {}", e.getMessage());
                    return false;
                }
            });

            Boolean isHealthy = kafkaCheck.get(3, TimeUnit.SECONDS); // 3 saniye timeout
            long responseTime = System.currentTimeMillis() - startTime;

            return KafkaHealth.builder()
                    .healthy(isHealthy)
                    .responseTimeMs(responseTime)
                    .status(isHealthy ? "UP" : "DOWN")
                    .details(isHealthy ? "Kafka cluster accessible" : "Kafka cluster unreachable")
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return KafkaHealth.builder()
                    .healthy(false)
                    .responseTimeMs(responseTime)
                    .status("DOWN")
                    .details("Kafka error: " + e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> createUnhealthyStatus(String message, String error) {
        Map<String, Object> status = new HashMap<>();
        status.put("healthy", false);
        status.put("status", "DOWN");
        status.put("message", message);
        status.put("error", error);
        status.put("responseTimeMs", -1);
        return status;
    }

    // Simple Health Response class
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SimpleHealthResponse {
        private String status;
        private boolean healthy;
        private Map<String, Object> details;
    }

    // Inner classes for structured health responses
    @lombok.Data
    @lombok.Builder
    public static class DatabaseHealth {
        private boolean healthy;
        private long responseTimeMs;
        private String status;
        private String details;
    }

    @lombok.Data
    @lombok.Builder
    public static class RedisHealth {
        private boolean healthy;
        private long responseTimeMs;
        private String status;
        private String pingResult;
        private String details;
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceHealth {
        private boolean healthy;
        private long responseTimeMs;
        private String status;
        private String serviceName;
        private String details;
    }

    @lombok.Data
    @lombok.Builder
    public static class KafkaHealth {
        private boolean healthy;
        private long responseTimeMs;
        private String status;
        private String details;
    }
}