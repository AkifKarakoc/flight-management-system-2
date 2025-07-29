package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.AirportRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirportResponse;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    @GetMapping
    public ResponseEntity<Page<AirportResponse>> getAllAirports(Pageable pageable) {
        return ResponseEntity.ok(airportService.getAllAirports(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getAirportById(@PathVariable Long id) {
        return ResponseEntity.ok(airportService.getAirportById(id));
    }

    @GetMapping("/iata/{iataCode}")
    public ResponseEntity<AirportResponse> getAirportByIataCode(@PathVariable String iataCode) {
        return ResponseEntity.ok(airportService.getAirportByIataCode(iataCode));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request) {
        return new ResponseEntity<>(airportService.createAirport(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirportResponse> updateAirport(@PathVariable Long id,
                                                         @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airportService.updateAirport(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAirport(@PathVariable Long id) {
        airportService.deleteAirport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deletion-check")
    public ResponseEntity<DeletionCheckResult> checkAirportDeletion(@PathVariable Long id) {
        DeletionCheckResult result = airportService.checkAirportDeletion(id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDeleteAirport(@PathVariable Long id) {
        airportService.forceDeleteAirport(id);
        return ResponseEntity.noContent().build();
    }
}