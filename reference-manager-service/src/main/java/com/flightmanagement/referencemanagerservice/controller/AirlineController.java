package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.AirlineRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirlineResponse;
import com.flightmanagement.referencemanagerservice.dto.response.DeletionCheckResult;
import com.flightmanagement.referencemanagerservice.service.AirlineService;
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
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @GetMapping
    public ResponseEntity<Page<AirlineResponse>> getAllAirlines(Pageable pageable) {
        return ResponseEntity.ok(airlineService.getAllAirlines(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getAirlineById(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    @GetMapping("/iata/{iataCode}")
    public ResponseEntity<AirlineResponse> getAirlineByIataCode(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirlineByIataCode(iataCode));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponse> createAirline(@Valid @RequestBody AirlineRequest request) {
        return new ResponseEntity<>(airlineService.createAirline(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponse> updateAirline(@PathVariable Long id,
                                                         @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.updateAirline(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAirline(@PathVariable Long id) {
        airlineService.deleteAirline(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deletion-check")
    public ResponseEntity<DeletionCheckResult> checkAirlineDeletion(@PathVariable Long id) {
        DeletionCheckResult result = airlineService.checkAirlineDeletion(id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDeleteAirline(@PathVariable Long id) {
        airlineService.forceDeleteAirline(id);
        return ResponseEntity.noContent().build();
    }
}