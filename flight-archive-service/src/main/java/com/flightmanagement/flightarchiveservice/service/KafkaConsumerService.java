package com.flightmanagement.flightarchiveservice.service;

import com.flightmanagement.flightarchiveservice.event.FlightEvent;
import com.flightmanagement.flightarchiveservice.event.ReferenceEvent;
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
public class KafkaConsumerService {

    private final FlightArchiveService flightArchiveService;

    @KafkaListener(
            topics = "flight.events",
            groupId = "flight-archive-service-group",
            containerFactory = "flightEventKafkaListenerContainerFactory"
    )
    public void handleFlightEvent(@Payload FlightEvent flightEvent,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        log.info("Received flight event from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        log.debug("Flight event ID: {}, Type: {}, Entity ID: {}",
                flightEvent.getEventId(), flightEvent.getEventType(), flightEvent.getEntityId());

        try {
            flightArchiveService.archiveFlightEvent(flightEvent);
            log.info("Flight event processed successfully: {} - {}",
                    flightEvent.getEventType(), flightEvent.getEventId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process flight event: {}", flightEvent.getEventId(), e);
            acknowledgment.acknowledge(); // Skip invalid messages to prevent infinite retry
        }
    }

    @KafkaListener(
            topics = "reference.events",
            groupId = "flight-archive-service-group-reference",
            containerFactory = "referenceEventKafkaListenerContainerFactory"
    )
    public void handleReferenceEvent(@Payload ReferenceEvent referenceEvent,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     Acknowledgment acknowledgment) {

        log.debug("Received reference event from topic: {}, Type: {}, Entity: {}",
                topic, referenceEvent.getEventType(), referenceEvent.getEntityType());

        try {
            // Reference event'leri şimdilik sadece log'layalım, ileride processing eklenebilir
            log.info("Reference event processed: {} - {} - ID: {}",
                    referenceEvent.getEntityType(), referenceEvent.getEventType(), referenceEvent.getEntityId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process reference event: {}", referenceEvent.getEventId(), e);
            acknowledgment.acknowledge();
        }
    }
}