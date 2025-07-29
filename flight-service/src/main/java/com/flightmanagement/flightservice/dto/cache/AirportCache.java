package com.flightmanagement.flightservice.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirportCache implements Serializable {
    private Long id;
    private String iataCode;
    private String icaoCode;
    private String name;
    private String city;
    private String country;
    private String timezone;
    private Double latitude;
    private Double longitude;
    private String type;
    private Boolean active;
}