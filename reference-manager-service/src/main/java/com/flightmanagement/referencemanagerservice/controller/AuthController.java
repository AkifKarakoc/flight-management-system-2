package com.flightmanagement.referencemanagerservice.controller;

import com.flightmanagement.referencemanagerservice.dto.request.LoginRequest;
import com.flightmanagement.referencemanagerservice.dto.response.JwtResponse;
import com.flightmanagement.referencemanagerservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
        // Test için sabit kullanıcı - gerçek projede UserDetailsService kullanın
        if ("admin".equals(loginRequest.getUsername()) && "admin123".equals(loginRequest.getPassword())) {
            String token = tokenProvider.generateToken("admin", "ROLE_ADMIN");
            return ResponseEntity.ok(new JwtResponse(token, "Bearer", 86400));
        }

        if ("user".equals(loginRequest.getUsername()) && "user123".equals(loginRequest.getPassword())) {
            String token = tokenProvider.generateToken("user", "ROLE_USER");
            return ResponseEntity.ok(new JwtResponse(token, "Bearer", 86400));
        }

        throw new RuntimeException("Invalid credentials");
    }
}