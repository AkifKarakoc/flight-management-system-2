package com.flightmanagement.referencemanagerservice.entity;

import com.flightmanagement.referencemanagerservice.entity.enums.RouteType;
import com.flightmanagement.referencemanagerservice.entity.enums.RouteVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"segments"})
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String routeCode;

    @Column(nullable = false, length = 200)
    private String routeName;

    @Column
    private Integer distance;

    @Column
    private Integer estimatedFlightTime;

    @Column
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteType routeType;

    // Legacy alanlar - backward compatibility için nullable
    @Column(name = "origin_airport_id", nullable = true)
    private Long originAirportId;

    @Column(name = "destination_airport_id", nullable = true)
    private Long destinationAirportId;

    // Yeni ownership alanları
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteVisibility visibility = RouteVisibility.PRIVATE;

    @Column(name = "airline_id")
    private Long airlineId;

    // Multi-segment support
    // CRITICAL: Cascade ALL ve orphanRemoval=true olmalı
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RouteSegment> segments = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isSimpleRoute() {
        return segments != null && segments.size() == 1;
    }

    public boolean isMultiSegmentRoute() {
        return segments != null && segments.size() > 1;
    }

    // Helper method for safe segment management
    public void addSegment(RouteSegment segment) {
        segments.add(segment);
        segment.setRoute(this);
    }

    public void removeSegment(RouteSegment segment) {
        segments.remove(segment);
        segment.setRoute(null);
    }

    public void clearSegments() {
        if (segments != null) {
            // Clear the collection, orphanRemoval will handle deletion
            segments.clear();
        }
    }
}