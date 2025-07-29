package com.flightmanagement.flightservice.dto;

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

    private String type; // CREATE, UPDATE, DELETE, STATUS_CHANGE
    private String entity; // FLIGHT
    private Object data;
    private Long entityId;
    private String flightNumber;
    private String status;
    private LocalDateTime timestamp;
    private String userId;

    public static WebSocketMessage create(String type, Object data, Long entityId, String flightNumber) {
        return WebSocketMessage.builder()
                .entity("FLIGHT")
                .type(type)
                .data(data)
                .entityId(entityId)
                .flightNumber(flightNumber)
                .timestamp(LocalDateTime.now())
                .build();
    }
}