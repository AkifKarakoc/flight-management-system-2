package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.AircraftRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AircraftResponse;
import com.flightmanagement.referencemanagerservice.service.AircraftService;
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
@RequestMapping("/api/v1/aircrafts")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @GetMapping
    public ResponseEntity<Page<AircraftResponse>> getAllAircrafts(Pageable pageable) {
        return ResponseEntity.ok(aircraftService.getAllAircrafts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AircraftResponse> getAircraftById(@PathVariable Long id) {
        return ResponseEntity.ok(aircraftService.getAircraftById(id));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<AircraftResponse>> getAircraftsByAirline(@PathVariable Long airlineId) {
        return ResponseEntity.ok(aircraftService.getAircraftsByAirline(airlineId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponse> createAircraft(@Valid @RequestBody AircraftRequest request) {
        return new ResponseEntity<>(aircraftService.createAircraft(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponse> updateAircraft(@PathVariable Long id,
                                                           @Valid @RequestBody AircraftRequest request) {
        return ResponseEntity.ok(aircraftService.updateAircraft(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDeleteAircraft(@PathVariable Long id) {
        aircraftService.forceDeleteAircraft(id);
        return ResponseEntity.noContent().build();
    }
}