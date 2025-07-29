package com.flightmanagement.flightarchiveservice.repository;

import com.flightmanagement.flightarchiveservice.entity.FlightArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightArchiveRepository extends JpaRepository<FlightArchive, Long> {

    // Flight number aramaları
    List<FlightArchive> findByFlightNumber(String flightNumber);
    List<FlightArchive> findByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    // Airline bazlı aramalar
    List<FlightArchive> findByAirlineId(Long airlineId);
    List<FlightArchive> findByAirlineIdAndFlightDateBetween(Long airlineId, LocalDate startDate, LocalDate endDate);

    // Airport bazlı aramalar
    List<FlightArchive> findByOriginAirportIdOrDestinationAirportId(Long originId, Long destinationId);
    List<FlightArchive> findByOriginAirportIdAndDestinationAirportId(Long originId, Long destinationId);

    // Tarih bazlı aramalar
    List<FlightArchive> findByFlightDate(LocalDate flightDate);
    List<FlightArchive> findByFlightDateBetween(LocalDate startDate, LocalDate endDate);
    Page<FlightArchive> findByFlightDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Status bazlı aramalar
    List<FlightArchive> findByStatus(String status);
    List<FlightArchive> findByStatusAndFlightDate(String status, LocalDate flightDate);

    // Event type bazlı aramalar
    List<FlightArchive> findByEventType(String eventType);
    List<FlightArchive> findByEventTypeAndEventTimeBetween(String eventType, LocalDateTime start, LocalDateTime end);

    // Gecikme aramaları
    @Query("SELECT f FROM FlightArchive f WHERE f.delayMinutes > :minutes")
    List<FlightArchive> findDelayedFlights(@Param("minutes") Integer minutes);

    @Query("SELECT f FROM FlightArchive f WHERE f.delayMinutes > :minutes AND f.flightDate = :date")
    List<FlightArchive> findDelayedFlightsByDate(@Param("minutes") Integer minutes, @Param("date") LocalDate date);

    // İstatistik sorguları
    @Query("SELECT COUNT(f) FROM FlightArchive f WHERE f.flightDate = :date")
    Long countByFlightDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM FlightArchive f WHERE f.airlineId = :airlineId AND f.flightDate = :date")
    Long countByAirlineAndDate(@Param("airlineId") Long airlineId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM FlightArchive f WHERE f.status = :status AND f.flightDate = :date")
    Long countByStatusAndDate(@Param("status") String status, @Param("date") LocalDate date);

    @Query("SELECT AVG(f.delayMinutes) FROM FlightArchive f WHERE f.delayMinutes > 0 AND f.flightDate = :date")
    Double getAverageDelayByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM FlightArchive f WHERE f.delayMinutes > 0 AND f.flightDate = :date")
    Long countDelayedFlightsByDate(@Param("date") LocalDate date);

    // Temizlik sorguları
    @Query("DELETE FROM FlightArchive f WHERE f.archivedAt < :cutoffDate")
    void deleteArchivedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // En son kayıtlar
    List<FlightArchive> findTop10ByOrderByEventTimeDesc();

    // Duplicate check
    boolean existsByEventId(String eventId);
}