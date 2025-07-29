package com.flightmanagement.flightservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.event.ReferenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("disabled")
public class KafkaConsumerService {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reference.events", groupId = "flight-service-group")
    public void handleReferenceEvent(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received message from topic: {}, partition: {}, offset: {}", topic, partition, offset);

        try {
            // Payload'ı ReferenceEvent'e dönüştür
            ReferenceEvent event;

            if (payload instanceof ReferenceEvent) {
                event = (ReferenceEvent) payload;
            } else if (payload instanceof Map) {
                // Map olarak gelmişse ObjectMapper ile dönüştür
                event = objectMapper.convertValue(payload, ReferenceEvent.class);
            } else {
                log.warn("Unknown payload type: {}, skipping message", payload.getClass());
                acknowledgment.acknowledge();
                return;
            }

            log.info("Processing reference event: {} for entity: {}", event.getEventType(), event.getEntityType());

            switch (event.getEntityType()) {
                case "AIRLINE":
                    handleAirlineEvent(event);
                    break;
                case "AIRPORT":
                    handleAirportEvent(event);
                    break;
                case "AIRCRAFT":
                    handleAircraftEvent(event);
                    break;
                default:
                    log.debug("Ignoring event for entity type: {}", event.getEntityType());
            }

            // Message'ı acknowledge et
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing reference event at offset {}: {}", offset, e.getMessage(), e);
            // Acknowledge etme, retry edilsin
        }
    }

    private void handleAirlineEvent(ReferenceEvent event) {
        Long entityId = Long.parseLong(event.getEntityId());

        switch (event.getEventType()) {
            case "AIRLINE_CREATED":
            case "AIRLINE_UPDATED":
                updateAirlineCache(entityId, event.getPayload());
                break;
            case "AIRLINE_DELETED":
            case "AIRLINE_FORCE_DELETED":
                cacheService.evictAirline(entityId);
                break;
            default:
                log.debug("Ignoring airline event type: {}", event.getEventType());
        }
    }

    private void handleAirportEvent(ReferenceEvent event) {
        Long entityId = Long.parseLong(event.getEntityId());

        switch (event.getEventType()) {
            case "AIRPORT_CREATED":
            case "AIRPORT_UPDATED":
                updateAirportCache(entityId, event.getPayload());
                break;
            case "AIRPORT_DELETED":
            case "AIRPORT_FORCE_DELETED":
                cacheService.evictAirport(entityId);
                break;
            default:
                log.debug("Ignoring airport event type: {}", event.getEventType());
        }
    }

    private void handleAircraftEvent(ReferenceEvent event) {
        Long entityId = Long.parseLong(event.getEntityId());

        switch (event.getEventType()) {
            case "AIRCRAFT_CREATED":
            case "AIRCRAFT_UPDATED":
                updateAircraftCache(entityId, event.getPayload());
                break;
            case "AIRCRAFT_DELETED":
            case "AIRCRAFT_FORCE_DELETED":
                cacheService.evictAircraft(entityId);
                break;
            default:
                log.debug("Ignoring aircraft event type: {}", event.getEventType());
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAirlineCache(Long entityId, Object payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload;
            AirlineCache airline = objectMapper.convertValue(data, AirlineCache.class);
            cacheService.cacheAirline(entityId, airline);
            log.debug("Updated airline cache for ID: {}", entityId);
        } catch (Exception e) {
            log.error("Failed to update airline cache for ID: {}", entityId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAirportCache(Long entityId, Object payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload;
            AirportCache airport = objectMapper.convertValue(data, AirportCache.class);
            cacheService.cacheAirport(entityId, airport);
            log.debug("Updated airport cache for ID: {}", entityId);
        } catch (Exception e) {
            log.error("Failed to update airport cache for ID: {}", entityId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAircraftCache(Long entityId, Object payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload;
            AircraftCache aircraft = objectMapper.convertValue(data, AircraftCache.class);
            cacheService.cacheAircraft(entityId, aircraft);
            log.debug("Updated aircraft cache for ID: {}", entityId);
        } catch (Exception e) {
            log.error("Failed to update aircraft cache for ID: {}", entityId, e);
        }
    }
}