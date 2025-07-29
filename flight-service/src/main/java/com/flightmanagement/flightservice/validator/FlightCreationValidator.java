package com.flightmanagement.flightservice.validator;

import com.flightmanagement.flightservice.dto.request.FlightRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FlightCreationValidator implements ConstraintValidator<ValidFlightCreation, FlightRequest> {

    @Override
    public boolean isValid(FlightRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        boolean hasRoute = request.getRouteId() != null;
        boolean hasAirports = request.getOriginAirportId() != null && request.getDestinationAirportId() != null;
        boolean hasMultiSegments = request.getAirportSegments() != null && !request.getAirportSegments().isEmpty();

        // En azından bir creation method olmalı
        if (!hasRoute && !hasAirports && !hasMultiSegments) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Either routeId, airport pair (originAirportId + destinationAirportId), or airportSegments must be provided")
                    .addConstraintViolation();
            return false;
        }

        // Multi-segment mode validation
        if (Boolean.TRUE.equals(request.getIsMultiSegmentAirportFlight())) {
            if (!hasMultiSegments) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Multi-segment flight requires airportSegments")
                        .addConstraintViolation();
                return false;
            }

            if (request.getAirportSegments().size() < 2) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Multi-segment flight must have at least 2 segments")
                        .addConstraintViolation();
                return false;
            }
        }

        // Conflicting creation modes check
        int creationModeCount = 0;
        if (hasRoute) creationModeCount++;
        if (hasAirports && !Boolean.TRUE.equals(request.getIsMultiSegmentAirportFlight())) creationModeCount++;
        if (hasMultiSegments && Boolean.TRUE.equals(request.getIsMultiSegmentAirportFlight())) creationModeCount++;

        if (creationModeCount > 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Only one creation mode should be used: routeId OR airport pair OR multi-segment airports")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}