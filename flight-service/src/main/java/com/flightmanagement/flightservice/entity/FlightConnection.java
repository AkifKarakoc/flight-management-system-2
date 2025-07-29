package com.flightmanagement.flightservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_flight_id", nullable = false)
    private Long mainFlightId;

    @Column(name = "segment_flight_id", nullable = false)
    private Long segmentFlightId;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "connection_time_minutes")
    private Integer connectionTimeMinutes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // İlişkiler
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_flight_id", insertable = false, updatable = false)
    private Flight mainFlight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_flight_id", insertable = false, updatable = false)
    private Flight segmentFlight;

    // Helper methods
    public boolean isValid() {
        return mainFlightId != null &&
                segmentFlightId != null &&
                segmentOrder != null &&
                segmentOrder > 0 &&
                !mainFlightId.equals(segmentFlightId);
    }

    public boolean hasConnectionTime() {
        return connectionTimeMinutes != null && connectionTimeMinutes > 0;
    }
}