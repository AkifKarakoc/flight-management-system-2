package com.flightmanagement.flightservice.validator;

import com.flightmanagement.flightservice.dto.cache.RouteCache;
import com.flightmanagement.flightservice.dto.request.AirportSegmentRequest;
import com.flightmanagement.flightservice.dto.request.FlightRequest;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.exception.BusinessException;
import com.flightmanagement.flightservice.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightValidator {

    private final ReferenceDataService referenceDataService;

    public void validateFlightRequest(FlightRequest request) {
        validateBasicRules(request);

        if (request.isRouteBasedCreation()) {
            validateRouteData(request);
        }

        if (request.isMultiSegmentAirportCreation()) {
            validateMultiSegmentAirportData(request);
        }

        validateReferenceData(request);
        validateBusinessRules(request);
    }

    public void validateFlightUpdate(Flight existingFlight, FlightRequest request) {
        validateBasicRules(request);
        validateRouteData(request);
        validateReferenceData(request);
        validateBusinessRules(request);
        validateUpdateRules(existingFlight, request);
    }

    private void validateBasicRules(FlightRequest request) {
        // Flight time consistency kontrolü
        if (!request.isFlightTimeValid()) {
            throw new BusinessException("Scheduled arrival must be after scheduled departure");
        }

        // Route veya Airport bilgisi zorunlu
        if (!request.hasValidFlightData()) {
            throw new BusinessException("Either routeId, airport pair, or airport segments must be provided");
        }

        // Flight date, scheduled departure ile uyumlu olmalı
        if (request.getFlightDate() != null && request.getScheduledDeparture() != null) {
            if (!request.getFlightDate().equals(request.getScheduledDeparture().toLocalDate())) {
                throw new BusinessException("Flight date must match scheduled departure date");
            }
        }
    }

    private void validateRouteData(FlightRequest request) {
        log.debug("Validating route-based flight with route ID: {}", request.getRouteId());

        try {
            RouteCache route = referenceDataService.getRoute(request.getRouteId());
            if (route == null) {
                throw new BusinessException("Route not found with ID: " + request.getRouteId());
            }

            if (route.getActive() != null && !route.getActive()) {
                throw new BusinessException("Route is not active: " + request.getRouteId());
            }

            // Eğer request'te airport bilgileri de varsa tutarlılık kontrolü yap
            if (request.isAirportBasedCreation()) {
                validateRouteAirportConsistency(request, route);
            }

            log.debug("Route validation passed for route: {}", route.getRouteCode());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Invalid route ID: " + request.getRouteId());
        }
    }

    private void validateReferenceData(FlightRequest request) {
        // Airline validation
        try {
            var airline = referenceDataService.getAirline(request.getAirlineId());
            if (airline == null) {
                throw new BusinessException("Airline not found with ID: " + request.getAirlineId());
            }
            if (airline.getActive() != null && !airline.getActive()) {
                throw new BusinessException("Airline is not active: " + request.getAirlineId());
            }
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Invalid airline ID: " + request.getAirlineId());
        }

        // Aircraft validation
        try {
            var aircraft = referenceDataService.getAircraft(request.getAircraftId());
            if (aircraft == null) {
                throw new BusinessException("Aircraft not found with ID: " + request.getAircraftId());
            }
            if (aircraft.getStatus() != null && !aircraft.getStatus().equals("ACTIVE")) {
                throw new BusinessException("Aircraft is not active: " + request.getAircraftId());
            }
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Invalid aircraft ID: " + request.getAircraftId());
        }
    }

    private void validateBusinessRules(FlightRequest request) {
        // Flight number format kontrolü
        if (request.getFlightNumber() != null &&
                !request.getFlightNumber().matches("^[A-Z]{2}\\d{1,4}$")) {
            throw new BusinessException("Flight number must be in format: TK123");
        }

        // Passenger count vs aircraft capacity kontrolü
        try {
            var aircraft = referenceDataService.getAircraft(request.getAircraftId());
            if (request.getPassengerCount() != null && aircraft.getPassengerCapacity() != null) {
                if (request.getPassengerCount() > aircraft.getPassengerCapacity()) {
                    throw new BusinessException(
                            String.format("Passenger count (%d) exceeds aircraft capacity (%d)",
                                    request.getPassengerCount(), aircraft.getPassengerCapacity())
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Could not validate passenger capacity: {}", e.getMessage());
            // Aircraft bilgisi alınamazsa capacity kontrolü yapma
        }

        // Departure time gelecekte olmalı (yeni uçuşlar için)
        if (request.getScheduledDeparture() != null &&
                request.getScheduledDeparture().isBefore(LocalDateTime.now().minusHours(1))) {
            // 1 saat tolerans veriyoruz
            throw new BusinessException("Scheduled departure cannot be more than 1 hour in the past");
        }

        // Flight type vs cargo/passenger validation
        validateFlightTypeConsistency(request);

        // Connecting flight validation
        if (request.isConnectingFlightRequest()) {
            validateConnectingFlightSegments(request);
        }
    }

    private void validateFlightTypeConsistency(FlightRequest request) {
        switch (request.getType()) {
            case CARGO:
                if (request.getPassengerCount() != null && request.getPassengerCount() > 0) {
                    throw new BusinessException("CARGO flights cannot have passengers");
                }
                if (request.getCargoWeight() == null || request.getCargoWeight() <= 0) {
                    throw new BusinessException("CARGO flights must have cargo weight");
                }
                break;
            case PASSENGER:
                if (request.getPassengerCount() == null || request.getPassengerCount() <= 0) {
                    throw new BusinessException("PASSENGER flights must have passenger count");
                }
                break;
            case POSITIONING:
            case FERRY:
            case TRAINING:
                // Bu tip uçuşlar için özel kurallar eklenebilir
                break;
        }
    }

    private void validateConnectingFlightSegments(FlightRequest request) {
        if (request.getSegments() == null || request.getSegments().isEmpty()) {
            throw new BusinessException("Connecting flight must have segments");
        }

        if (request.getSegments().size() < 2) {
            throw new BusinessException("Connecting flight must have at least 2 segments");
        }

        if (request.getSegments().size() > 10) {
            throw new BusinessException("Connecting flight cannot have more than 10 segments");
        }

        // Segment'lerin bağlantısını kontrol et
        for (int i = 0; i < request.getSegments().size() - 1; i++) {
            var currentSegment = request.getSegments().get(i);
            var nextSegment = request.getSegments().get(i + 1);

            if (!currentSegment.getDestinationAirportId().equals(nextSegment.getOriginAirportId())) {
                throw new BusinessException(
                        String.format("Segment %d destination must match segment %d origin", i + 1, i + 2)
                );
            }

            // Connection time kontrolü
            if (currentSegment.getScheduledArrival().isAfter(nextSegment.getScheduledDeparture())) {
                throw new BusinessException(
                        String.format("Segment %d arrival time must be before segment %d departure time", i + 1, i + 2)
                );
            }

            // Minimum connection time (30 dakika)
            long connectionMinutes = java.time.Duration.between(
                    currentSegment.getScheduledArrival(), nextSegment.getScheduledDeparture()).toMinutes();

            if (connectionMinutes < 30) {
                throw new BusinessException(
                        String.format("Minimum 30 minutes connection time required between segments %d and %d", i + 1, i + 2)
                );
            }

            // Maximum connection time (24 saat)
            if (connectionMinutes > 1440) {
                throw new BusinessException(
                        String.format("Maximum 24 hours connection time allowed between segments %d and %d", i + 1, i + 2)
                );
            }
        }
    }

    private void validateUpdateRules(Flight existingFlight, FlightRequest request) {
        // Eğer uçuş DEPARTED veya ARRIVED durumunda ise, bazı alanlar değiştirilemez
        if (existingFlight.isDeparted()) {
            if (!existingFlight.getFlightNumber().equals(request.getFlightNumber())) {
                throw new BusinessException("Cannot change flight number after departure");
            }

            if (!existingFlight.getRouteId().equals(request.getRouteId())) {
                throw new BusinessException("Cannot change route after departure");
            }

            if (!existingFlight.getScheduledDeparture().equals(request.getScheduledDeparture())) {
                throw new BusinessException("Cannot change scheduled departure after departure");
            }
        }

        // Completed flights için daha kısıtlayıcı kurallar
        if (existingFlight.isCompleted()) {
            if (!existingFlight.getScheduledArrival().equals(request.getScheduledArrival())) {
                throw new BusinessException("Cannot change scheduled arrival for completed flights");
            }
        }

        // Cancelled flights güncellenmemeli
        if (existingFlight.getStatus() != null &&
                existingFlight.getStatus().name().equals("CANCELLED")) {
            throw new BusinessException("Cannot update cancelled flights");
        }
    }

    // Route availability validation
    public void validateRouteAvailability(Long routeId, LocalDateTime departure, LocalDateTime arrival) {
        try {
            RouteCache route = referenceDataService.getRoute(routeId);
            if (route == null) {
                throw new BusinessException("Route not found: " + routeId);
            }

            // Route'un belirli saatlerde kullanılabilir olup olmadığını kontrol et
            // Bu business requirement'a göre implement edilebilir

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Route availability check failed: " + e.getMessage());
        }
    }

    // Aircraft route compatibility validation
    public void validateAircraftRouteCompatibility(Long aircraftId, Long routeId) {
        try {
            var aircraft = referenceDataService.getAircraft(aircraftId);
            var route = referenceDataService.getRoute(routeId);

            if (aircraft == null || route == null) {
                return; // Bu kontrol referenceData validation'da yapıldı
            }

            // Aircraft range vs route distance kontrolü
            if (aircraft.getMaxRange() != null && route.getDistance() != null) {
                if (route.getDistance() > aircraft.getMaxRange()) {
                    throw new BusinessException(
                            String.format("Aircraft range (%d km) is insufficient for route distance (%d km)",
                                    aircraft.getMaxRange(), route.getDistance())
                    );
                }
            }

            // Aircraft type vs route type compatibility
            validateAircraftTypeRouteCompatibility(aircraft, route);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            log.warn("Aircraft-route compatibility check failed: {}", e.getMessage());
        }
    }

    private void validateAircraftTypeRouteCompatibility(Object aircraft, RouteCache route) {
        // Bu method aircraft type'a göre route compatibility kontrolü yapar
        // Örneğin: Domestic aircraft international route'ta uçamaz gibi kurallar
        // Business requirement'a göre implement edilebilir
    }

    private void validateRouteAirportConsistency(FlightRequest request, RouteCache route) {
        if (route.getOriginAirportId() != null && route.getDestinationAirportId() != null) {
            if (!route.getOriginAirportId().equals(request.getOriginAirportId()) ||
                    !route.getDestinationAirportId().equals(request.getDestinationAirportId())) {
                throw new BusinessException("Route airports don't match provided airport IDs");
            }
        }
    }

    /**
     * Multi-segment airport data validation
     */
    private void validateMultiSegmentAirportData(FlightRequest request) {
        List<AirportSegmentRequest> segments = request.getAirportSegments();

        if (segments == null || segments.size() < 2) {
            throw new BusinessException("Multi-segment flight must have at least 2 segments");
        }

        if (segments.size() > 10) {
            throw new BusinessException("Multi-segment flight cannot have more than 10 segments");
        }

        // Segment order validation
        for (int i = 0; i < segments.size(); i++) {
            AirportSegmentRequest segment = segments.get(i);

            if (segment.getSegmentOrder() == null || segment.getSegmentOrder() != (i + 1)) {
                throw new BusinessException("Segment order must be sequential starting from 1");
            }

            if (!segment.isValidSegment()) {
                throw new BusinessException("Invalid segment " + (i + 1) + ": origin and destination must be different");
            }

            if (!segment.hasValidConnectionTime()) {
                throw new BusinessException("Invalid connection time for segment " + (i + 1) + ": must be between 30-1440 minutes");
            }
        }

        // Segment continuity validation
        for (int i = 0; i < segments.size() - 1; i++) {
            AirportSegmentRequest current = segments.get(i);
            AirportSegmentRequest next = segments.get(i + 1);

            if (!current.getDestinationAirportId().equals(next.getOriginAirportId())) {
                throw new BusinessException(
                        String.format("Segment %d destination must match segment %d origin", i + 1, i + 2));
            }
        }

        // Airport existence validation
        for (int i = 0; i < segments.size(); i++) {
            AirportSegmentRequest segment = segments.get(i);

            try {
                var origin = referenceDataService.getAirport(segment.getOriginAirportId());
                var destination = referenceDataService.getAirport(segment.getDestinationAirportId());

                if (origin == null || !origin.getActive()) {
                    throw new BusinessException("Invalid or inactive origin airport in segment " + (i + 1));
                }

                if (destination == null || !destination.getActive()) {
                    throw new BusinessException("Invalid or inactive destination airport in segment " + (i + 1));
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) {
                    throw e;
                }
                throw new BusinessException("Error validating airports for segment " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
}