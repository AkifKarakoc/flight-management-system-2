package com.flightmanagement.referencemanagerservice.repository;

import com.flightmanagement.referencemanagerservice.entity.Route;
import com.flightmanagement.referencemanagerservice.entity.enums.RouteVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    // YENİ: Paginated metodlar
    @Query("SELECT r FROM Route r")
    Page<Route> findAllForAdmin(Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.visibility = 'PUBLIC' OR r.createdByUserId = :userId")
    Page<Route> findVisibleRoutesForUser(@Param("userId") Long userId, Pageable pageable);

    Page<Route> findByCreatedByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.airlineId = :airlineId AND (r.visibility = 'SHARED' OR r.visibility = 'PUBLIC')")
    Page<Route> findSharedRoutesForAirline(@Param("airlineId") Long airlineId, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.createdByUserId = :userId AND r.airlineId = :airlineId")
    Page<Route> findByCreatedByUserIdAndAirlineId(@Param("userId") Long userId, @Param("airlineId") Long airlineId, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.createdByUserId = :userId AND r.visibility = :visibility")
    Page<Route> findByCreatedByUserIdAndVisibility(@Param("userId") Long userId, @Param("visibility") RouteVisibility visibility, Pageable pageable);

    Page<Route> findByVisibility(RouteVisibility visibility, Pageable pageable);

    Page<Route> findByCreatedByUserIdAndActive(Long userId, Boolean active, Pageable pageable);

    // User-specific methodlar
    List<Route> findByCreatedByUserId(Long userId);

    List<Route> findByCreatedByUserIdAndActive(Long userId, Boolean active);

    @Query("SELECT r FROM Route r WHERE r.createdByUserId = :userId AND r.airlineId = :airlineId")
    List<Route> findByCreatedByUserIdAndAirlineId(@Param("userId") Long userId, @Param("airlineId") Long airlineId);

    // Admin için tüm route'ları görme
    @Query("SELECT r FROM Route r WHERE r.visibility = 'PUBLIC' OR r.createdByUserId = :userId")
    List<Route> findVisibleRoutesForUser(@Param("userId") Long userId);

    // Admin tüm route'ları görebilir
    @Query("SELECT r FROM Route r")
    List<Route> findAllForAdmin();

    // Airline bazlı route'lar (shared visibility için)
    @Query("SELECT r FROM Route r WHERE r.airlineId = :airlineId AND (r.visibility = 'SHARED' OR r.visibility = 'PUBLIC')")
    List<Route> findSharedRoutesForAirline(@Param("airlineId") Long airlineId);

    // Route code unique kontrolü
    Optional<Route> findByRouteCode(String routeCode);

    boolean existsByRouteCode(String routeCode);

    // Visibility bazlı arama
    List<Route> findByVisibility(RouteVisibility visibility);

    // Kullanıcı ve visibility kombinasyonu
    @Query("SELECT r FROM Route r WHERE r.createdByUserId = :userId AND r.visibility = :visibility")
    List<Route> findByCreatedByUserIdAndVisibility(@Param("userId") Long userId, @Param("visibility") RouteVisibility visibility);
}