package com.loadgen.oltp;

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
 * OLTP Load Generator - Simulates high-frequency transactional operations via REST APIs
 * This application generates massive load by making HTTP calls to REST endpoints:
 * - Customer creation and updates
 * - Order processing
 * - Inventory management
 * - Transaction logging
 * - Session tracking
 */
@Component
public class OltpLoadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(OltpLoadGenerator.class);
    private static final Random random = new Random();

    private final RestTemplate restTemplate;
    private final DatabaseManager dbManager;
    private final int numThreads;
    private final String apiBaseUrl;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public OltpLoadGenerator(RestTemplate restTemplate,
                            DatabaseManager dbManager,
                            @Value("${threads:25}") int numThreads,
                            @Value("${api.base.url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.dbManager = dbManager;
        this.numThreads = numThreads;
        this.apiBaseUrl = apiBaseUrl;
        this.executorService = Executors.newFixedThreadPool(numThreads);

        logger.info("OLTP Load Generator initialized with {} threads", numThreads);
        logger.info("API Base URL: {}", apiBaseUrl);
        NewRelic.setTransactionName("OltpLoadGenerator", "Initialize");
    }

    @Trace(dispatcher = true)
    public void start() {
        logger.info("Starting OLTP load generation...");
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

        // Keep main thread alive
        try {
            while (running) {
                Thread.sleep(10000);
                logStatistics();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Main thread interrupted", e);
        }
    }

    @Trace(dispatcher = true)
    private void runWorker(int threadId) {
        logger.info("Worker thread {} started", threadId);
        NewRelic.setTransactionName("OltpLoadGenerator", "Worker");
        NewRelic.addCustomParameter("threadId", threadId);

        int operationCount = 0;
        long lastBreakTime = System.currentTimeMillis();
        int cycleOperations = 0;
        int consecutiveErrors = 0;

        while (running) {
            try {
                // Check if it's time for a 10-second break (after 30-60 seconds of work)
                long currentTime = System.currentTimeMillis();
                long timeSinceBreak = currentTime - lastBreakTime;

                // Take a break every 30-60 seconds (randomized per thread to avoid all threads breaking at once)
                int breakInterval = 30000 + random.nextInt(30000); // 30-60 seconds
                if (timeSinceBreak > breakInterval) {
                    logger.info("Worker thread {} taking 10-second break after {} operations", threadId, cycleOperations);
                    Thread.sleep(10000); // 10 second break
                    lastBreakTime = System.currentTimeMillis();
                    cycleOperations = 0;
                    logger.info("Worker thread {} resuming work", threadId);
                }

                // Randomly select operation type with weighted distribution
                int operation = random.nextInt(100);

                if (operation < 25) {
                    // 25% - Create new orders (heavy inserts)
                    createOrderWorkflow();
                } else if (operation < 45) {
                    // 20% - Update customer information (heavy updates)
                    updateCustomerWorkflow();
                } else if (operation < 60) {
                    // 15% - Check and update inventory (updates)
                    inventoryCheckWorkflow();
                } else if (operation < 75) {
                    // 15% - Process transactions (inserts + updates)
                    processTransactionWorkflow();
                } else if (operation < 85) {
                    // 10% - Delete old data (aggressive cleanup)
                    deleteOldDataWorkflow();
                } else if (operation < 92) {
                    // 7% - Bulk insert operations
                    bulkInsertWorkflow();
                } else if (operation < 97) {
                    // 5% - Session management
                    sessionManagementWorkflow();
                } else {
                    // 3% - Product operations
                    productOperationsWorkflow();
                }

                operationCount++;
                cycleOperations++;
                consecutiveErrors = 0; // Reset error counter on success

                // MINIMAL delay for MAXIMUM load - only 1-5ms!
                Thread.sleep(random.nextInt(5) + 1); // 1-5ms delay = VERY HIGH LOAD

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Worker thread {} interrupted", threadId);
                break;
            } catch (Exception e) {
                consecutiveErrors++;
                logger.error("Error in worker thread {} (consecutive errors: {}): {}", threadId, consecutiveErrors, e.getMessage());
                NewRelic.noticeError(e);

                // Exponential backoff: 1s, 2s, 4s, 8s (max 8s)
                int backoffMs = Math.min(1000 * (1 << (consecutiveErrors - 1)), 8000);
                logger.warn("Worker thread {} backing off for {}ms after {} consecutive errors", threadId, backoffMs, consecutiveErrors);

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Worker thread {} completed {} operations", threadId, operationCount);
    }

    @Trace
    private void createOrderWorkflow() {
        try {
            // Get random customer
            long customerId = random.nextInt(1000) + 1;
            int numItems = random.nextInt(5) + 1;

            // Call REST API to create order
            String url = apiBaseUrl + "/api/orders/create?customerId=" + customerId + "&numItems=" + numItems;
            restTemplate.postForObject(url, null, String.class);

        } catch (Exception e) {
            logger.error("Error in createOrderWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void updateCustomerWorkflow() {
        try {
            long customerId = random.nextInt(1000) + 1;

            // Update customer loyalty points via REST API
            int points = random.nextInt(100);
            String url = apiBaseUrl + "/api/customers/" + customerId + "/loyalty?points=" + points;
            restTemplate.put(url, null);

            // Upgrade customer type randomly
            if (random.nextInt(10) == 0) {
                String upgradeUrl = apiBaseUrl + "/api/customers/" + customerId + "/upgrade";
                restTemplate.put(upgradeUrl, null);
            }

            // Log customer access
            String logUrl = apiBaseUrl + "/api/customers/" + customerId + "/access-log";
            restTemplate.postForObject(logUrl, null, String.class);

        } catch (Exception e) {
            logger.error("Error in updateCustomerWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void inventoryCheckWorkflow() {
        try {
            long productId = random.nextInt(500) + 1;

            // Check inventory via REST API (also auto-restocks if low)
            String url = apiBaseUrl + "/api/inventory/" + productId + "/check";
            restTemplate.getForObject(url, String.class);

        } catch (Exception e) {
            logger.error("Error in inventoryCheckWorkflow", e);
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
    private void sessionManagementWorkflow() {
        try {
            long customerId = random.nextInt(1000) + 1;

            // Create session via REST API
            String url = apiBaseUrl + "/api/sessions/create?customerId=" + customerId;
            restTemplate.postForObject(url, null, String.class);

            // Randomly expire old sessions
            if (random.nextInt(10) == 0) {
                String expireUrl = apiBaseUrl + "/api/sessions/expire";
                restTemplate.delete(expireUrl);
            }

        } catch (Exception e) {
            logger.error("Error in sessionManagementWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void productOperationsWorkflow() {
        try {
            long productId = random.nextInt(500) + 1;

            // Update product price via REST API
            String updateUrl = apiBaseUrl + "/api/products/" + productId + "/price";
            restTemplate.put(updateUrl, null);

            // Query product details
            String getUrl = apiBaseUrl + "/api/products/" + productId;
            restTemplate.getForObject(getUrl, String.class);

            // Search products by category
            String searchUrl = apiBaseUrl + "/api/products/search";
            restTemplate.getForObject(searchUrl, String.class);

        } catch (Exception e) {
            logger.error("Error in productOperationsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void deleteOldDataWorkflow() {
        try {
            // Delete old orders via REST API
            String url = apiBaseUrl + "/api/orders/old?daysToKeep=30";
            restTemplate.delete(url);

        } catch (Exception e) {
            logger.error("Error in deleteOldDataWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void bulkInsertWorkflow() {
        try {
            // Bulk create multiple orders at once via REST API
            int batchSize = random.nextInt(5) + 3; // 3-7 orders at once
            String url = apiBaseUrl + "/api/orders/bulk?batchSize=" + batchSize;
            restTemplate.postForObject(url, null, String.class);

        } catch (Exception e) {
            logger.error("Error in bulkInsertWorkflow", e);
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
        logger.info("OLTP Load Generator shutdown complete");
    }
}
