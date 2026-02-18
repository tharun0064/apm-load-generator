package com.loadgen.analytics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Analytics Load Generator - Simulates heavy analytical queries
 * This application generates massive analytical load with:
 * - Complex multi-table joins
 * - Aggregations and grouping
 * - Large data scans
 * - Reporting queries
 * - Data warehouse style operations
 */
public class AnalyticsLoadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsLoadGenerator.class);
    private static final Random random = new Random();

    private final DatabaseManager dbManager;
    private final SalesAnalyticsService salesAnalytics;
    private final CustomerAnalyticsService customerAnalytics;
    private final ProductAnalyticsService productAnalytics;
    private final ReportingService reportingService;
    private final DataWarehouseService dataWarehouseService;

    private final int numThreads;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public AnalyticsLoadGenerator(int numThreads) {
        this.numThreads = numThreads;
        this.executorService = Executors.newFixedThreadPool(numThreads);

        this.dbManager = new DatabaseManager();
        this.salesAnalytics = new SalesAnalyticsService(dbManager);
        this.customerAnalytics = new CustomerAnalyticsService(dbManager);
        this.productAnalytics = new ProductAnalyticsService(dbManager);
        this.reportingService = new ReportingService(dbManager);
        this.dataWarehouseService = new DataWarehouseService(dbManager);

        logger.info("Analytics Load Generator initialized with {} threads", numThreads);
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

        while (running) {
            try {
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

                // SHORT delay for HEAVY load - analytics queries will overlap and stress DB!
                Thread.sleep(random.nextInt(50) + 10); // 10-60ms delay = HEAVY CONCURRENT LOAD

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Analytics worker thread {} interrupted", threadId);
                break;
            } catch (Exception e) {
                logger.error("Error in analytics worker thread {}: {}", threadId, e.getMessage());
                NewRelic.noticeError(e);
                // Continue running even on errors
            }
        }

        logger.info("Analytics worker thread {} completed {} queries", threadId, queryCount);
    }

    @Trace
    private void salesAnalyticsWorkflow() {
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "SalesAnalytics");

        try {
            // Run various sales analytics queries
            int choice = random.nextInt(5);

            switch (choice) {
                case 0:
                    salesAnalytics.getDailySalesSummary();
                    break;
                case 1:
                    salesAnalytics.getMonthlySalesTrend();
                    break;
                case 2:
                    salesAnalytics.getSalesByCategory();
                    break;
                case 3:
                    salesAnalytics.getTopSellingProducts(20);
                    break;
                case 4:
                    salesAnalytics.getRevenueByPaymentMethod();
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in salesAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void customerAnalyticsWorkflow() {
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "CustomerAnalytics");

        try {
            int choice = random.nextInt(5);

            switch (choice) {
                case 0:
                    customerAnalytics.getCustomerSegmentation();
                    break;
                case 1:
                    customerAnalytics.getCustomerLifetimeValue();
                    break;
                case 2:
                    customerAnalytics.getCustomerRetentionRate();
                    break;
                case 3:
                    customerAnalytics.getHighValueCustomers(50);
                    break;
                case 4:
                    customerAnalytics.getCustomerPurchaseFrequency();
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in customerAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void productAnalyticsWorkflow() {
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "ProductAnalytics");

        try {
            int choice = random.nextInt(5);

            switch (choice) {
                case 0:
                    productAnalytics.getProductPerformanceReport();
                    break;
                case 1:
                    productAnalytics.getInventoryTurnoverRate();
                    break;
                case 2:
                    productAnalytics.getSlowMovingProducts();
                    break;
                case 3:
                    productAnalytics.getProfitMarginByCategory();
                    break;
                case 4:
                    productAnalytics.getProductAffinityAnalysis();
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in productAnalyticsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void reportingWorkflow() {
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "Reporting");

        try {
            int choice = random.nextInt(4);

            switch (choice) {
                case 0:
                    reportingService.generateExecutiveDashboard();
                    break;
                case 1:
                    reportingService.generateSalesReport();
                    break;
                case 2:
                    reportingService.generateInventoryReport();
                    break;
                case 3:
                    reportingService.generateCustomerReport();
                    break;
            }

        } catch (Exception e) {
            logger.error("Error in reportingWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void dataWarehouseWorkflow() {
        NewRelic.setTransactionName("AnalyticsLoadGenerator", "DataWarehouse");

        try {
            int choice = random.nextInt(5);

            switch (choice) {
                case 0:
                    dataWarehouseService.aggregateSalesData();
                    break;
                case 1:
                    dataWarehouseService.aggregateCustomerData();
                    break;
                case 2:
                    dataWarehouseService.aggregateProductData();
                    break;
                case 3:
                    // VERY HEAVY - Full table scan
                    dataWarehouseService.performFullTableScan();
                    break;
                case 4:
                    // VERY HEAVY - Complex 5-table join
                    dataWarehouseService.performComplexJoinQuery();
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
        dbManager.close();
        logger.info("Analytics Load Generator shutdown complete");
    }

    public static void main(String[] args) {
        // Configure thread count from environment or default to 20 (fewer than OLTP)
        int numThreads = Integer.parseInt(System.getProperty("threads", "20"));

        logger.info("=".repeat(80));
        logger.info("Analytics Load Generator Starting");
        logger.info("Threads: {}", numThreads);
        logger.info("=".repeat(80));

        AnalyticsLoadGenerator generator = new AnalyticsLoadGenerator(numThreads);
        generator.start();
    }
}
