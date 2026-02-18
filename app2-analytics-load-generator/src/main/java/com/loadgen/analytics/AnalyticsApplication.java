package com.loadgen.analytics;

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
 * Spring Boot Application for Analytics Load Generator
 * Exposes REST APIs and runs load generation worker threads
 */
@SpringBootApplication
public class AnalyticsApplication {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsApplication.class);

    @Autowired
    @Lazy
    private AnalyticsLoadGenerator loadGenerator;

    public static void main(String[] args) {
        logger.info("=".repeat(80));
        logger.info("Analytics Load Generator Starting");
        logger.info("=".repeat(80));
        SpringApplication.run(AnalyticsApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startLoadGeneration() {
        logger.info("Application ready, starting load generation...");
        // Start load generator in a separate thread so it doesn't block Spring Boot
        new Thread(() -> loadGenerator.start(), "LoadGeneratorMain").start();
    }
}
