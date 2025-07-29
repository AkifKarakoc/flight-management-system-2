package com.flightmanagement.referencemanagerservice.dto.response;

import com.flightmanagement.referencemanagerservice.entity.enums.CrewStatus;
import com.flightmanagement.referencemanagerservice.entity.enums.CrewType;
import com.flightmanagement.referencemanagerservice.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CrewMemberResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String employeeNumber;
    private String nationalId;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private String phoneNumber;
    private String email;
    private CrewType crewType;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private Boolean licenseValid;
    private String aircraftQualifications;
    private String languages;
    private CrewStatus status;
    private AirportResponse baseAirport;
    private AirlineResponse airline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}