package com.flightmanagement.flightservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class SecurityEnhancementConfig {

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    public static class SecurityHeadersFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            // Security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // Only for production
            if (isProductionEnvironment()) {
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }

            // Server header removal (security through obscurity)
            response.setHeader("Server", "Flight-Service");

            filterChain.doFilter(request, response);
        }

        private boolean isProductionEnvironment() {
            String profile = System.getProperty("spring.profiles.active");
            return profile != null && profile.contains("prod");
        }
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    public static class RateLimitFilter extends OncePerRequestFilter {

        private static final int MAX_REQUESTS_PER_MINUTE = 100;
        private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String clientIp = getClientIpAddress(request);
            long currentTime = System.currentTimeMillis();

            // Simple rate limiting (in production, use Redis or proper rate limiting solution)
            if (!isRateLimitExceeded(clientIp, currentTime)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            }
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }

        private boolean isRateLimitExceeded(String clientIp, long currentTime) {
            requestCounts.computeIfAbsent(clientIp, k -> new java.util.ArrayList<>());
            List<Long> requests = requestCounts.get(clientIp);

            // Remove requests older than 1 minute
            requests.removeIf(time -> currentTime - time > 60000);

            if (requests.size() >= MAX_REQUESTS_PER_MINUTE) {
                return true;
            }

            requests.add(currentTime);
            return false;
        }
    }
}