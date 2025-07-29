package com.flightmanagement.referencemanagerservice.entity;

import com.flightmanagement.referencemanagerservice.entity.enums.CrewStatus;
import com.flightmanagement.referencemanagerservice.entity.enums.CrewType;
import com.flightmanagement.referencemanagerservice.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "crew_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"airline"})
public class CrewMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String employeeNumber;      // Personel numarası

    @Column
    private String nationalId;          // TC Kimlik

    @Column
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    private String phoneNumber;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    private CrewType crewType;          // PILOT, COPILOT, FLIGHT_ATTENDANT, CABIN_CREW

    @Column
    private String licenseNumber;       // Lisans numarası

    @Column
    private LocalDate licenseExpiry;    // Lisans geçerlilik

    @Column
    private String aircraftQualifications; // B737, A320, B777 (comma separated)

    @Column
    private String languages;           // TR, EN, DE (comma separated)

    @Enumerated(EnumType.STRING)
    private CrewStatus status = CrewStatus.ACTIVE;

    // Hangi havayolu şirketinde çalışıyor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isQualifiedForAircraft(String aircraftType) {
        return aircraftQualifications != null &&
                aircraftQualifications.contains(aircraftType);
    }

    public boolean isLicenseValid() {
        return licenseExpiry != null &&
                licenseExpiry.isAfter(LocalDate.now());
    }
}