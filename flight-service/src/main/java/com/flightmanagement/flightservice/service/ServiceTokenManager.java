package com.flightmanagement.flightservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenManager {

    private final RestTemplate restTemplate;

    @Value("${reference-manager.base-url}")
    private String referenceManagerBaseUrl;

    private String cachedToken;
    private LocalDateTime tokenExpiry;

    public String getServiceToken() {
        // Token var ve geçerli mi kontrol et
        if (cachedToken != null && tokenExpiry != null &&
                LocalDateTime.now().isBefore(tokenExpiry.minusMinutes(5))) {
            return cachedToken;
        }

        // Yeni token al
        return refreshToken();
    }

    private String refreshToken() {
        try {
            String loginUrl = referenceManagerBaseUrl + "/api/v1/auth/login";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", "admin");
            loginData.put("password", "admin123");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(loginData, headers);

            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    loginUrl, HttpMethod.POST, request, LoginResponse.class);

            if (response.getBody() != null && response.getBody().getToken() != null) {
                cachedToken = response.getBody().getToken(); // getAccessToken() -> getToken()
                tokenExpiry = LocalDateTime.now().plusSeconds(response.getBody().getExpiresIn());
                log.info("Service token refreshed successfully");
                return cachedToken;
            }

            log.error("Login response body or token is null");
            return null;

        } catch (Exception e) {
            log.error("Failed to refresh service token: {}", e.getMessage(), e);
            cachedToken = null;
            tokenExpiry = null;
            return null;
        }
    }

    public static class LoginResponse {
        private String token;        // accessToken -> token değişti
        private String tokenType;
        private long expiresIn;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getAccessToken() { return token; } // backward compatibility
        public void setAccessToken(String token) { this.token = token; } // backward compatibility
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    }
}