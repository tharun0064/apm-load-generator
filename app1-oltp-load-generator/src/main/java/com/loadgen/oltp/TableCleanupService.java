package com.loadgen.oltp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service to clean up (truncate) all tables before starting load generation
 */
@Service
public class TableCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(TableCleanupService.class);
    private final DatabaseManager dbManager;

    // Tables to truncate in order (respecting foreign key constraints)
    private static final String[] TABLES_TO_TRUNCATE = {
        "ORDER_ITEMS",      // FK to ORDERS
        "TRANSACTIONS",     // FK to ORDERS
        "ORDERS",           // FK to CUSTOMERS
        "SESSION_DATA",     // FK to CUSTOMERS (possibly)
        "AUDIT_LOG",        // No FK
        "INVENTORY",        // FK to PRODUCTS (possibly)
        "CUSTOMERS",        // Referenced by ORDERS, SESSION_DATA
        "PRODUCTS"          // Referenced by ORDER_ITEMS, INVENTORY
    };

    public TableCleanupService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Truncate all tables and repopulate seed data
     */
    public void truncateAndRebuild() {
        truncateAllTables();
        repopulateSeedData();
    }

    /**
     * Truncate all tables to start with a clean slate
     */
    public void truncateAllTables() {
        logger.info("=" .repeat(80));
        logger.info("Starting table cleanup - truncating all tables...");
        logger.info("=" .repeat(80));

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Disable foreign key constraints temporarily (Oracle syntax)
            try {
                stmt.execute("BEGIN FOR c IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R') LOOP EXECUTE IMMEDIATE 'ALTER TABLE ' || c.table_name || ' DISABLE CONSTRAINT ' || c.constraint_name; END LOOP; END;");
                logger.info("Foreign key constraints disabled");
            } catch (SQLException e) {
                logger.warn("Could not disable foreign key constraints: {}", e.getMessage());
                // Continue anyway, truncate might work without this
            }

            // Truncate each table
            int successCount = 0;
            for (String table : TABLES_TO_TRUNCATE) {
                try {
                    stmt.execute("TRUNCATE TABLE " + table);
                    logger.info("✓ Truncated table: {}", table);
                    successCount++;
                } catch (SQLException e) {
                    logger.error("✗ Failed to truncate table {}: {}", table, e.getMessage());
                }
            }

            // Re-enable foreign key constraints (Oracle syntax)
            try {
                stmt.execute("BEGIN FOR c IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R') LOOP EXECUTE IMMEDIATE 'ALTER TABLE ' || c.table_name || ' ENABLE CONSTRAINT ' || c.constraint_name; END LOOP; END;");
                logger.info("Foreign key constraints re-enabled");
            } catch (SQLException e) {
                logger.warn("Could not re-enable foreign key constraints: {}", e.getMessage());
            }

            logger.info("=" .repeat(80));
            logger.info("Table cleanup completed: {}/{} tables truncated successfully", successCount, TABLES_TO_TRUNCATE.length);
            logger.info("=" .repeat(80));

        } catch (SQLException e) {
            logger.error("Error during table cleanup", e);
            throw new RuntimeException("Failed to clean up tables", e);
        }
    }

    /**
     * Repopulate seed data (1000 customers, 500 products, inventory)
     */
    public void repopulateSeedData() {
        logger.info("=" .repeat(80));
        logger.info("Repopulating seed data...");
        logger.info("=" .repeat(80));

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insert 1000 customers
            logger.info("Inserting 1000 customers...");
            String customerInsert =
                "BEGIN " +
                "  FOR i IN 1..1000 LOOP " +
                "    INSERT INTO CUSTOMERS (customer_id, first_name, last_name, email, phone, city, state, country, customer_type, loyalty_points) " +
                "    VALUES (i, 'FirstName' || i, 'LastName' || i, 'customer' || i || '@example.com', '555-' || LPAD(i, 7, '0'), " +
                "            'City' || MOD(i, 50), 'State' || MOD(i, 50), 'USA', " +
                "            CASE WHEN MOD(i, 10) = 0 THEN 'PREMIUM' ELSE 'REGULAR' END, MOD(i * 100, 10000)); " +
                "  END LOOP; " +
                "  COMMIT; " +
                "END;";
            stmt.execute(customerInsert);
            logger.info("✓ Inserted 1000 customers");

            // Insert 500 products
            logger.info("Inserting 500 products...");
            String productInsert =
                "BEGIN " +
                "  FOR i IN 1..500 LOOP " +
                "    INSERT INTO PRODUCTS (product_id, product_name, category, subcategory, price, cost, sku, is_active) " +
                "    VALUES (i, 'Product ' || i, 'Category' || MOD(i, 10), 'SubCat' || MOD(i, 20), " +
                "            19.99 + (i * 1.5), 10.00 + (i * 0.75), 'SKU-' || LPAD(i, 8, '0'), 1); " +
                "  END LOOP; " +
                "  COMMIT; " +
                "END;";
            stmt.execute(productInsert);
            logger.info("✓ Inserted 500 products");

            // Insert inventory for all products
            logger.info("Inserting 500 inventory records...");
            String inventoryInsert =
                "BEGIN " +
                "  FOR i IN 1..500 LOOP " +
                "    INSERT INTO INVENTORY (inventory_id, product_id, warehouse_location, quantity_available, reorder_level) " +
                "    VALUES (i, i, 'WH-' || MOD(i, 5), 1000 + MOD(i * 137, 5000), 100); " +
                "  END LOOP; " +
                "  COMMIT; " +
                "END;";
            stmt.execute(inventoryInsert);
            logger.info("✓ Inserted 500 inventory records");

            logger.info("=" .repeat(80));
            logger.info("Seed data repopulation completed successfully!");
            logger.info("=" .repeat(80));

        } catch (SQLException e) {
            logger.error("Error repopulating seed data", e);
            throw new RuntimeException("Failed to repopulate seed data", e);
        }
    }
}
