package com.flightmanagement.referencemanagerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {

    private String type; // CREATE, UPDATE, DELETE
    private String entity; // AIRLINE, AIRCRAFT, AIRPORT
    private Object data;
    private Long entityId;
    private String action;
    private LocalDateTime timestamp;
    private String userId;

    public static WebSocketMessage create(String entity, String type, Object data, Long entityId) {
        return WebSocketMessage.builder()
                .entity(entity)
                .type(type)
                .data(data)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}