package com.loadgen.analytics;

import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Warehouse Service - ETL and aggregation operations
 */
@Service
public class DataWarehouseService {
    private static final Logger logger = LoggerFactory.getLogger(DataWarehouseService.class);
    private final DatabaseManager dbManager;

    public DataWarehouseService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void aggregateSalesData() {
        // Aggregate sales data and insert into analytics tables
        String sql = "MERGE INTO analytics_user.SALES_SUMMARY ss " +
                     "USING ( " +
                     "  SELECT " +
                     "    TRUNC(o.order_date) as summary_date, " +
                     "    COUNT(DISTINCT o.order_id) as total_orders, " +
                     "    SUM(o.total_amount) as total_revenue, " +
                     "    COUNT(DISTINCT o.customer_id) as total_customers, " +
                     "    AVG(o.total_amount) as avg_order_value " +
                     "  FROM oltp_user.ORDERS o " +
                     "  WHERE o.order_date >= TRUNC(SYSDATE) - 7 " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  GROUP BY TRUNC(o.order_date) " +
                     ") src " +
                     "ON (ss.summary_date = src.summary_date) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    ss.total_orders = src.total_orders, " +
                     "    ss.total_revenue = src.total_revenue, " +
                     "    ss.total_customers = src.total_customers, " +
                     "    ss.avg_order_value = src.avg_order_value, " +
                     "    ss.created_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (summary_id, summary_date, total_orders, total_revenue, total_customers, avg_order_value) " +
                     "  VALUES (sales_summary_seq.NEXTVAL, src.summary_date, src.total_orders, src.total_revenue, src.total_customers, src.avg_order_value)";

        executeDataWarehouseOperation(sql, "AggregateSalesData");
    }

    @Trace
    public void aggregateCustomerData() {
        // Aggregate customer analytics
        String sql = "MERGE INTO analytics_user.CUSTOMER_ANALYTICS ca " +
                     "USING ( " +
                     "  SELECT " +
                     "    c.customer_id, " +
                     "    COUNT(o.order_id) as total_orders, " +
                     "    COALESCE(SUM(o.total_amount), 0) as total_spent, " +
                     "    COALESCE(AVG(o.total_amount), 0) as avg_order_value, " +
                     "    MAX(o.order_date) as last_order_date, " +
                     "    CASE " +
                     "      WHEN COUNT(o.order_id) >= 10 AND SUM(o.total_amount) >= 1000 THEN 'VIP' " +
                     "      WHEN COUNT(o.order_id) >= 5 AND SUM(o.total_amount) >= 500 THEN 'Loyal' " +
                     "      WHEN COUNT(o.order_id) >= 2 THEN 'Regular' " +
                     "      ELSE 'New' " +
                     "    END as customer_segment " +
                     "  FROM oltp_user.CUSTOMERS c " +
                     "  LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  GROUP BY c.customer_id " +
                     ") src " +
                     "ON (ca.customer_id = src.customer_id) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    ca.total_orders = src.total_orders, " +
                     "    ca.total_spent = src.total_spent, " +
                     "    ca.avg_order_value = src.avg_order_value, " +
                     "    ca.last_order_date = src.last_order_date, " +
                     "    ca.customer_segment = src.customer_segment, " +
                     "    ca.calculated_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (analytics_id, customer_id, total_orders, total_spent, avg_order_value, last_order_date, customer_segment) " +
                     "  VALUES (customer_analytics_seq.NEXTVAL, src.customer_id, src.total_orders, src.total_spent, src.avg_order_value, src.last_order_date, src.customer_segment)";

        executeDataWarehouseOperation(sql, "AggregateCustomerData");
    }

    @Trace
    public void aggregateProductData() {
        // Aggregate product performance data
        String sql = "MERGE INTO analytics_user.PRODUCT_PERFORMANCE pp " +
                     "USING ( " +
                     "  SELECT " +
                     "    p.product_id, " +
                     "    TRUNC(SYSDATE, 'MM') as period_start, " +
                     "    LAST_DAY(SYSDATE) as period_end, " +
                     "    COALESCE(SUM(oi.quantity), 0) as units_sold, " +
                     "    COALESCE(SUM(oi.subtotal), 0) as revenue, " +
                     "    COALESCE(SUM(oi.subtotal) - SUM(oi.quantity * p.cost), 0) as profit, " +
                     "    0 as return_count " +
                     "  FROM oltp_user.PRODUCTS p " +
                     "  LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "  LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "    AND o.order_date >= TRUNC(SYSDATE, 'MM') " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  WHERE p.is_active = 1 " +
                     "  GROUP BY p.product_id, p.cost " +
                     ") src " +
                     "ON (pp.product_id = src.product_id AND pp.period_start = src.period_start) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    pp.units_sold = src.units_sold, " +
                     "    pp.revenue = src.revenue, " +
                     "    pp.profit = src.profit, " +
                     "    pp.calculated_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (performance_id, product_id, period_start, period_end, units_sold, revenue, profit, return_count) " +
                     "  VALUES (product_perf_seq.NEXTVAL, src.product_id, src.period_start, src.period_end, src.units_sold, src.revenue, src.profit, src.return_count)";

        executeDataWarehouseOperation(sql, "AggregateProductData");
    }

    @Trace
    public void performFullTableScan() {
        // Intentionally heavy query to stress the database
        String sql = "SELECT " +
                     "  o.order_id, " +
                     "  o.customer_id, " +
                     "  o.order_date, " +
                     "  o.status, " +
                     "  o.total_amount, " +
                     "  c.first_name, " +
                     "  c.last_name, " +
                     "  c.email, " +
                     "  COUNT(oi.order_item_id) as item_count " +
                     "FROM oltp_user.ORDERS o " +
                     "JOIN oltp_user.CUSTOMERS c ON o.customer_id = c.customer_id " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "GROUP BY o.order_id, o.customer_id, o.order_date, o.status, o.total_amount, c.first_name, c.last_name, c.email " +
                     "ORDER BY o.order_date DESC";

        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                if (rowCount >= 1000) break; // Limit to prevent excessive processing
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Full table scan returned {} rows in {}ms", rowCount, duration);

        } catch (SQLException e) {
            logger.error("Error performing full table scan", e);
            throw new RuntimeException(e);
        }
    }

    @Trace
    public void performComplexJoinQuery() {
        // Very heavy 5-table join
        String sql = "SELECT " +
                     "  c.customer_id, " +
                     "  c.first_name || ' ' || c.last_name as customer_name, " +
                     "  c.customer_type, " +
                     "  COUNT(DISTINCT o.order_id) as order_count, " +
                     "  COUNT(DISTINCT oi.product_id) as unique_products, " +
                     "  SUM(oi.quantity) as total_items, " +
                     "  SUM(oi.subtotal) as total_spent, " +
                     "  COUNT(DISTINCT t.transaction_id) as transaction_count, " +
                     "  SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_transactions, " +
                     "  COUNT(DISTINCT p.category) as categories_purchased, " +
                     "  AVG(inv.quantity_available) as avg_inventory_level " +
                     "FROM oltp_user.CUSTOMERS c " +
                     "LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "LEFT JOIN oltp_user.TRANSACTIONS t ON o.order_id = t.order_id " +
                     "LEFT JOIN oltp_user.PRODUCTS p ON oi.product_id = p.product_id " +
                     "LEFT JOIN oltp_user.INVENTORY inv ON p.product_id = inv.product_id " +
                     "WHERE o.order_date >= SYSDATE - 90 " +
                     "GROUP BY c.customer_id, c.first_name, c.last_name, c.customer_type " +
                     "HAVING COUNT(DISTINCT o.order_id) > 0 " +
                     "ORDER BY total_spent DESC " +
                     "FETCH FIRST 100 ROWS ONLY";

        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Complex join query returned {} rows in {}ms", rowCount, duration);

        } catch (SQLException e) {
            logger.error("Error performing complex join query", e);
            throw new RuntimeException(e);
        }
    }

    private void executeDataWarehouseOperation(String sql, String operationName) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int affectedRows = pstmt.executeUpdate();
            long duration = System.currentTimeMillis() - startTime;

            logger.debug("{} affected {} rows in {}ms", operationName, affectedRows, duration);

        } catch (SQLException e) {
            logger.error("Error executing {} operation", operationName, e);
            throw new RuntimeException(e);
        }
    }
}
