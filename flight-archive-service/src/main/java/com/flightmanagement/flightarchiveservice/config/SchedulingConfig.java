package com.flightmanagement.flightarchiveservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "archive.kpi.calculation.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}