package com.flightmanagement.flightservice.repository;

import com.flightmanagement.flightservice.entity.FlightConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightConnectionRepository extends JpaRepository<FlightConnection, Long> {

    // Ana uçuşa ait tüm segment'leri getir
    List<FlightConnection> findByMainFlightIdOrderBySegmentOrder(Long mainFlightId);

    // Belirli segment'i getir
    Optional<FlightConnection> findByMainFlightIdAndSegmentOrder(Long mainFlightId, Integer segmentOrder);

    // Segment uçuşunun bağlı olduğu ana uçuşu getir
    Optional<FlightConnection> findBySegmentFlightId(Long segmentFlightId);

    // Ana uçuşun segment sayısını getir
    @Query("SELECT COUNT(fc) FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId")
    long countByMainFlightId(@Param("mainFlightId") Long mainFlightId);

    // Ana uçuşun en son segment'ini getir
    @Query("SELECT fc FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId ORDER BY fc.segmentOrder DESC LIMIT 1")
    Optional<FlightConnection> findLastSegmentByMainFlightId(@Param("mainFlightId") Long mainFlightId);

    // Ana uçuşun ilk segment'ini getir
    @Query("SELECT fc FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId ORDER BY fc.segmentOrder ASC LIMIT 1")
    Optional<FlightConnection> findFirstSegmentByMainFlightId(@Param("mainFlightId") Long mainFlightId);

    // Belirli segment sırasından sonraki segment'i getir
    @Query("SELECT fc FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId AND fc.segmentOrder > :segmentOrder ORDER BY fc.segmentOrder ASC LIMIT 1")
    Optional<FlightConnection> findNextSegment(@Param("mainFlightId") Long mainFlightId, @Param("segmentOrder") Integer segmentOrder);

    // Belirli segment sırasından önceki segment'i getir
    @Query("SELECT fc FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId AND fc.segmentOrder < :segmentOrder ORDER BY fc.segmentOrder DESC LIMIT 1")
    Optional<FlightConnection> findPreviousSegment(@Param("mainFlightId") Long mainFlightId, @Param("segmentOrder") Integer segmentOrder);

    // Ana uçuş var mı kontrolü
    boolean existsByMainFlightId(Long mainFlightId);

    // Segment uçuş var mı kontrolü
    boolean existsBySegmentFlightId(Long segmentFlightId);

    // Ana uçuşun belirli segment sırası var mı kontrolü
    boolean existsByMainFlightIdAndSegmentOrder(Long mainFlightId, Integer segmentOrder);

    // Ana uçuş ID'sine göre tüm connection'ları sil
    void deleteByMainFlightId(Long mainFlightId);

    // Segment uçuş ID'sine göre connection'ı sil
    void deleteBySegmentFlightId(Long segmentFlightId);

    // Connection time'ı olan segment'leri getir
    @Query("SELECT fc FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId AND fc.connectionTimeMinutes IS NOT NULL ORDER BY fc.segmentOrder")
    List<FlightConnection> findConnectionsWithTimeByMainFlightId(@Param("mainFlightId") Long mainFlightId);

    // Toplam connection time'ı hesapla
    @Query("SELECT COALESCE(SUM(fc.connectionTimeMinutes), 0) FROM FlightConnection fc WHERE fc.mainFlightId = :mainFlightId")
    Integer getTotalConnectionTimeByMainFlightId(@Param("mainFlightId") Long mainFlightId);

    // Segment'leri flight bilgileri ile birlikte getir
    @Query("SELECT fc FROM FlightConnection fc " +
            "LEFT JOIN FETCH fc.segmentFlight sf " +
            "WHERE fc.mainFlightId = :mainFlightId " +
            "ORDER BY fc.segmentOrder")
    List<FlightConnection> findByMainFlightIdWithSegmentFlights(@Param("mainFlightId") Long mainFlightId);
}