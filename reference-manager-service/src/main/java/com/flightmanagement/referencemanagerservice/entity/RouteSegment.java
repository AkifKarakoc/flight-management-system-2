package com.flightmanagement.referencemanagerservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"route", "originAirport", "destinationAirport"})
public class RouteSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CRITICAL: Route ile ManyToOne ilişkisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;       // 1, 2, 3... (sıralı segment'ler)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @Column
    private Integer distance;          // km (bu segment için)

    @Column
    private Integer estimatedFlightTime; // dakika (bu segment için)

    @Column
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}