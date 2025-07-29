package com.flightmanagement.referencemanagerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
}