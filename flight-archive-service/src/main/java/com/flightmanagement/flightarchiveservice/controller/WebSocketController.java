package com.flightmanagement.flightarchiveservice.controller;

import com.flightmanagement.flightarchiveservice.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    @MessageMapping("/archive/subscribe")
    @SendTo("/topic/archive/status")
    public WebSocketMessage handleArchiveSubscription(WebSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client {} subscribed to archive updates", sessionId);

        return WebSocketMessage.builder()
                .type("SUBSCRIPTION_CONFIRMED")
                .entity("ARCHIVE")
                .data("Successfully subscribed to archive updates")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @MessageMapping("/archive/kpi/subscribe")
    @SendTo("/topic/archive/kpi")
    public WebSocketMessage handleKpiSubscription(WebSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client {} subscribed to KPI updates", sessionId);

        return WebSocketMessage.builder()
                .type("KPI_SUBSCRIPTION_CONFIRMED")
                .entity("KPI")
                .data("Successfully subscribed to KPI updates")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    @MessageMapping("/archive/ping")
    @SendTo("/topic/archive/pong")
    public WebSocketMessage handlePing(WebSocketMessage message) {
        return WebSocketMessage.builder()
                .type("PONG")
                .entity("SYSTEM")
                .data("Archive Service is alive")
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }
}