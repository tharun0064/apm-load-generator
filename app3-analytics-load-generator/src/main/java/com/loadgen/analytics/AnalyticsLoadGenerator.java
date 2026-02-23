package com.loadgen.analytics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Analytics Load Generator - Simulates heavy analytical queries via REST APIs
 * This application generates massive analytical load by calling REST endpoints:
 * - Complex multi-table joins
 * - Aggregations and grouping
 * - Large data scans
 * - Reporting queries
 * - Data warehouse style operations
 */
@Component
public class AnalyticsLoadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsLoadGenerator.class);
    private static final Random random = new Random();

    private final RestTemplate restTemplate;
    private final DatabaseManager dbManager;
    private final int numThreads;
    private final String apiBaseUrl;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public AnalyticsLoadGenerator(RestTemplate restTemplate,
                                  DatabaseManager dbManager,
                                  @Value("${threads:10}") int numThreads,
                                  @Value("${api.base.url:http://localhost:8081}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.dbManager = dbManager;
        this.numThreads = numThreads;
        this.apiBaseUrl = apiBaseUrl;
        this.executorService = Executors.newFixedThreadPool(numThreads);

        logger.info("Analytics Load Generator initialized with {} threads", numThreads);
        logger.info("API Base URL: {}", apiBaseUrl);
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "Initialize");
    }

    @Trace(dispatcher = true)
    public void start() {
        logger.info("Starting Analytics load generation...");
        NewRelic.addCustomParameter("threads", numThreads);

        List<Future<?>> futures = new ArrayList<>();

        // Start multiple worker threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Future<?> future = executorService.submit(() -> runWorker(threadId));
            futures.add(future);
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, stopping load generator...");
            shutdown();
        }));

        // Keep main thread alive and log statistics
        try {
            while (running) {
                Thread.sleep(30000); // Log every 30 seconds
                logStatistics();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Main thread interrupted", e);
        }
    }

    @Trace(dispatcher = true)
    private void runWorker(int threadId) {
        logger.info("Analytics worker thread {} started", threadId);
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "Worker");
        NewRelic.addCustomParameter("threadId", threadId);

        int queryCount = 0;
        long lastBreakTime = System.currentTimeMillis();
        int cycleQueries = 0;

        while (running) {
            try {
                // Check if it's time for a break (sustainable load)
                long currentTime = System.currentTimeMillis();
                long timeSinceBreak = currentTime - lastBreakTime;

                // Take a break every 3-5 minutes (randomized per thread)
                int breakInterval = 180000 + random.nextInt(120000); // 180-300 seconds (3-5 minutes)
                if (timeSinceBreak > breakInterval) {
                    logger.info("Analytics worker thread {} taking 3-second break after {} queries", threadId, cycleQueries);
                    Thread.sleep(3000); // 3 second break
                    lastBreakTime = System.currentTimeMillis();
                    cycleQueries = 0;
                    logger.info("Analytics worker thread {} resuming work", threadId);
                }

                // Randomly select analytical operation
                int operation = random.nextInt(100);

                if (operation < 25) {
                    // 25% - Sales analytics queries
                    salesAnalyticsWorkflow();
                } else if (operation < 45) {
                    // 20% - Customer analytics
                    customerAnalyticsWorkflow();
                } else if (operation < 65) {
                    // 20% - Product performance analytics
                    productAnalyticsWorkflow();
                } else if (operation < 85) {
                    // 20% - Reporting queries
                    reportingWorkflow();
                } else {
                    // 15% - Data warehouse operations
                    dataWarehouseWorkflow();
                }

                queryCount++;
                cycleQueries++;

                // MODERATE-HEAVY LOAD - gradual increase for testing
                Thread.sleep(random.nextInt(15) + 8); // 8-23ms delay = MODERATE-HEAVY LOAD

            } catch (Exception e) {
                logger.error("Error in analytics worker thread {}: {}", threadId, e.getMessage());
                NewRelic.noticeError(e);
                // Continue running even on errors, but add small delay to prevent tight error loops
                try {
                    Thread.sleep(2000); // 2 second backoff on error (longer for analytics)
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Analytics worker thread {} completed {} queries", threadId, queryCount);
    }

    @Trace
    private void salesAnalyticsWorkflow() {
        try {
            int choice = random.nextInt(5);
            String url;

            switch (choice) {
                case 0:
                    url = apiBaseUrl + "/api/analytics/sales/daily";
                    break;
                case 1:
                    url = apiBaseUrl + "/api/analytics/sales/monthly";
                    break;
                case 2:
                    url = apiBaseUrl + "/api/analytics/sales/by-category";
                    break;
                case 3:
                    url = apiBaseUrl + "/api/analytics/sales/top-products?limit=20";
                    break;
                default:
                    url = apiBaseUrl + "/api/analytics/sales/by-payment-method";
                    break;
            }

            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            logger.error("Error in salesAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void customerAnalyticsWorkflow() {
        try {
            int choice = random.nextInt(5);
            String url;

            switch (choice) {
                case 0:
                    url = apiBaseUrl + "/api/analytics/customers/segmentation";
                    break;
                case 1:
                    url = apiBaseUrl + "/api/analytics/customers/lifetime-value";
                    break;
                case 2:
                    url = apiBaseUrl + "/api/analytics/customers/retention";
                    break;
                case 3:
                    url = apiBaseUrl + "/api/analytics/customers/high-value?limit=50";
                    break;
                default:
                    url = apiBaseUrl + "/api/analytics/customers/purchase-frequency";
                    break;
            }

            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            logger.error("Error in customerAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void productAnalyticsWorkflow() {
        try {
            int choice = random.nextInt(5);
            String url;

            switch (choice) {
                case 0:
                    url = apiBaseUrl + "/api/analytics/products/performance";
                    break;
                case 1:
                    url = apiBaseUrl + "/api/analytics/products/inventory-turnover";
                    break;
                case 2:
                    url = apiBaseUrl + "/api/analytics/products/slow-moving";
                    break;
                case 3:
                    url = apiBaseUrl + "/api/analytics/products/profit-margin";
                    break;
                default:
                    url = apiBaseUrl + "/api/analytics/products/affinity";
                    break;
            }

            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            logger.error("Error in productAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void reportingWorkflow() {
        try {
            int choice = random.nextInt(4);
            String url;

            switch (choice) {
                case 0:
                    url = apiBaseUrl + "/api/analytics/reports/executive";
                    break;
                case 1:
                    url = apiBaseUrl + "/api/analytics/reports/sales";
                    break;
                case 2:
                    url = apiBaseUrl + "/api/analytics/reports/inventory";
                    break;
                default:
                    url = apiBaseUrl + "/api/analytics/reports/customer";
                    break;
            }

            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            logger.error("Error in reportingWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void dataWarehouseWorkflow() {
        try {
            int choice = random.nextInt(6);
            String url;

            switch (choice) {
                case 0:
                    url = apiBaseUrl + "/api/analytics/warehouse/aggregate-sales";
                    restTemplate.postForObject(url, null, String.class);
                    break;
                case 1:
                    url = apiBaseUrl + "/api/analytics/warehouse/aggregate-customers";
                    restTemplate.postForObject(url, null, String.class);
                    break;
                case 2:
                    url = apiBaseUrl + "/api/analytics/warehouse/aggregate-products";
                    restTemplate.postForObject(url, null, String.class);
                    break;
                case 3:
                    // VERY HEAVY - Full table scan
                    url = apiBaseUrl + "/api/analytics/warehouse/full-scan";
                    restTemplate.getForObject(url, String.class);
                    break;
                case 4:
                    // VERY HEAVY - Complex 5-table join
                    url = apiBaseUrl + "/api/analytics/warehouse/complex-join";
                    restTemplate.getForObject(url, String.class);
                    break;
                default:
                    // VERY HEAVY - Customer data with comprehensive order and product history (5-10 seconds)
                    url = apiBaseUrl + "/api/analytics/customer-data";
                    restTemplate.getForObject(url, String.class);
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in dataWarehouseWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    private void logStatistics() {
        try {
            int activeConnections = dbManager.getActiveConnections();
            logger.info("Active DB connections: {}", activeConnections);
            NewRelic.recordMetric("Custom/Database/ActiveConnections", activeConnections);
        } catch (Exception e) {
            logger.error("Error logging statistics", e);
        }
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Analytics Load Generator shutdown complete");
    }
}
