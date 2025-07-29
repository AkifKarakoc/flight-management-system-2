package com.flightmanagement.flightservice.service;

import com.flightmanagement.flightservice.dto.cache.AircraftCache;
import com.flightmanagement.flightservice.dto.cache.AirlineCache;
import com.flightmanagement.flightservice.dto.cache.AirportCache;
import com.flightmanagement.flightservice.dto.cache.RouteCache;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientReferenceDataService {

    private final ReferenceDataService referenceDataService;
    private final CacheService cacheService;

    @CircuitBreaker(name = "reference-service", fallbackMethod = "getAirlineFallback")
    @Retry(name = "reference-service")
    @TimeLimiter(name = "reference-service")
    public CompletableFuture<AirlineCache> getAirlineAsync(Long airlineId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching airline {} with circuit breaker", airlineId);

            // Önce cache'den kontrol et
            AirlineCache cached = cacheService.getAirlineFromCache(airlineId);
            if (cached != null) {
                log.debug("Airline {} found in cache", airlineId);
                return cached;
            }

            // Cache'de yoksa service'den al
            AirlineCache airline = referenceDataService.getAirline(airlineId);

            // Cache'e kaydet
            if (airline != null) {
                cacheService.cacheAirline(airlineId, airline);
            }

            return airline;
        });
    }

    @CircuitBreaker(name = "reference-service", fallbackMethod = "getAirportFallback")
    @Retry(name = "reference-service")
    @TimeLimiter(name = "reference-service")
    public CompletableFuture<AirportCache> getAirportAsync(Long airportId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching airport {} with circuit breaker", airportId);

            // Önce cache'den kontrol et
            AirportCache cached = cacheService.getAirportFromCache(airportId);
            if (cached != null) {
                log.debug("Airport {} found in cache", airportId);
                return cached;
            }

            // Cache'de yoksa service'den al
            AirportCache airport = referenceDataService.getAirport(airportId);

            // Cache'e kaydet
            if (airport != null) {
                cacheService.cacheAirport(airportId, airport);
            }

            return airport;
        });
    }

    @CircuitBreaker(name = "reference-service", fallbackMethod = "getAircraftFallback")
    @Retry(name = "reference-service")
    @TimeLimiter(name = "reference-service")
    public CompletableFuture<AircraftCache> getAircraftAsync(Long aircraftId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching aircraft {} with circuit breaker", aircraftId);

            // Önce cache'den kontrol et
            AircraftCache cached = cacheService.getAircraftFromCache(aircraftId);
            if (cached != null) {
                log.debug("Aircraft {} found in cache", aircraftId);
                return cached;
            }

            // Cache'de yoksa service'den al
            AircraftCache aircraft = referenceDataService.getAircraft(aircraftId);

            // Cache'e kaydet
            if (aircraft != null) {
                cacheService.cacheAircraft(aircraftId, aircraft);
            }

            return aircraft;
        });
    }

    @CircuitBreaker(name = "reference-service", fallbackMethod = "getRouteFallback")
    @Retry(name = "reference-service")
    @TimeLimiter(name = "reference-service")
    public CompletableFuture<RouteCache> getRouteAsync(Long routeId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Fetching route {} with circuit breaker", routeId);

            // Route için cache implementasyonu eklenebilir
            RouteCache route = referenceDataService.getRoute(routeId);

            return route;
        });
    }

    // Synchronous wrapper methods for backward compatibility
    public AirlineCache getAirline(Long airlineId) {
        try {
            return getAirlineAsync(airlineId).get();
        } catch (Exception e) {
            log.error("Failed to get airline {}: {}", airlineId, e.getMessage());
            return getAirlineSyncFallback(airlineId, e);
        }
    }

    public AirportCache getAirport(Long airportId) {
        try {
            return getAirportAsync(airportId).get();
        } catch (Exception e) {
            log.error("Failed to get airport {}: {}", airportId, e.getMessage());
            return getAirportSyncFallback(airportId, e);
        }
    }

    public AircraftCache getAircraft(Long aircraftId) {
        try {
            return getAircraftAsync(aircraftId).get();
        } catch (Exception e) {
            log.error("Failed to get aircraft {}: {}", aircraftId, e.getMessage());
            return getAircraftSyncFallback(aircraftId, e);
        }
    }

    public RouteCache getRoute(Long routeId) {
        try {
            return getRouteAsync(routeId).get();
        } catch (Exception e) {
            log.error("Failed to get route {}: {}", routeId, e.getMessage());
            return getRouteSyncFallback(routeId, e);
        }
    }

    // Fallback methods - Circuit breaker açık olduğunda çalışır
    public CompletableFuture<AirlineCache> getAirlineFallback(Long airlineId, Exception ex) {
        log.warn("Using fallback for airline {}: {}", airlineId, ex.getMessage());

        // Önce cache'den dene
        AirlineCache cached = cacheService.getAirlineFromCache(airlineId);
        if (cached != null) {
            log.info("Returning cached airline {} during fallback", airlineId);
            return CompletableFuture.completedFuture(cached);
        }

        // Cache'de de yoksa default değer
        AirlineCache fallback = createFallbackAirline(airlineId);
        return CompletableFuture.completedFuture(fallback);
    }

    public CompletableFuture<AirportCache> getAirportFallback(Long airportId, Exception ex) {
        log.warn("Using fallback for airport {}: {}", airportId, ex.getMessage());

        // Önce cache'den dene
        AirportCache cached = cacheService.getAirportFromCache(airportId);
        if (cached != null) {
            log.info("Returning cached airport {} during fallback", airportId);
            return CompletableFuture.completedFuture(cached);
        }

        // Cache'de de yoksa default değer
        AirportCache fallback = createFallbackAirport(airportId);
        return CompletableFuture.completedFuture(fallback);
    }

    public CompletableFuture<AircraftCache> getAircraftFallback(Long aircraftId, Exception ex) {
        log.warn("Using fallback for aircraft {}: {}", aircraftId, ex.getMessage());

        // Önce cache'den dene
        AircraftCache cached = cacheService.getAircraftFromCache(aircraftId);
        if (cached != null) {
            log.info("Returning cached aircraft {} during fallback", aircraftId);
            return CompletableFuture.completedFuture(cached);
        }

        // Cache'de de yoksa default değer
        AircraftCache fallback = createFallbackAircraft(aircraftId);
        return CompletableFuture.completedFuture(fallback);
    }

    public CompletableFuture<RouteCache> getRouteFallback(Long routeId, Exception ex) {
        log.warn("Using fallback for route {}: {}", routeId, ex.getMessage());

        RouteCache fallback = createFallbackRoute(routeId);
        return CompletableFuture.completedFuture(fallback);
    }

    // Sync fallback methods for direct calls
    public AirlineCache getAirlineSyncFallback(Long airlineId, Exception ex) {
        log.warn("Using sync fallback for airline {}: {}", airlineId, ex.getMessage());

        AirlineCache cached = cacheService.getAirlineFromCache(airlineId);
        if (cached != null) {
            return cached;
        }

        return createFallbackAirline(airlineId);
    }

    public AirportCache getAirportSyncFallback(Long airportId, Exception ex) {
        log.warn("Using sync fallback for airport {}: {}", airportId, ex.getMessage());

        AirportCache cached = cacheService.getAirportFromCache(airportId);
        if (cached != null) {
            return cached;
        }

        return createFallbackAirport(airportId);
    }

    public AircraftCache getAircraftSyncFallback(Long aircraftId, Exception ex) {
        log.warn("Using sync fallback for aircraft {}: {}", aircraftId, ex.getMessage());

        AircraftCache cached = cacheService.getAircraftFromCache(aircraftId);
        if (cached != null) {
            return cached;
        }

        return createFallbackAircraft(aircraftId);
    }

    public RouteCache getRouteSyncFallback(Long routeId, Exception ex) {
        log.warn("Using sync fallback for route {}: {}", routeId, ex.getMessage());
        return createFallbackRoute(routeId);
    }

    // Helper methods to create fallback objects
    private AirlineCache createFallbackAirline(Long airlineId) {
        AirlineCache fallback = new AirlineCache();
        fallback.setId(airlineId);
        fallback.setName("Unknown Airline");
        fallback.setIataCode("??");
        fallback.setIcaoCode("???");
        fallback.setCountry("Unknown");
        fallback.setType("UNKNOWN");
        fallback.setActive(false);
        return fallback;
    }

    private AirportCache createFallbackAirport(Long airportId) {
        AirportCache fallback = new AirportCache();
        fallback.setId(airportId);
        fallback.setName("Unknown Airport");
        fallback.setIataCode("???");
        fallback.setIcaoCode("????");
        fallback.setCity("Unknown");
        fallback.setCountry("Unknown");
        fallback.setType("UNKNOWN");
        fallback.setActive(false);
        return fallback;
    }

    private AircraftCache createFallbackAircraft(Long aircraftId) {
        AircraftCache fallback = new AircraftCache();
        fallback.setId(aircraftId);
        fallback.setRegistrationNumber("UNKNOWN");
        fallback.setAircraftType("UNKNOWN");
        fallback.setManufacturer("Unknown");
        fallback.setModel("Unknown");
        fallback.setSeatCapacity(0);
        fallback.setStatus("UNKNOWN");
        fallback.setAirlineId(0L);
        return fallback;
    }

    private RouteCache createFallbackRoute(Long routeId) {
        RouteCache fallback = new RouteCache();
        fallback.setId(routeId);
        fallback.setRouteName("Unknown Route");
        fallback.setRouteCode("UNKNOWN");
        fallback.setRoutePath("??? → ???");
        fallback.setRouteType("UNKNOWN");
        fallback.setDistance(0);
        fallback.setEstimatedFlightTime(0);
        fallback.setActive(false);
        return fallback;
    }

    // Health check methods
    public boolean isReferenceServiceHealthy() {
        return referenceDataService.isReferenceServiceHealthy();
    }

    public RouteCache[] getActiveRoutes() {
        try {
            return referenceDataService.getActiveRoutes();
        } catch (Exception e) {
            log.error("Failed to get active routes: {}", e.getMessage());
            return new RouteCache[0];
        }
    }
}