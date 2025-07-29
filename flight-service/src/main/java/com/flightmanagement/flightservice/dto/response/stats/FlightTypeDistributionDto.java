package com.flightmanagement.flightservice.dto.response.stats;

import com.flightmanagement.flightservice.entity.enums.FlightType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightTypeDistributionDto {
    private FlightType type;
    private long count;
}