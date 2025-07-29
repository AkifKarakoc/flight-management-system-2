package com.flightmanagement.referencemanagerservice.service;

import com.flightmanagement.referencemanagerservice.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendAirlineUpdate(String type, Object data, Long entityId) {
        WebSocketMessage message = WebSocketMessage.create("AIRLINE", type, data, entityId);
        sendMessage("/topic/reference/airlines", message);
        sendMessage("/topic/reference/updates", message);
        log.debug("Sent airline {} message for ID: {}", type, entityId);
    }

    public void sendAircraftUpdate(String type, Object data, Long entityId) {
        WebSocketMessage message = WebSocketMessage.create("AIRCRAFT", type, data, entityId);
        sendMessage("/topic/reference/aircrafts", message);
        sendMessage("/topic/reference/updates", message);
        log.debug("Sent aircraft {} message for ID: {}", type, entityId);
    }

    public void sendAirportUpdate(String type, Object data, Long entityId) {
        WebSocketMessage message = WebSocketMessage.create("AIRPORT", type, data, entityId);
        sendMessage("/topic/reference/airports", message);
        sendMessage("/topic/reference/updates", message);
        log.debug("Sent airport {} message for ID: {}", type, entityId);
    }

    private void sendMessage(String destination, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message to {}: {}", destination, e.getMessage());
        }
    }
}