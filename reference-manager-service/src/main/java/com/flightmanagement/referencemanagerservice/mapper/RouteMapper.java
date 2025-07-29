package com.flightmanagement.referencemanagerservice.mapper;

import com.flightmanagement.referencemanagerservice.dto.request.RouteRequest;
import com.flightmanagement.referencemanagerservice.dto.request.RouteSegmentRequest;
import com.flightmanagement.referencemanagerservice.dto.response.RouteResponse;
import com.flightmanagement.referencemanagerservice.dto.response.RouteSegmentResponse;
import com.flightmanagement.referencemanagerservice.entity.Route;
import com.flightmanagement.referencemanagerservice.entity.RouteSegment;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AirportMapper.class})
public interface RouteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Route toEntity(RouteRequest request);

    @Mapping(target = "segments", source = "segments")
    @Mapping(target = "totalDistance", expression = "java(calculateTotalDistance(route))")
    @Mapping(target = "totalEstimatedTime", expression = "java(calculateTotalEstimatedTime(route))")
    @Mapping(target = "routePath", expression = "java(buildRoutePath(route))")
    @Mapping(target = "segmentCount", expression = "java(getSegmentCount(route))")
    @Mapping(target = "createdByUserName", ignore = true)
    @Mapping(target = "airlineName", ignore = true)
    RouteResponse toResponse(Route route);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Route route, RouteRequest request);

    // RouteSegment mapping
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "originAirport", ignore = true)
    @Mapping(target = "destinationAirport", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RouteSegment toSegmentEntity(RouteSegmentRequest request);

    @Mapping(target = "segmentPath", expression = "java(buildSegmentPath(segment))")
    RouteSegmentResponse toSegmentResponse(RouteSegment segment);

    List<RouteSegmentResponse> toSegmentResponseList(List<RouteSegment> segments);

    // Helper methods for calculations
    default Integer calculateTotalDistance(Route route) {
        if (route == null) return null;
        if (route.getSegments() != null) {
            return route.getSegments().stream()
                    .filter(segment -> segment.getDistance() != null)
                    .mapToInt(RouteSegment::getDistance)
                    .sum();
        }
        return route.getDistance();
    }

    default Integer calculateTotalEstimatedTime(Route route) {
        if (route == null) return null;
        if (route.getSegments() != null) {
            return route.getSegments().stream()
                    .filter(segment -> segment.getEstimatedFlightTime() != null)
                    .mapToInt(RouteSegment::getEstimatedFlightTime)
                    .sum();
        }
        return route.getEstimatedFlightTime();
    }

    default String buildRoutePath(Route route) {
        if (route == null) return null;
        if (route.getSegments() != null && !route.getSegments().isEmpty()) {
            StringBuilder path = new StringBuilder();
            List<RouteSegment> sortedSegments = route.getSegments().stream()
                    .sorted((s1, s2) -> Integer.compare(s1.getSegmentOrder(), s2.getSegmentOrder()))
                    .collect(Collectors.toList());
            if (!sortedSegments.isEmpty() && sortedSegments.get(0).getOriginAirport() != null) {
                path.append(sortedSegments.get(0).getOriginAirport().getIataCode());
            }
            for (RouteSegment segment : sortedSegments) {
                if (segment.getDestinationAirport() != null) {
                    path.append(" → ").append(segment.getDestinationAirport().getIataCode());
                }
            }
            return path.toString();
        }
        return null;
    }

    default String buildSegmentPath(RouteSegment segment) {
        if (segment == null) return null;

        StringBuilder path = new StringBuilder();
        if (segment.getOriginAirport() != null) {
            path.append(segment.getOriginAirport().getIataCode());
        }
        if (segment.getDestinationAirport() != null) {
            if (path.length() > 0) {
                path.append(" → ");
            }
            path.append(segment.getDestinationAirport().getIataCode());
        }
        return path.toString();
    }

    default Integer getSegmentCount(Route route) {
        if (route == null) return 0;
        if (route.getSegments() != null) {
            return route.getSegments().size();
        }
        return 0;
    }
}