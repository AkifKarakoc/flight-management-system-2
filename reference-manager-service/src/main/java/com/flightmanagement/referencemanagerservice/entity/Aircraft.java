package com.flightmanagement.referencemanagerservice.entity;

import com.flightmanagement.referencemanagerservice.entity.enums.AircraftStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aircrafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"airline"}) // Circular reference'ı önlemek için
public class Aircraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String registrationNumber;  // TC-JJA, N747BA

    @Column(nullable = false)
    private String aircraftType;        // B777, A320, B737

    @Column
    private String manufacturer;        // Boeing, Airbus

    @Column
    private String model;              // 777-300ER, A320-200

    @Column
    private Integer seatCapacity;      // Yolcu kapasitesi

    @Enumerated(EnumType.STRING)
    private AircraftStatus status = AircraftStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}