package com.flightmanagement.flightarchiveservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlightArchiveServiceApplication {

    public static void main(String[] args) {
        System.out.println("📂 Starting Flight Archive Service...");
        SpringApplication.run(FlightArchiveServiceApplication.class, args);
        System.out.println("✅ Flight Archive Service Started Successfully!");
    }
}