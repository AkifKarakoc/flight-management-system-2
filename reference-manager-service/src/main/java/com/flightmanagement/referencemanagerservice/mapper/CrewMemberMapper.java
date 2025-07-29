package com.flightmanagement.referencemanagerservice.mapper;

import com.flightmanagement.referencemanagerservice.dto.request.CrewMemberRequest;
import com.flightmanagement.referencemanagerservice.dto.response.CrewMemberResponse;
import com.flightmanagement.referencemanagerservice.entity.CrewMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AirportMapper.class, AirlineMapper.class})
public interface CrewMemberMapper {
    @Mapping(target = "airline", ignore = true)
    CrewMember toEntity(CrewMemberRequest request);

    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "age", target = "age")
    @Mapping(expression = "java(crewMember.isLicenseValid())", target = "licenseValid")
    CrewMemberResponse toResponse(CrewMember crewMember);

    @Mapping(target = "airline", ignore = true)
    void updateEntity(@MappingTarget CrewMember crewMember, CrewMemberRequest request);
}