package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByRouteIdOrderBySegmentOrder(Long routeId);

    List<RouteSegment> findByRouteId(Long routeId);

    List<RouteSegment> findByOriginAirportId(Long originAirportId);

    List<RouteSegment> findByDestinationAirportId(Long destinationAirportId);

    @Query("SELECT rs FROM RouteSegment rs WHERE rs.route.id = :routeId AND rs.segmentOrder = :order")
    RouteSegment findByRouteIdAndSegmentOrder(@Param("routeId") Long routeId, @Param("order") Integer order);

    @Query("SELECT COUNT(rs) FROM RouteSegment rs WHERE rs.originAirport.id = :airportId OR rs.destinationAirport.id = :airportId")
    long countByAirportId(@Param("airportId") Long airportId);

    // YENİ: Manual delete metodu - Constraint violation'u önler
    @Modifying
    @Transactional
    @Query("DELETE FROM RouteSegment rs WHERE rs.route.id = :routeId")
    void deleteByRouteId(@Param("routeId") Long routeId);
}