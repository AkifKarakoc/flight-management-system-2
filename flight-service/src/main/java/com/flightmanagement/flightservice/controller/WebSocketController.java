package com.flightmanagement.flightservice.controller;

import com.flightmanagement.flightservice.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    @MessageMapping("/flights/subscribe")
    @SendTo("/topic/flights/status")
    public WebSocketMessage handleFlightSubscription(WebSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client {} subscribed to flight updates", sessionId);

        return WebSocketMessage.builder()
                .type("SUBSCRIPTION_CONFIRMED")
                .entity("FLIGHT")
                .data("Successfully subscribed to flight updates")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @MessageMapping("/flights/subscribe/{flightNumber}")
    @SendTo("/topic/flights/{flightNumber}")
    public WebSocketMessage handleSpecificFlightSubscription(@DestinationVariable String flightNumber,
                                                             WebSocketMessage message,
                                                             SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client {} subscribed to flight {} updates", sessionId, flightNumber);

        return WebSocketMessage.builder()
                .type("SPECIFIC_SUBSCRIPTION_CONFIRMED")
                .entity("FLIGHT")
                .flightNumber(flightNumber)
                .data("Successfully subscribed to " + flightNumber + " updates")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @MessageMapping("/flights/ping")
    @SendTo("/topic/flights/pong")
    public WebSocketMessage handlePing(WebSocketMessage message) {
        return WebSocketMessage.builder()
                .type("PONG")
                .entity("SYSTEM")
                .data("Flight Service is alive")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }
}