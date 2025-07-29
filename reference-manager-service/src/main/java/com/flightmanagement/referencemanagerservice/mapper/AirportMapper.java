package com.flightmanagement.referencemanagerservice.mapper;

import com.flightmanagement.referencemanagerservice.dto.request.AirportRequest;
import com.flightmanagement.referencemanagerservice.dto.response.AirportResponse;
import com.flightmanagement.referencemanagerservice.entity.Airport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AirportMapper {
    Airport toEntity(AirportRequest request);

    AirportResponse toResponse(Airport airport);

    void updateEntity(@MappingTarget Airport airport, AirportRequest request);
}