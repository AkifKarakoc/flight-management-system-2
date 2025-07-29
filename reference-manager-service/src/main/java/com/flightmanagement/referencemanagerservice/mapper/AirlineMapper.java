package com.flightmanagement.referencemanagerservice.mapper;

import com.flightmanagement.referencemanagerservice.dto.request.AirlineRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirlineResponse;
import com.flightmanagement.referencemanagerservice.entity.Airline;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AirlineMapper {
    Airline toEntity(AirlineRequest request);
    AirlineResponse toResponse(Airline airline);
    void updateEntity(@MappingTarget Airline airline, AirlineRequest request);
}