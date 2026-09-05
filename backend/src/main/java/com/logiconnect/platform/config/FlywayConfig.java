package com.logiconnect.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration strategy that automatically repairs schema history checksums
 * before applying migrations, preventing checksum mismatch errors when seed/migration
 * scripts are updated during development.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Running Flyway repair to synchronize schema history checksums with local scripts...");
            flyway.repair();
            log.info("Executing Flyway database migrations...");
            flyway.migrate();
        };
    }
}
