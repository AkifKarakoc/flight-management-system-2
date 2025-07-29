package com.flightmanagement.flightservice.repository;

import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.entity.enums.FlightStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // Temel sorgular
    List<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    Optional<Flight> findByFlightNumberAndFlightDateAndSegmentNumber(
            String flightNumber, LocalDate flightDate, Integer segmentNumber);

    List<Flight> findByStatus(FlightStatus status);

    List<Flight> findByFlightDate(LocalDate flightDate);

    List<Flight> findByFlightDateBetween(LocalDate startDate, LocalDate endDate);

    // Route bazlı sorgular (YENİ SİSTEM)
    List<Flight> findByRouteId(Long routeId);

    List<Flight> findByRouteIdAndFlightDate(Long routeId, LocalDate flightDate);

    Page<Flight> findByRouteId(Long routeId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.routeId = :routeId AND f.active = true")
    long countActiveFlightsByRoute(@Param("routeId") Long routeId);

    @Query("SELECT f FROM Flight f WHERE f.routeId = :routeId AND f.flightDate BETWEEN :startDate AND :endDate ORDER BY f.scheduledDeparture")
    List<Flight> findFlightsByRouteAndDateRange(@Param("routeId") Long routeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // Airline bazlı sorgular
    List<Flight> findByAirlineId(Long airlineId);

    Page<Flight> findByAirlineId(Long airlineId, Pageable pageable);

    List<Flight> findByAirlineIdAndFlightDate(Long airlineId, LocalDate flightDate);

    // Aircraft bazlı sorgular
    List<Flight> findByAircraftId(Long aircraftId);

    List<Flight> findByAircraftIdAndFlightDate(Long aircraftId, LocalDate flightDate);

    // Status bazlı sorgular
    List<Flight> findByStatusAndFlightDate(FlightStatus status, LocalDate flightDate);

    @Query("SELECT f FROM Flight f WHERE f.status = :status AND f.flightDate BETWEEN :startDate AND :endDate")
    List<Flight> findByStatusAndDateRange(@Param("status") FlightStatus status,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // Aktarmalı uçuş sorgular
    List<Flight> findByParentFlightId(Long parentFlightId);

    @Query("SELECT f FROM Flight f WHERE f.parentFlightId = :parentFlightId ORDER BY f.segmentNumber")
    List<Flight> findByParentFlightIdOrderBySegmentNumber(@Param("parentFlightId") Long parentFlightId);

    @Query("SELECT f FROM Flight f WHERE f.isConnectingFlight = true AND f.parentFlightId IS NULL")
    List<Flight> findMainConnectingFlights();

    @Query("SELECT f FROM Flight f WHERE f.isConnectingFlight = true AND f.parentFlightId IS NULL AND f.flightDate = :date")
    List<Flight> findMainConnectingFlightsByDate(@Param("date") LocalDate date);

    @Query("SELECT f FROM Flight f WHERE f.isConnectingFlight = true AND f.parentFlightId IS NULL " +
            "AND (:airlineId IS NULL OR f.airlineId = :airlineId) " +
            "AND (:flightDate IS NULL OR f.flightDate = :flightDate)")
    Page<Flight> findConnectingFlightsWithFilters(@Param("airlineId") Long airlineId,
                                                  @Param("flightDate") LocalDate flightDate,
                                                  Pageable pageable);

    // İstatistik sorguları
    @Query("SELECT COUNT(f) FROM Flight f WHERE f.flightDate = :date AND f.active = true")
    long countFlightsByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.flightDate = :date AND f.status = :status AND f.active = true")
    long countFlightsByDateAndStatus(@Param("date") LocalDate date, @Param("status") FlightStatus status);

    @Query("SELECT f.type, COUNT(f) FROM Flight f WHERE f.flightDate = :date AND f.active = true GROUP BY f.type")
    List<Object[]> countFlightsGroupedByTypeAndDate(@Param("date") LocalDate date);

    @Query("SELECT f.type, COUNT(f) FROM Flight f WHERE f.active = true GROUP BY f.type")
    List<Object[]> countFlightsGroupedByType();

    // Airline bazlı istatistikler
    @Query("SELECT COUNT(f) FROM Flight f WHERE f.airlineId = :airlineId AND f.flightDate = :date AND f.active = true")
    long countFlightsByAirlineAndDate(@Param("airlineId") Long airlineId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.status = :status AND f.flightDate = :date AND f.active = true")
    long countFlightsByStatusAndDate(@Param("status") FlightStatus status, @Param("date") LocalDate date);

    // Route bazlı istatistikler
    @Query("SELECT COUNT(f) FROM Flight f WHERE f.routeId = :routeId AND f.flightDate = :date AND f.active = true")
    long countFlightsByRouteAndDate(@Param("routeId") Long routeId, @Param("date") LocalDate date);

    @Query("SELECT f.routeId, COUNT(f) FROM Flight f WHERE f.flightDate = :date AND f.active = true GROUP BY f.routeId")
    List<Object[]> countFlightsGroupedByRouteAndDate(@Param("date") LocalDate date);

    // Performans sorguları
    @Query("SELECT f FROM Flight f WHERE f.scheduledDeparture BETWEEN :start AND :end AND f.active = true ORDER BY f.scheduledDeparture")
    List<Flight> findFlightsInTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT f FROM Flight f WHERE f.routeId = :routeId AND f.scheduledDeparture BETWEEN :start AND :end AND f.active = true ORDER BY f.scheduledDeparture")
    List<Flight> findFlightsByRouteInTimeRange(@Param("routeId") Long routeId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    // Gecikme analizi
    @Query("SELECT f FROM Flight f WHERE f.delayMinutes >= :minDelayMinutes AND f.flightDate = :date AND f.active = true")
    List<Flight> findDelayedFlightsByDateAndMinutes(@Param("date") LocalDate date, @Param("minDelayMinutes") Integer minDelayMinutes);

    @Query("SELECT AVG(f.delayMinutes) FROM Flight f WHERE f.flightDate = :date AND f.delayMinutes > 0 AND f.active = true")
    Double getAverageDelayByDate(@Param("date") LocalDate date);

    // Validation sorguları
    boolean existsByFlightNumberAndFlightDate(String flightNumber, LocalDate flightDate);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.flightNumber = :flightNumber AND f.flightDate = :date AND f.isConnectingFlight = true AND f.parentFlightId IS NULL")
    boolean existsMainFlightByFlightNumberAndDate(@Param("flightNumber") String flightNumber, @Param("date") LocalDate date);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.aircraftId = :aircraftId AND f.flightDate = :date AND f.active = true AND " +
            "((f.scheduledDeparture <= :departure AND f.scheduledArrival > :departure) OR " +
            "(f.scheduledDeparture < :arrival AND f.scheduledArrival >= :arrival) OR " +
            "(f.scheduledDeparture >= :departure AND f.scheduledArrival <= :arrival))")
    boolean hasAircraftConflict(@Param("aircraftId") Long aircraftId,
                                @Param("date") LocalDate date,
                                @Param("departure") LocalDateTime departure,
                                @Param("arrival") LocalDateTime arrival);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f WHERE f.routeId = :routeId")
    boolean existsByRouteId(@Param("routeId") Long routeId);

    // Dashboard sorguları için complex queries
    @Query("SELECT " +
            "COUNT(CASE WHEN f.status = 'SCHEDULED' THEN 1 END) as scheduled, " +
            "COUNT(CASE WHEN f.status = 'DEPARTED' THEN 1 END) as departed, " +
            "COUNT(CASE WHEN f.status = 'ARRIVED' THEN 1 END) as arrived, " +
            "COUNT(CASE WHEN f.status = 'CANCELLED' THEN 1 END) as cancelled, " +
            "COUNT(CASE WHEN f.status = 'DELAYED' THEN 1 END) as delayed " +
            "FROM Flight f WHERE f.flightDate = :date AND f.active = true")
    Object[] getFlightStatsByDate(@Param("date") LocalDate date);

    // Chart data için daily statistics
    @Query("SELECT f.flightDate, " +
            "COUNT(CASE WHEN f.status = 'SCHEDULED' THEN 1 END) as scheduled, " +
            "COUNT(CASE WHEN f.status = 'DEPARTED' THEN 1 END) as departed, " +
            "COUNT(CASE WHEN f.status = 'ARRIVED' THEN 1 END) as arrived, " +
            "COUNT(CASE WHEN f.status = 'CANCELLED' THEN 1 END) as cancelled, " +
            "COUNT(CASE WHEN f.status = 'DELAYED' THEN 1 END) as delayed " +
            "FROM Flight f WHERE f.flightDate BETWEEN :startDate AND :endDate AND f.active = true " +
            "GROUP BY f.flightDate ORDER BY f.flightDate")
    List<Object[]> getFlightChartData(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Filtered queries with multiple parameters
    @Query("SELECT f FROM Flight f WHERE " +
            "(:flightNumber IS NULL OR UPPER(f.flightNumber) LIKE UPPER(CONCAT('%', :flightNumber, '%'))) AND " +
            "(:airlineId IS NULL OR f.airlineId = :airlineId) AND " +
            "(:flightDate IS NULL OR f.flightDate = :flightDate) AND " +
            "(:routeId IS NULL OR f.routeId = :routeId) AND " +
            "f.active = true " +
            "ORDER BY f.scheduledDeparture DESC")
    Page<Flight> findFlightsWithFilters(@Param("flightNumber") String flightNumber,
                                        @Param("airlineId") Long airlineId,
                                        @Param("flightDate") LocalDate flightDate,
                                        @Param("routeId") Long routeId,
                                        Pageable pageable);
}