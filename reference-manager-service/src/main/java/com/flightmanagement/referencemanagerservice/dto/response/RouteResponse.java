package com.flightmanagement.referencemanagerservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flightmanagement.referencemanagerservice.entity.enums.RouteType;
import com.flightmanagement.referencemanagerservice.entity.enums.RouteVisibility;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResponse {
    private Long id;
    private String routeCode;
    private String routeName;
    private Integer distance;
    private Integer estimatedFlightTime;

    private Boolean active;
    private RouteType routeType;

    // Ownership bilgileri
    private Long createdByUserId;
    private String createdByUserName; // Frontend için kullanıcı adı
    private RouteVisibility visibility;
    private Long airlineId;
    private String airlineName; // Frontend için havayolu adı

    // Multi-segment route bilgileri
    private List<RouteSegmentResponse> segments;

    // Hesaplanan alanlar
    private Integer totalDistance; // Tüm segment'lerin toplam mesafesi
    private Integer totalEstimatedTime; // Tüm segment'lerin toplam süresi
    private String routePath; // "IST → ANK → IZM" şeklinde string
    private Integer segmentCount; // Segment sayısı

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}