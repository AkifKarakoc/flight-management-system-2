package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendFlightUpdate(String type, Object data, Long entityId, String flightNumber) {
        WebSocketMessage message = WebSocketMessage.create(type, data, entityId, flightNumber);
        sendMessage("/topic/flights", message);
        sendMessage("/topic/flights/" + flightNumber, message);
        sendMessage("/topic/updates", message);
        log.debug("Sent flight {} message for flight: {}", type, flightNumber);
    }

    public void sendFlightStatusUpdate(String flightNumber, String oldStatus, String newStatus, Object data, Long entityId) {
        WebSocketMessage message = WebSocketMessage.create("STATUS_CHANGE", data, entityId, flightNumber);
        message.setStatus(newStatus);

        sendMessage("/topic/flights", message);
        sendMessage("/topic/flights/" + flightNumber, message);
        sendMessage("/topic/flights/status/" + newStatus, message);
        sendMessage("/topic/updates", message);

        log.info("Flight {} status changed from {} to {}", flightNumber, oldStatus, newStatus);
    }

    public void sendBulkFlightUpdate(String type, Object data) {
        WebSocketMessage message = WebSocketMessage.builder()
                .entity("FLIGHT")
                .type("BULK_" + type)
                .data(data)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendMessage("/topic/flights/bulk", message);
        sendMessage("/topic/updates", message);
        log.info("Sent bulk flight {} update", type);
    }

    private void sendMessage(String destination, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to {}: {}", destination, e.getMessage());
        }
    }
}