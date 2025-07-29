package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {
    Optional<Airline> findByIataCode(String iataCode);
    Optional<Airline> findByIcaoCode(String icaoCode);
    boolean existsByIataCode(String iataCode);
    boolean existsByIcaoCode(String icaoCode);
}