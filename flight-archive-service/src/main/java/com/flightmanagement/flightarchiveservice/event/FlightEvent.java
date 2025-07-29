package com.flightmanagement.flightarchiveservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime eventTime;
    private String entityType;
    private String entityId;
    private Object payload;
    private String version;
}