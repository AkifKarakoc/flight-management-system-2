package com.flightmanagement.referencemanagerservice.entity;

import com.flightmanagement.referencemanagerservice.entity.enums.AirlineType;
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
@Table(name = "airlines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"aircrafts", "crewMembers"}) // Circular reference'ı önlemek için
public class Airline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 3)
    private String iataCode;        // TK, PC, SU

    @Column(unique = true, length = 4)
    private String icaoCode;        // THY, PGT, AFL

    @Column(nullable = false)
    private String name;            // Turkish Airlines

    @Column
    private String country;         // Turkey

    @Enumerated(EnumType.STRING)
    private AirlineType type;       // FULL_SERVICE, LOW_COST, CARGO

    @Column
    private Boolean active = true;  // Aktif/Pasif durum

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "airline")
    private List<Aircraft> aircrafts = new ArrayList<>();

    @OneToMany(mappedBy = "airline")
    private List<CrewMember> crewMembers = new ArrayList<>();
}