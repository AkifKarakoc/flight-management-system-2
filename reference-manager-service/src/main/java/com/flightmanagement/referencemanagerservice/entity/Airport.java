package com.flightmanagement.referencemanagerservice.entity;

import com.flightmanagement.referencemanagerservice.entity.enums.AirportType;
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
@Table(name = "airports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"originRoutes", "destinationRoutes"})
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 3)
    private String iataCode;        // IST, ESB, ADB

    @Column(unique = true, length = 4)
    private String icaoCode;        // LTFM, LTAC, LTBJ

    @Column(nullable = false)
    private String name;            // Istanbul Airport

    @Column(nullable = false)
    private String city;            // Istanbul

    @Column(nullable = false)
    private String country;         // Turkey

    @Enumerated(EnumType.STRING)
    private AirportType type;       // INTERNATIONAL, DOMESTIC, CARGO

    @Column
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}