package com.loadgen.oltp;

import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * Inventory Service - Handles inventory-related operations
 */
@Service
public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private final DatabaseManager dbManager;
    private final Random random = new Random();

    private static final String[] WAREHOUSES = {"WH-0", "WH-1", "WH-2", "WH-3", "WH-4"};

    public InventoryService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public int checkAvailability(long productId) {
        String sql = "SELECT SUM(quantity_available - quantity_reserved) as available FROM INVENTORY WHERE product_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int available = rs.getInt("available");
                    logger.debug("Product {} availability: {}", productId, available);
                    return available;
                }
            }

        } catch (SQLException e) {
            logger.error("Error checking availability for product {}", productId, e);
        }

        return 0;
    }

    @Trace
    public void restockInventory(long productId, int quantity) {
        String sql = "UPDATE INVENTORY SET " +
                     "quantity_available = quantity_available + ?, " +
                     "last_restock_date = CURRENT_TIMESTAMP, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantity);
            pstmt.setLong(2, productId);

            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                logger.debug("Restocked product {}: +{} units", productId, quantity);
            }

        } catch (SQLException e) {
            logger.error("Error restocking product {}", productId, e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public void updateWarehouseLocation(long productId) {
        String newLocation = WAREHOUSES[random.nextInt(WAREHOUSES.length)];
        String sql = "UPDATE INVENTORY SET warehouse_location = ?, updated_at = CURRENT_TIMESTAMP WHERE product_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newLocation);
            pstmt.setLong(2, productId);

            pstmt.executeUpdate();
            logger.debug("Updated warehouse location for product {} to {}", productId, newLocation);

        } catch (SQLException e) {
            logger.error("Error updating warehouse location for product {}", productId, e);
        }
    }

    @Trace
    public void reserveInventory(long orderId) {
        String sql = "UPDATE INVENTORY SET " +
                     "quantity_reserved = quantity_reserved + (" +
                     "  SELECT oi.quantity FROM ORDER_ITEMS oi WHERE oi.order_id = ? AND oi.product_id = INVENTORY.product_id" +
                     "), updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id IN (" +
                     "  SELECT product_id FROM ORDER_ITEMS WHERE order_id = ?" +
                     ")";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, orderId);
            pstmt.setLong(2, orderId);

            int updated = pstmt.executeUpdate();
            logger.debug("Reserved inventory for order {}: {} products updated", orderId, updated);

        } catch (SQLException e) {
            logger.error("Error reserving inventory for order {}", orderId, e);
            // Don't throw - inventory reservation failure should not break order creation
        }
    }

    @Trace
    public void releaseInventory(long orderId) {
        String sql = "UPDATE INVENTORY SET " +
                     "quantity_reserved = GREATEST(0, quantity_reserved - (" +
                     "  SELECT oi.quantity FROM ORDER_ITEMS oi WHERE oi.order_id = ? AND oi.product_id = INVENTORY.product_id" +
                     ")), updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id IN (" +
                     "  SELECT product_id FROM ORDER_ITEMS WHERE order_id = ?" +
                     ")";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, orderId);
            pstmt.setLong(2, orderId);

            int updated = pstmt.executeUpdate();
            logger.debug("Released inventory for order {}: {} products updated", orderId, updated);

        } catch (SQLException e) {
            logger.error("Error releasing inventory for order {}", orderId, e);
        }
    }

    @Trace
    public void adjustInventory(long productId, int adjustment) {
        String sql = "UPDATE INVENTORY SET " +
                     "quantity_available = GREATEST(0, quantity_available + ?), " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, adjustment);
            pstmt.setLong(2, productId);

            pstmt.executeUpdate();
            logger.debug("Adjusted inventory for product {}: {}", productId, adjustment);

        } catch (SQLException e) {
            logger.error("Error adjusting inventory for product {}", productId, e);
        }
    }

    @Trace
    public void bulkUpdateInventory() {
        // Randomly adjust inventory for multiple products
        Random random = new Random();
        int updateCount = random.nextInt(20) + 10; // Update 10-30 products

        String sql = "UPDATE INVENTORY SET " +
                     "quantity_available = quantity_available + ?, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < updateCount; i++) {
                long productId = random.nextInt(500) + 1;
                int adjustment = random.nextInt(100) - 50; // Random adjustment between -50 and +50

                pstmt.setInt(1, adjustment);
                pstmt.setLong(2, productId);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            logger.debug("Bulk updated inventory for {} products", results.length);

        } catch (SQLException e) {
            logger.error("Error in bulk inventory update", e);
        }
    }
}
