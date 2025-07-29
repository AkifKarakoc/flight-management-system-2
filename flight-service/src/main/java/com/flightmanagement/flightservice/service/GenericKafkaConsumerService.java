package com.flightmanagement.flightservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericKafkaConsumerService {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reference.events", groupId = "flight-service-group")
    public void handleReferenceEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received message from topic: {}, partition: {}, offset: {}", topic, partition, offset);

        try {
            // String payload'ı JsonNode'a parse et
            JsonNode eventNode = objectMapper.readTree(payload);

            // Event bilgilerini extract et
            String eventType = extractStringValue(eventNode, "eventType");
            String entityType = extractStringValue(eventNode, "entityType");
            String entityId = extractStringValue(eventNode, "entityId");
            JsonNode payloadNode = eventNode.get("payload");

            if (eventType == null || entityType == null || entityId == null) {
                log.warn("Missing required fields in event. EventType: {}, EntityType: {}, EntityId: {}",
                        eventType, entityType, entityId);
                acknowledgment.acknowledge();
                return;
            }

            log.info("Processing reference event: {} for entity: {} with ID: {}",
                    eventType, entityType, entityId);

            Long id = Long.parseLong(entityId);

            switch (entityType.toUpperCase()) {
                case "AIRLINE":
                    handleAirlineEvent(eventType, id, payloadNode);
                    break;
                case "AIRPORT":
                    handleAirportEvent(eventType, id, payloadNode);
                    break;
                case "AIRCRAFT":
                    handleAircraftEvent(eventType, id, payloadNode);
                    break;
                case "ROUTE":
                    handleRouteEvent(eventType, id, payloadNode);
                    break;
                default:
                    log.debug("Ignoring event for entity type: {}", entityType);
            }

            // Message'ı acknowledge et
            acknowledgment.acknowledge();
            log.debug("Successfully processed and acknowledged message at offset: {}", offset);

        } catch (NumberFormatException e) {
            log.error("Invalid entity ID format at offset {}: {}", offset, e.getMessage());
            acknowledgment.acknowledge(); // Skip this message
        } catch (Exception e) {
            log.error("Error processing reference event at offset {}: {}", offset, e.getMessage(), e);
            // Don't acknowledge - message will be retried
        }
    }

    private void handleAirlineEvent(String eventType, Long entityId, JsonNode payloadNode) {
        switch (eventType.toUpperCase()) {
            case "AIRLINE_CREATED":
            case "AIRLINE_UPDATED":
                if (payloadNode != null) {
                    updateAirlineCache(entityId, payloadNode);
                } else {
                    log.warn("No payload found for airline event: {} with ID: {}", eventType, entityId);
                }
                break;
            case "AIRLINE_DELETED":
            case "AIRLINE_FORCE_DELETED":
                cacheService.evictAirline(entityId);
                log.info("Evicted airline from cache: {}", entityId);
                break;
            default:
                log.debug("Ignoring airline event type: {}", eventType);
        }
    }

    private void handleAirportEvent(String eventType, Long entityId, JsonNode payloadNode) {
        switch (eventType.toUpperCase()) {
            case "AIRPORT_CREATED":
            case "AIRPORT_UPDATED":
                if (payloadNode != null) {
                    updateAirportCache(entityId, payloadNode);
                } else {
                    log.warn("No payload found for airport event: {} with ID: {}", eventType, entityId);
                }
                break;
            case "AIRPORT_DELETED":
            case "AIRPORT_FORCE_DELETED":
                cacheService.evictAirport(entityId);
                log.info("Evicted airport from cache: {}", entityId);
                break;
            default:
                log.debug("Ignoring airport event type: {}", eventType);
        }
    }

    private void handleAircraftEvent(String eventType, Long entityId, JsonNode payloadNode) {
        switch (eventType.toUpperCase()) {
            case "AIRCRAFT_CREATED":
            case "AIRCRAFT_UPDATED":
                if (payloadNode != null) {
                    updateAircraftCache(entityId, payloadNode);
                } else {
                    log.warn("No payload found for aircraft event: {} with ID: {}", eventType, entityId);
                }
                break;
            case "AIRCRAFT_DELETED":
            case "AIRCRAFT_FORCE_DELETED":
                cacheService.evictAircraft(entityId);
                log.info("Evicted aircraft from cache: {}", entityId);
                break;
            default:
                log.debug("Ignoring aircraft event type: {}", eventType);
        }
    }

    private void handleRouteEvent(String eventType, Long entityId, JsonNode payloadNode) {
        switch (eventType.toUpperCase()) {
            case "ROUTE_CREATED":
            case "ROUTE_UPDATED":
                log.info("Route {} event received for ID: {}", eventType, entityId);
                // Route cache invalidation burada yapılabilir
                break;
            case "ROUTE_DELETED":
            case "ROUTE_FORCE_DELETED":
                log.info("Route deleted event received for ID: {}", entityId);
                // Route cache invalidation burada yapılabilir
                break;
            default:
                log.debug("Ignoring route event type: {}", eventType);
        }
    }

    private void updateAirlineCache(Long entityId, JsonNode payloadNode) {
        try {
            AirlineCache airline = objectMapper.treeToValue(payloadNode, AirlineCache.class);
            airline.setId(entityId); // ID'yi garanti et
            cacheService.cacheAirline(entityId, airline);
            log.debug("Updated airline cache for ID: {} - {}", entityId, airline.getName());
        } catch (Exception e) {
            log.error("Failed to update airline cache for ID: {}", entityId, e);
        }
    }

    private void updateAirportCache(Long entityId, JsonNode payloadNode) {
        try {
            AirportCache airport = objectMapper.treeToValue(payloadNode, AirportCache.class);
            airport.setId(entityId); // ID'yi garanti et
            cacheService.cacheAirport(entityId, airport);
            log.debug("Updated airport cache for ID: {} - {}", entityId, airport.getName());
        } catch (Exception e) {
            log.error("Failed to update airport cache for ID: {}", entityId, e);
        }
    }

    private void updateAircraftCache(Long entityId, JsonNode payloadNode) {
        try {
            AircraftCache aircraft = objectMapper.treeToValue(payloadNode, AircraftCache.class);
            aircraft.setId(entityId); // ID'yi garanti et
            cacheService.cacheAircraft(entityId, aircraft);
            log.debug("Updated aircraft cache for ID: {} - {}", entityId, aircraft.getRegistrationNumber());
        } catch (Exception e) {
            log.error("Failed to update aircraft cache for ID: {}", entityId, e);
        }
    }

    private String extractStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }
}