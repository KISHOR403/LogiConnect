package com.logiconnect.platform;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

/**
 * LogiConnect Platform - Main Spring Boot Application Entrypoint.
 *
 * Designed for ~2,000 logistics company employees, providing secure, high-throughput,
 * stateless communication, collaboration, and operational coordination.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LogiConnectApplication {

    private static final Logger log = LoggerFactory.getLogger(LogiConnectApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LogiConnectApplication.class, args);
    }

    /**
     * Enforce standard UTC timezone across all JVM operations, date serialization,
     * and database timestamp interactions.
     */
    @PostConstruct
    public void initializeTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("LogiConnect Platform initialized with default timezone: UTC");
    }
}
