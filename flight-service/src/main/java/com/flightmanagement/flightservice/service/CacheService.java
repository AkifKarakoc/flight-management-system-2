package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.ttl.airline}")
    private long airlineTtl;

    @Value("${cache.ttl.airport}")
    private long airportTtl;

    @Value("${cache.ttl.aircraft}")
    private long aircraftTtl;

    // Cache key prefixes
    private static final String AIRLINE_PREFIX = "airline:";
    private static final String AIRPORT_PREFIX = "airport:";
    private static final String AIRCRAFT_PREFIX = "aircraft:";

    // Airline Cache Operations
    public void cacheAirline(Long id, AirlineCache airline) {
        String key = AIRLINE_PREFIX + id;
        redisTemplate.opsForValue().set(key, airline, airlineTtl, TimeUnit.SECONDS);
        log.debug("Cached airline: {} with key: {}", airline.getName(), key);
    }

    public AirlineCache getAirlineFromCache(Long id) {
        String key = AIRLINE_PREFIX + id;
        AirlineCache airline = (AirlineCache) redisTemplate.opsForValue().get(key);
        if (airline != null) {
            log.debug("Cache hit for airline: {}", key);
        } else {
            log.debug("Cache miss for airline: {}", key);
        }
        return airline;
    }

    public void evictAirline(Long id) {
        String key = AIRLINE_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Evicted airline from cache: {}", key);
    }

    // Airport Cache Operations
    public void cacheAirport(Long id, AirportCache airport) {
        String key = AIRPORT_PREFIX + id;
        redisTemplate.opsForValue().set(key, airport, airportTtl, TimeUnit.SECONDS);
        log.debug("Cached airport: {} with key: {}", airport.getName(), key);
    }

    public AirportCache getAirportFromCache(Long id) {
        String key = AIRPORT_PREFIX + id;
        AirportCache airport = (AirportCache) redisTemplate.opsForValue().get(key);
        if (airport != null) {
            log.debug("Cache hit for airport: {}", key);
        } else {
            log.debug("Cache miss for airport: {}", key);
        }
        return airport;
    }

    public void evictAirport(Long id) {
        String key = AIRPORT_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Evicted airport from cache: {}", key);
    }

    // Aircraft Cache Operations
    public void cacheAircraft(Long id, AircraftCache aircraft) {
        String key = AIRCRAFT_PREFIX + id;

        log.debug("Caching aircraft: id={}, regNumber={}, airlineId={}",
                aircraft.getId(), aircraft.getRegistrationNumber(), aircraft.getAirlineId());
        redisTemplate.opsForValue().set(key, aircraft, aircraftTtl, TimeUnit.SECONDS);
        log.debug("Cached aircraft: {} with key: {}", aircraft.getRegistrationNumber(), key);
    }

    public AircraftCache getAircraftFromCache(Long id) {
        String key = AIRCRAFT_PREFIX + id;
        AircraftCache aircraft = (AircraftCache) redisTemplate.opsForValue().get(key);
        if (aircraft != null) {
            log.debug("Cache hit for aircraft: {}", key);
        } else {
            log.debug("Cache miss for aircraft: {}", key);
        }
        return aircraft;
    }

    public void evictAircraft(Long id) {
        String key = AIRCRAFT_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Evicted aircraft from cache: {}", key);
    }

    // Utility methods
    public void evictAll() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.info("Evicted all cache entries");
    }

    public boolean isConnected() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis connection failed", e);
            return false;
        }
    }
}