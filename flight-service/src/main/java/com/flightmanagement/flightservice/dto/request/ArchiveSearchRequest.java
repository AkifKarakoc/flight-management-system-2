package com.flightmanagement.flightservice.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchiveSearchRequest {

    private String flightNumber;
    private Long airlineId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String eventType;
    private String routeCode;
    private String originAirportCode;
    private String destinationAirportCode;

    // Search options
    private boolean includePayload = false;
    private boolean sortByDateDesc = true;
    private int maxResults = 100;
}