package com.flightmanagement.referencemanagerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReferenceManagerServiceApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Reference Manager Service...");
        SpringApplication.run(ReferenceManagerServiceApplication.class, args);
        System.out.println("✅ Reference Manager Service Started Successfully!");
    }
}