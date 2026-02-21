package com.loadgen.oltp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Boot Application for OLTP Load Generator
 * Exposes REST APIs and runs load generation worker threads
 */
@SpringBootApplication
public class OltpApplication {
    private static final Logger logger = LoggerFactory.getLogger(OltpApplication.class);

    @Autowired
    @Lazy
    private OltpLoadGenerator loadGenerator;

    @Autowired
    private TableCleanupService tableCleanupService;

    public static void main(String[] args) {
        logger.info("=" .repeat(80));
        logger.info("OLTP Load Generator Starting");
        logger.info("=" .repeat(80));
        SpringApplication.run(OltpApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startLoadGeneration() {
        logger.info("Application ready, cleaning up tables before starting load generation...");

        // Clean up all tables before starting
        try {
            tableCleanupService.truncateAllTables();
        } catch (Exception e) {
            logger.error("Failed to clean up tables, aborting startup", e);
            System.exit(1);
        }

        // Start load generator in a separate thread so it doesn't block Spring Boot
        new Thread(() -> loadGenerator.start(), "LoadGeneratorMain").start();
    }
}
