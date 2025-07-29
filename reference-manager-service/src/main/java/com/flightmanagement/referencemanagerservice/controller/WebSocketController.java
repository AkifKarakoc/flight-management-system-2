package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    @MessageMapping("/reference/subscribe")
    @SendTo("/topic/reference/status")
    public WebSocketMessage handleSubscription(WebSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client {} subscribed to reference updates", sessionId);

        return WebSocketMessage.builder()
                .type("SUBSCRIPTION_CONFIRMED")
                .entity("REFERENCE")
                .data("Successfully subscribed to reference updates")
                .build();
    }

    @MessageMapping("/reference/ping")
    @SendTo("/topic/reference/pong")
    public WebSocketMessage handlePing(WebSocketMessage message) {
        return WebSocketMessage.builder()
                .type("PONG")
                .entity("SYSTEM")
                .data("Reference Manager Service is alive")
                .build();
    }
}