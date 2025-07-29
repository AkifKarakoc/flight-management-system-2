package com.flightmanagement.flightservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlightServiceApplication {

    public static void main(String[] args) {
        System.out.println("🛫 Starting Flight Service...");
        SpringApplication.run(FlightServiceApplication.class, args);
        System.out.println("✅ Flight Service Started Successfully!");
    }
}