package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);
    List<Aircraft> findByAirlineId(Long airlineId);
    List<Aircraft> findByAircraftType(String aircraftType);
    boolean existsByRegistrationNumber(String registrationNumber);
    long countByAirlineId(Long airlineId);
}