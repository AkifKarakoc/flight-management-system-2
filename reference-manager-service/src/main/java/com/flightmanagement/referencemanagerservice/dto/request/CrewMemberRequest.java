package com.flightmanagement.referencemanagerservice.dto.request;

import com.flightmanagement.referencemanagerservice.entity.enums.CrewStatus;
import com.flightmanagement.referencemanagerservice.entity.enums.CrewType;
import com.flightmanagement.referencemanagerservice.entity.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrewMemberRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Employee number is required")
    private String employeeNumber;

    private String nationalId;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String phoneNumber;

    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Crew type is required")
    private CrewType crewType;

    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String aircraftQualifications;
    private String languages;

    private CrewStatus status = CrewStatus.ACTIVE;

    private Long baseAirportId;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;
}