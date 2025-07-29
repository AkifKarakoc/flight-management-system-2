package com.flightmanagement.flightarchiveservice.dto;

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

    private String type; // ARCHIVED, REPORT_GENERATED, KPI_UPDATE
    private String entity; // FLIGHT_ARCHIVE, REPORT, KPI
    private Object data;
    private Long entityId;
    private String flightNumber;
    private String eventType;
    private LocalDateTime timestamp;
    private String userId;

    public static WebSocketMessage create(String type, String entity, Object data, Long entityId) {
        return WebSocketMessage.builder()
                .type(type)
                .entity(entity)
                .data(data)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}