package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.RouteRequest;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.dto.response.RouteResponse;
import com.flightmanagement.referencemanagerservice.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * Admin için tüm route'lar - paginated
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RouteResponse>> getAllRoutesForAdmin(Pageable pageable) {
        return ResponseEntity.ok(routeService.getAllRoutes(pageable));
    }

    /**
     * Ana route listesi - paginated (kullanıcı için görünür route'lar)
     */
    @GetMapping
    public ResponseEntity<Page<RouteResponse>> getRoutes(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        return ResponseEntity.ok(routeService.getRoutesForUser(currentUserId, isAdmin, pageable));
    }

    /**
     * Kullanıcının kendi route'ları - paginated
     */
    @GetMapping("/my-routes")
    public ResponseEntity<Page<RouteResponse>> getMyRoutes(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(routeService.getUserRoutes(currentUserId, pageable));
    }

    /**
     * Havayolu paylaşılan route'ları - paginated
     */
    @GetMapping("/airline/{airlineId}/shared")
    public ResponseEntity<Page<RouteResponse>> getSharedRoutesForAirline(
            @PathVariable Long airlineId, Pageable pageable) {
        return ResponseEntity.ok(routeService.getSharedRoutesForAirline(airlineId, pageable));
    }

    /**
     * Route detayını getir
     */
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    /**
     * Route silme kontrolü
     */
    @GetMapping("/{id}/deletion-check")
    @PreAuthorize("hasRole('ADMIN') or @routeService.isRouteOwner(#id, authentication.name)")
    public ResponseEntity<DeletionCheckResult> checkRouteDeletion(@PathVariable Long id) {
        DeletionCheckResult result = routeService.checkRouteDeletion(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Yeni route oluştur
     */
    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteRequest request) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();

        // CreatedByUserId'yi request'e set et
        request.setCreatedByUserId(currentUserId);

        RouteResponse response = routeService.createRoute(request, currentUserId, isAdmin);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Route güncelle
     */
    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request) {

        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();

        RouteResponse response = routeService.updateRoute(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * Route sil
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();

        routeService.deleteRoute(id, currentUserId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Route code generate et (frontend için helper)
     */
    @GetMapping("/generate-code")
    public ResponseEntity<String> generateRouteCode(@RequestParam String prefix) {
        String generatedCode = routeService.generateRouteCode(prefix);
        return ResponseEntity.ok(generatedCode);
    }

    /**
     * Admin: Kullanıcı bazlı route istatistikleri
     */
    @GetMapping("/admin/stats/by-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRouteStatsByUser() {
        // TODO: İstatistik servisi eklendiğinde implement edilecek
        return ResponseEntity.ok("Route stats by user - TODO");
    }

    /**
     * Admin: Visibility bazlı route istatistikleri
     */
    @GetMapping("/admin/stats/by-visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRouteStatsByVisibility() {
        // TODO: İstatistik servisi eklendiğinde implement edilecek
        return ResponseEntity.ok("Route stats by visibility - TODO");
    }

    // Helper methods
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            // UserDetails'ten user ID'yi çıkar
            // Bu implementasyon JWT token yapısına göre değişebilir
            String username = authentication.getName();
            // TODO: Username'den user ID'yi resolve etme logic'i eklenecek
            // Şimdilik mock return
            return 1L;
        }
        throw new RuntimeException("User not authenticated");
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}