package com.flightmanagement.referencemanagerservice.mapper;

import com.flightmanagement.referencemanagerservice.dto.request.AircraftRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AircraftResponse;
import com.flightmanagement.referencemanagerservice.entity.Aircraft;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AirlineMapper.class})
public interface AircraftMapper {
    @Mapping(target = "airline", ignore = true)
    Aircraft toEntity(AircraftRequest request);

    AircraftResponse toResponse(Aircraft aircraft);

    @Mapping(target = "airline", ignore = true)
    void updateEntity(@MappingTarget Aircraft aircraft, AircraftRequest request);
}

// Note: The AirlineMapper is assumed to be defined elsewhere in the project, providing the necessary mappings for the Airline entity.