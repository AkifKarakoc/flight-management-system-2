package com.flightmanagement.flightarchiveservice.mapper;

import com.flightmanagement.flightarchiveservice.dto.response.FlightArchiveResponse;
import com.flightmanagement.flightarchiveservice.entity.FlightArchive;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlightArchiveMapper {

    @Mapping(expression = "java(archive.isDelayed())", target = "isDelayed")
    @Mapping(expression = "java(archive.isCompleted())", target = "isCompleted")
    @Mapping(expression = "java(archive.isCancelled())", target = "isCancelled")
    @Mapping(expression = "java(archive.getFlightDuration())", target = "flightDuration")
    FlightArchiveResponse toResponse(FlightArchive archive);
}