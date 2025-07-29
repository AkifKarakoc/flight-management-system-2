package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByIataCode(String iataCode);
    Optional<Airport> findByIcaoCode(String icaoCode);
    boolean existsByIataCode(String iataCode);
    boolean existsByIcaoCode(String icaoCode);
}