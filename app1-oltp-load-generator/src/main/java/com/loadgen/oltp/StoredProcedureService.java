package com.loadgen.oltp;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class StoredProcedureService {
    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureService.class);
    private final DatabaseManager dbManager;

    public StoredProcedureService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public Map<String, Object> createOrderWithItems(long customerId, int numItems) {
        String sql = "{call proc_create_order_with_items(?, ?, ?, ?)}";

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setLong(1, customerId);
            cstmt.setInt(2, numItems);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.registerOutParameter(4, Types.NUMERIC);

            cstmt.execute();

            long orderId = cstmt.getLong(3);
            double total = cstmt.getDouble(4);

            logger.debug("proc_create_order_with_items: orderId={}, total={}", orderId, total);
            NewRelic.addCustomParameter("proc.orderId", orderId);
            NewRelic.addCustomParameter("proc.orderTotal", total);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("total", total);
            result.put("customerId", customerId);
            result.put("numItems", numItems);
            return result;

        } catch (SQLException e) {
            logger.error("Error calling proc_create_order_with_items", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public Map<String, Object> restockLowInventory(int restockQuantity) {
        String sql = "{call proc_restock_low_inventory(?, ?)}";

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, restockQuantity);
            cstmt.registerOutParameter(2, Types.NUMERIC);

            cstmt.execute();

            int itemsRestocked = cstmt.getInt(2);

            logger.debug("proc_restock_low_inventory: restocked={}", itemsRestocked);
            NewRelic.addCustomParameter("proc.itemsRestocked", itemsRestocked);

            Map<String, Object> result = new HashMap<>();
            result.put("itemsRestocked", itemsRestocked);
            result.put("restockQuantity", restockQuantity);
            return result;

        } catch (SQLException e) {
            logger.error("Error calling proc_restock_low_inventory", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public Map<String, Object> calculateCustomerLoyalty(long customerId) {
        String sql = "{call proc_calculate_customer_loyalty(?, ?, ?)}";

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setLong(1, customerId);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.VARCHAR);

            cstmt.execute();

            int newPoints = cstmt.getInt(2);
            String tier = cstmt.getString(3);

            logger.debug("proc_calculate_customer_loyalty: customerId={}, points={}, tier={}", customerId, newPoints, tier);
            NewRelic.addCustomParameter("proc.loyaltyPoints", newPoints);
            NewRelic.addCustomParameter("proc.loyaltyTier", tier);

            Map<String, Object> result = new HashMap<>();
            result.put("customerId", customerId);
            result.put("newPoints", newPoints);
            result.put("tier", tier);
            return result;

        } catch (SQLException e) {
            logger.error("Error calling proc_calculate_customer_loyalty", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public Map<String, Object> purgeOldData(int daysToKeep) {
        String sql = "{call proc_purge_old_data(?, ?, ?, ?)}";

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, daysToKeep);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.registerOutParameter(4, Types.NUMERIC);

            cstmt.execute();

            int ordersDeleted = cstmt.getInt(2);
            int itemsDeleted = cstmt.getInt(3);
            int logsDeleted = cstmt.getInt(4);

            logger.debug("proc_purge_old_data: orders={}, items={}, logs={}", ordersDeleted, itemsDeleted, logsDeleted);
            NewRelic.addCustomParameter("proc.ordersDeleted", ordersDeleted);
            NewRelic.addCustomParameter("proc.itemsDeleted", itemsDeleted);

            Map<String, Object> result = new HashMap<>();
            result.put("ordersDeleted", ordersDeleted);
            result.put("itemsDeleted", itemsDeleted);
            result.put("logsDeleted", logsDeleted);
            result.put("daysToKeep", daysToKeep);
            return result;

        } catch (SQLException e) {
            logger.error("Error calling proc_purge_old_data", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public Map<String, Object> dailySalesSummary() {
        String sql = "{call proc_daily_sales_summary(?, ?, ?)}";

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);

            cstmt.execute();

            int totalOrders = cstmt.getInt(2);
            double totalRevenue = cstmt.getDouble(3);

            logger.debug("proc_daily_sales_summary: orders={}, revenue={}", totalOrders, totalRevenue);
            NewRelic.addCustomParameter("proc.summaryOrders", totalOrders);
            NewRelic.addCustomParameter("proc.summaryRevenue", totalRevenue);

            Map<String, Object> result = new HashMap<>();
            result.put("totalOrders", totalOrders);
            result.put("totalRevenue", totalRevenue);
            return result;

        } catch (SQLException e) {
            logger.error("Error calling proc_daily_sales_summary", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }
}
