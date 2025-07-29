package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.CrewMemberRequest;
import com.flightmanagement.referencemanagerservice.dto.response.CrewMemberResponse;
import com.flightmanagement.referencemanagerservice.service.CrewMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crew-members")
@RequiredArgsConstructor
public class CrewMemberController {

    private final CrewMemberService crewMemberService;

    @GetMapping
    public ResponseEntity<List<CrewMemberResponse>> getAllCrewMembers() {
        return ResponseEntity.ok(crewMemberService.getAllCrewMembers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrewMemberResponse> getCrewMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(crewMemberService.getCrewMemberById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrewMemberResponse> createCrewMember(@Valid @RequestBody CrewMemberRequest request) {
        return new ResponseEntity<>(crewMemberService.createCrewMember(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrewMemberResponse> updateCrewMember(@PathVariable Long id,
                                                               @Valid @RequestBody CrewMemberRequest request) {
        return ResponseEntity.ok(crewMemberService.updateCrewMember(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCrewMember(@PathVariable Long id) {
        crewMemberService.deleteCrewMember(id);
        return ResponseEntity.noContent().build();
    }
}