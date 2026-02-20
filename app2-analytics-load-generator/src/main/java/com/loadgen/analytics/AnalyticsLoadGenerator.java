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
                // Check if it's time for a 10-second break (after 30-60 seconds of work)
                long currentTime = System.currentTimeMillis();
                long timeSinceBreak = currentTime - lastBreakTime;

                // Take a break every 30-60 seconds (randomized per thread to avoid all threads breaking at once)
                int breakInterval = 30000 + random.nextInt(30000); // 30-60 seconds
                if (timeSinceBreak > breakInterval) {
                    logger.info("Analytics worker thread {} taking 10-second break after {} queries", threadId, cycleQueries);
                    Thread.sleep(10000); // 10 second break
                    lastBreakTime = System.currentTimeMillis();
                    cycleQueries = 0;
                    logger.info("Analytics worker thread {} resuming work", threadId);
                }

                // EQUAL MIXED WORKLOAD: 40% WRITES + 40% READS + 20% CLEANUP
                int operation = random.nextInt(100);

                // WRITES (40%) - Now has same write controllers as app1!
                if (operation < 12) {
                    // 12% - Create orders
                    createOrderWorkflow();
                } else if (operation < 22) {
                    // 10% - Update customers
                    customerUpdateWorkflow();
                } else if (operation < 32) {
                    // 10% - Process transactions
                    processTransactionWorkflow();
                } else if (operation < 40) {
                    // 8% - Update inventory
                    updateInventoryWorkflow();
                }
                // READS (40%) - Analytics queries
                else if (operation < 55) {
                    // 15% - Sales analytics
                    salesAnalyticsWorkflow();
                } else if (operation < 65) {
                    // 10% - Customer analytics
                    customerAnalyticsWorkflow();
                } else if (operation < 75) {
                    // 10% - Product analytics
                    productAnalyticsWorkflow();
                } else if (operation < 80) {
                    // 5% - Reporting
                    reportingWorkflow();
                }
                // CLEANUP (20%)
                else {
                    // 20% - Delete old data
                    cleanupOldDataWorkflow();
                }

                queryCount++;
                cycleQueries++;

                // MODERATE delay to prevent overwhelming PDB receiver
                Thread.sleep(random.nextInt(400) + 100); // 100-500ms delay = MODERATE LOAD

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Analytics worker thread {} interrupted", threadId);
                break;
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
            int choice = random.nextInt(5);
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
                default:
                    // VERY HEAVY - Complex 5-table join
                    url = apiBaseUrl + "/api/analytics/warehouse/complex-join";
                    restTemplate.getForObject(url, String.class);
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in dataWarehouseWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    // ============ NEW WRITE OPERATIONS (40% of workload) ============

    @Trace
    private void createOrderWorkflow() {
        try {
            // Get random customer
            long customerId = random.nextInt(1000) + 1;
            int numItems = random.nextInt(5) + 1;

            // Call REST API to create order (assumes same API structure as app1)
            String url = apiBaseUrl + "/api/orders/create?customerId=" + customerId + "&numItems=" + numItems;
            restTemplate.postForObject(url, null, String.class);

        } catch (Exception e) {
            logger.error("Error in createOrderWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void updateInventoryWorkflow() {
        try {
            long productId = random.nextInt(500) + 1;
            int choice = random.nextInt(3);
            String url;

            switch (choice) {
                case 0:
                    // Restock inventory
                    int quantity = random.nextInt(100) + 50;
                    url = apiBaseUrl + "/api/inventory/" + productId + "/restock?quantity=" + quantity;
                    restTemplate.put(url, null);
                    break;
                case 1:
                    // Update warehouse location
                    String location = "WH-" + (random.nextInt(5) + 1) + "-" + (char)('A' + random.nextInt(10));
                    url = apiBaseUrl + "/api/inventory/" + productId + "/location?location=" + location;
                    restTemplate.put(url, null);
                    break;
                default:
                    // Check and auto-restock (triggers update if low)
                    url = apiBaseUrl + "/api/inventory/" + productId + "/check";
                    restTemplate.getForObject(url, String.class);
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in updateInventoryWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void processTransactionWorkflow() {
        try {
            // Get random recent order ID
            long orderId = random.nextInt(1000) + 1;

            // Process payment via REST API
            String url = apiBaseUrl + "/api/transactions/process?orderId=" + orderId;
            restTemplate.postForObject(url, null, String.class);

        } catch (Exception e) {
            logger.error("Error in processTransactionWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void customerUpdateWorkflow() {
        try {
            long customerId = random.nextInt(1000) + 1;
            int choice = random.nextInt(3);
            String url;

            switch (choice) {
                case 0:
                    // Update customer loyalty points
                    int points = random.nextInt(100);
                    url = apiBaseUrl + "/api/customers/" + customerId + "/loyalty?points=" + points;
                    restTemplate.put(url, null);
                    break;
                case 1:
                    // Upgrade customer type (10% chance)
                    if (random.nextInt(10) == 0) {
                        url = apiBaseUrl + "/api/customers/" + customerId + "/upgrade";
                        restTemplate.put(url, null);
                    }
                    break;
                default:
                    // Log customer access
                    url = apiBaseUrl + "/api/customers/" + customerId + "/access-log";
                    restTemplate.postForObject(url, null, String.class);
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in customerUpdateWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    // ============ CLEANUP OPERATIONS (10% of workload) ============

    @Trace
    private void cleanupOldDataWorkflow() {
        try {
            int choice = random.nextInt(3);
            String url;

            switch (choice) {
                case 0:
                    // Delete old orders (>30 days)
                    url = apiBaseUrl + "/api/orders/old?daysToKeep=30";
                    restTemplate.delete(url);
                    logger.debug("Deleted old orders (>30 days)");
                    break;
                case 1:
                    // Expire old sessions (>2 hours)
                    url = apiBaseUrl + "/api/sessions/expire";
                    restTemplate.delete(url);
                    logger.debug("Expired old sessions");
                    break;
                default:
                    // Clean up old audit logs (>60 days) if endpoint exists
                    url = apiBaseUrl + "/api/audit/cleanup?daysToKeep=60";
                    restTemplate.delete(url);
                    logger.debug("Cleaned up old audit logs (>60 days)");
                    break;
            }

        } catch (Exception e) {
            // Don't log error heavily - cleanup failures shouldn't break the flow
            logger.debug("Cleanup operation encountered error: {}", e.getMessage());
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
