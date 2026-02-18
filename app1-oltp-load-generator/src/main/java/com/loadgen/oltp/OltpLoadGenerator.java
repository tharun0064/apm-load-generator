package com.loadgen.oltp;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * OLTP Load Generator - Simulates high-frequency transactional operations
 * This application generates massive load with various database operations:
 * - Customer creation and updates
 * - Order processing
 * - Inventory management
 * - Transaction logging
 * - Session tracking
 */
public class OltpLoadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(OltpLoadGenerator.class);
    private static final Random random = new Random();

    private final DatabaseManager dbManager;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final TransactionService transactionService;
    private final SessionService sessionService;

    private final int numThreads;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public OltpLoadGenerator(int numThreads) {
        this.numThreads = numThreads;
        this.executorService = Executors.newFixedThreadPool(numThreads);

        this.dbManager = new DatabaseManager();
        this.customerService = new CustomerService(dbManager);
        this.orderService = new OrderService(dbManager);
        this.productService = new ProductService(dbManager);
        this.inventoryService = new InventoryService(dbManager);
        this.transactionService = new TransactionService(dbManager);
        this.sessionService = new SessionService(dbManager);

        logger.info("OLTP Load Generator initialized with {} threads", numThreads);
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

        while (running) {
            try {
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

                // MINIMAL delay for MAXIMUM load - only 1-5ms!
                Thread.sleep(random.nextInt(5) + 1); // 1-5ms delay = VERY HIGH LOAD

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Worker thread {} interrupted", threadId);
                break;
            } catch (Exception e) {
                logger.error("Error in worker thread {}: {}", threadId, e.getMessage());
                NewRelic.noticeError(e);
                // Continue running even on errors
            }
        }

        logger.info("Worker thread {} completed {} operations", threadId, operationCount);
    }

    @Trace
    private void createOrderWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "CreateOrder");

        try {
            // 1. Get random customer
            long customerId = random.nextInt(1000) + 1;

            // 2. Create order
            long orderId = orderService.createOrder(customerId);
            NewRelic.addCustomParameter("orderId", orderId);

            // 3. Add 1-5 items to order
            int numItems = random.nextInt(5) + 1;
            for (int i = 0; i < numItems; i++) {
                long productId = random.nextInt(500) + 1;
                int quantity = random.nextInt(5) + 1;
                orderService.addOrderItem(orderId, productId, quantity);
            }

            // 4. Update order total
            orderService.calculateOrderTotal(orderId);

            // 5. Create transaction record
            transactionService.createTransaction(orderId, "PAYMENT");

            // 6. Update inventory
            inventoryService.reserveInventory(orderId);

            // 7. Log audit
            orderService.logAudit("ORDERS", orderId, "CREATE");

        } catch (Exception e) {
            logger.error("Error in createOrderWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void updateCustomerWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "UpdateCustomer");

        try {
            long customerId = random.nextInt(1000) + 1;

            // Update customer loyalty points
            customerService.updateLoyaltyPoints(customerId, random.nextInt(100));

            // Update customer type randomly
            if (random.nextInt(10) == 0) {
                customerService.upgradeCustomerType(customerId);
            }

            // Log customer access
            customerService.logCustomerAccess(customerId);

        } catch (Exception e) {
            logger.error("Error in updateCustomerWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void inventoryCheckWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "InventoryCheck");

        try {
            long productId = random.nextInt(500) + 1;

            // Check inventory levels
            int available = inventoryService.checkAvailability(productId);

            // Restock if low
            if (available < 100) {
                inventoryService.restockInventory(productId, random.nextInt(500) + 100);
            }

            // Update inventory location
            inventoryService.updateWarehouseLocation(productId);

        } catch (Exception e) {
            logger.error("Error in inventoryCheckWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void processTransactionWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "ProcessTransaction");

        try {
            // Get random recent order
            long orderId = Math.max(1, orderService.getMaxOrderId() - random.nextInt(100));

            // Process payment
            boolean success = transactionService.processPayment(orderId);

            if (success) {
                // Update order status
                orderService.updateOrderStatus(orderId, "COMPLETED");
            } else {
                // Handle failed transaction
                orderService.updateOrderStatus(orderId, "PAYMENT_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error in processTransactionWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void sessionManagementWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "SessionManagement");

        try {
            long customerId = random.nextInt(1000) + 1;

            // Create or update session
            String sessionId = sessionService.createSession(customerId);

            // Update session activity
            sessionService.updateSessionActivity(sessionId);

            // Randomly expire old sessions
            if (random.nextInt(10) == 0) {
                sessionService.expireOldSessions();
            }

        } catch (Exception e) {
            logger.error("Error in sessionManagementWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void productOperationsWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "ProductOperations");

        try {
            long productId = random.nextInt(500) + 1;

            // Update product price
            productService.updatePrice(productId);

            // Query product details
            productService.getProductDetails(productId);

            // Search products by category
            productService.searchByCategory();

        } catch (Exception e) {
            logger.error("Error in productOperationsWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void deleteOldDataWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "DeleteOldData");

        try {
            // Delete old completed orders to prevent data buildup
            orderService.deleteOldCompletedOrders(30); // Keep last 30 days only

            // Delete expired sessions
            sessionService.deleteExpiredSessions();

            // Clean up old audit logs
            orderService.deleteOldAuditLogs(7); // Keep last 7 days

            // Delete cancelled/failed orders
            orderService.deleteCancelledOrders();

        } catch (Exception e) {
            logger.error("Error in deleteOldDataWorkflow", e);
            NewRelic.noticeError(e);
        }
    }

    @Trace
    private void bulkInsertWorkflow() {
        NewRelic.setTransactionName("OltpLoadGenerator", "BulkInsert");

        try {
            // Bulk create multiple orders at once
            int batchSize = random.nextInt(5) + 3; // 3-7 orders at once

            for (int i = 0; i < batchSize; i++) {
                long customerId = random.nextInt(1000) + 1;
                long orderId = orderService.createOrder(customerId);

                // Add items to each order
                int numItems = random.nextInt(3) + 1;
                for (int j = 0; j < numItems; j++) {
                    long productId = random.nextInt(500) + 1;
                    int quantity = random.nextInt(3) + 1;
                    orderService.addOrderItem(orderId, productId, quantity);
                }

                orderService.calculateOrderTotal(orderId);
            }

            // Bulk update inventory
            inventoryService.bulkUpdateInventory();

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
        dbManager.close();
        logger.info("OLTP Load Generator shutdown complete");
    }

    public static void main(String[] args) {
        // Configure thread count from environment or default to 50
        int numThreads = Integer.parseInt(System.getProperty("threads", "50"));

        logger.info("=".repeat(80));
        logger.info("OLTP Load Generator Starting");
        logger.info("Threads: {}", numThreads);
        logger.info("=".repeat(80));

        OltpLoadGenerator generator = new OltpLoadGenerator(numThreads);
        generator.start();
    }
}
