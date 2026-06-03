package com.loadgen.analytics;

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

    public void aggregateSalesData() {
        String sql = "MERGE analytics.SALES_SUMMARY AS ss " +
                     "USING ( " +
                     "  SELECT " +
                     "    CAST(o.order_date AS DATE) as summary_date, " +
                     "    COUNT(DISTINCT o.order_id) as total_orders, " +
                     "    SUM(o.total_amount) as total_revenue, " +
                     "    COUNT(DISTINCT o.customer_id) as total_customers, " +
                     "    AVG(o.total_amount) as avg_order_value " +
                     "  FROM oltp.ORDERS o " +
                     "  WHERE o.order_date >= DATEADD(day, -7, CAST(GETDATE() AS DATE)) " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  GROUP BY CAST(o.order_date AS DATE) " +
                     ") src ON (ss.summary_date = src.summary_date) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    ss.total_orders = src.total_orders, " +
                     "    ss.total_revenue = src.total_revenue, " +
                     "    ss.total_customers = src.total_customers, " +
                     "    ss.avg_order_value = src.avg_order_value, " +
                     "    ss.created_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (summary_date, total_orders, total_revenue, total_customers, avg_order_value) " +
                     "  VALUES (src.summary_date, src.total_orders, src.total_revenue, src.total_customers, src.avg_order_value);";

        executeDataWarehouseOperation(sql, "AggregateSalesData");
    }

    public void aggregateCustomerData() {
        String sql = "MERGE analytics.CUSTOMER_ANALYTICS AS ca " +
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
                     "  FROM oltp.CUSTOMERS c " +
                     "  LEFT JOIN oltp.ORDERS o ON c.customer_id = o.customer_id " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  GROUP BY c.customer_id " +
                     ") src ON (ca.customer_id = src.customer_id) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    ca.total_orders = src.total_orders, " +
                     "    ca.total_spent = src.total_spent, " +
                     "    ca.avg_order_value = src.avg_order_value, " +
                     "    ca.last_order_date = src.last_order_date, " +
                     "    ca.customer_segment = src.customer_segment, " +
                     "    ca.calculated_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (customer_id, total_orders, total_spent, avg_order_value, last_order_date, customer_segment) " +
                     "  VALUES (src.customer_id, src.total_orders, src.total_spent, src.avg_order_value, src.last_order_date, src.customer_segment);";

        executeDataWarehouseOperation(sql, "AggregateCustomerData");
    }

    public void aggregateProductData() {
        String sql = "MERGE analytics.PRODUCT_PERFORMANCE AS pp " +
                     "USING ( " +
                     "  SELECT " +
                     "    p.product_id, " +
                     "    DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1) as period_start, " +
                     "    EOMONTH(GETDATE()) as period_end, " +
                     "    COALESCE(SUM(oi.quantity), 0) as units_sold, " +
                     "    COALESCE(SUM(oi.subtotal), 0) as revenue, " +
                     "    COALESCE(SUM(oi.subtotal) - SUM(oi.quantity * p.cost), 0) as profit, " +
                     "    0 as return_count " +
                     "  FROM oltp.PRODUCTS p " +
                     "  LEFT JOIN oltp.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "  LEFT JOIN oltp.ORDERS o ON oi.order_id = o.order_id " +
                     "    AND o.order_date >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1) " +
                     "    AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  WHERE p.is_active = 1 " +
                     "  GROUP BY p.product_id, p.cost " +
                     ") src ON (pp.product_id = src.product_id AND pp.period_start = src.period_start) " +
                     "WHEN MATCHED THEN " +
                     "  UPDATE SET " +
                     "    pp.units_sold = src.units_sold, " +
                     "    pp.revenue = src.revenue, " +
                     "    pp.profit = src.profit, " +
                     "    pp.calculated_at = CURRENT_TIMESTAMP " +
                     "WHEN NOT MATCHED THEN " +
                     "  INSERT (product_id, period_start, period_end, units_sold, revenue, profit, return_count) " +
                     "  VALUES (src.product_id, src.period_start, src.period_end, src.units_sold, src.revenue, src.profit, src.return_count);";

        executeDataWarehouseOperation(sql, "AggregateProductData");
    }

    public void performFullTableScan() {
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
                     "FROM oltp.ORDERS o " +
                     "JOIN oltp.CUSTOMERS c ON o.customer_id = c.customer_id " +
                     "LEFT JOIN oltp.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "GROUP BY o.order_id, o.customer_id, o.order_date, o.status, o.total_amount, c.first_name, c.last_name, c.email " +
                     "ORDER BY o.order_date DESC";

        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                if (rowCount >= 1000) break;
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Full table scan returned {} rows in {}ms", rowCount, duration);

        } catch (SQLException e) {
            logger.error("Error performing full table scan", e);
            throw new RuntimeException(e);
        }
    }

    public void performComplexJoinQuery() {
        String sql = "SELECT TOP 100 " +
                     "  c.customer_id, " +
                     "  c.first_name + ' ' + c.last_name as customer_name, " +
                     "  c.customer_type, " +
                     "  COUNT(DISTINCT o.order_id) as order_count, " +
                     "  COUNT(DISTINCT oi.product_id) as unique_products, " +
                     "  SUM(oi.quantity) as total_items, " +
                     "  SUM(oi.subtotal) as total_spent, " +
                     "  COUNT(DISTINCT t.transaction_id) as transaction_count, " +
                     "  SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_transactions, " +
                     "  COUNT(DISTINCT p.category) as categories_purchased, " +
                     "  AVG(inv.quantity_available) as avg_inventory_level " +
                     "FROM oltp.CUSTOMERS c " +
                     "LEFT JOIN oltp.ORDERS o ON c.customer_id = o.customer_id " +
                     "LEFT JOIN oltp.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "LEFT JOIN oltp.TRANSACTIONS t ON o.order_id = t.order_id " +
                     "LEFT JOIN oltp.PRODUCTS p ON oi.product_id = p.product_id " +
                     "LEFT JOIN oltp.INVENTORY inv ON p.product_id = inv.product_id " +
                     "WHERE o.order_date >= DATEADD(day, -90, GETDATE()) " +
                     "GROUP BY c.customer_id, c.first_name, c.last_name, c.customer_type " +
                     "HAVING COUNT(DISTINCT o.order_id) > 0 " +
                     "ORDER BY total_spent DESC";

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
