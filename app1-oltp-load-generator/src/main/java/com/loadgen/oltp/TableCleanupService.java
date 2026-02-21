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
}
