package com.loadgen.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Sales Analytics Service - Heavy sales analysis queries
 */
@Service
public class SalesAnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(SalesAnalyticsService.class);
    private final DatabaseManager dbManager;

    public SalesAnalyticsService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void getDailySalesSummary() {
        String sql = "SELECT " +
                     "  CAST(o.order_date AS DATE) as order_day, " +
                     "  COUNT(DISTINCT o.order_id) as total_orders, " +
                     "  COUNT(DISTINCT o.customer_id) as unique_customers, " +
                     "  SUM(o.total_amount) as revenue, " +
                     "  AVG(o.total_amount) as avg_order_value, " +
                     "  SUM(oi.quantity) as total_items_sold " +
                     "FROM oltp.ORDERS o " +
                     "JOIN oltp.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "WHERE o.order_date >= DATEADD(day, -30, GETDATE()) " +
                     "GROUP BY CAST(o.order_date AS DATE) " +
                     "ORDER BY order_day DESC";

        executeAnalyticsQuery(sql, "DailySalesSummary");
    }

    public void getMonthlySalesTrend() {
        String sql = "SELECT " +
                     "  FORMAT(o.order_date, 'yyyy-MM') as month, " +
                     "  COUNT(o.order_id) as total_orders, " +
                     "  SUM(o.total_amount) as revenue, " +
                     "  AVG(o.total_amount) as avg_order_value, " +
                     "  COUNT(DISTINCT o.customer_id) as unique_customers, " +
                     "  SUM(o.total_amount) / COUNT(DISTINCT o.customer_id) as revenue_per_customer " +
                     "FROM oltp.ORDERS o " +
                     "WHERE o.order_date >= DATEADD(month, -12, GETDATE()) " +
                     "GROUP BY FORMAT(o.order_date, 'yyyy-MM') " +
                     "ORDER BY month DESC";

        executeAnalyticsQuery(sql, "MonthlySalesTrend");
    }

    public void getSalesByCategory() {
        String sql = "SELECT " +
                     "  p.category, " +
                     "  COUNT(DISTINCT oi.order_id) as orders, " +
                     "  SUM(oi.quantity) as units_sold, " +
                     "  SUM(oi.subtotal) as revenue, " +
                     "  AVG(oi.unit_price) as avg_price, " +
                     "  SUM(oi.subtotal) - SUM(oi.quantity * p.cost) as profit " +
                     "FROM oltp.ORDER_ITEMS oi " +
                     "JOIN oltp.PRODUCTS p ON oi.product_id = p.product_id " +
                     "JOIN oltp.ORDERS o ON oi.order_id = o.order_id " +
                     "WHERE o.order_date >= DATEADD(day, -30, GETDATE()) " +
                     "GROUP BY p.category " +
                     "ORDER BY revenue DESC";

        executeAnalyticsQuery(sql, "SalesByCategory");
    }

    public void getTopSellingProducts(int limit) {
        String sql = "SELECT TOP " + limit + " " +
                     "  p.product_id, " +
                     "  p.product_name, " +
                     "  p.category, " +
                     "  COUNT(DISTINCT oi.order_id) as order_count, " +
                     "  SUM(oi.quantity) as total_quantity_sold, " +
                     "  SUM(oi.subtotal) as total_revenue, " +
                     "  AVG(oi.unit_price) as avg_selling_price, " +
                     "  (SUM(oi.subtotal) - SUM(oi.quantity * p.cost)) as profit, " +
                     "  ROUND((SUM(oi.subtotal) - SUM(oi.quantity * p.cost)) / NULLIF(SUM(oi.subtotal), 0) * 100, 2) as profit_margin_pct " +
                     "FROM oltp.PRODUCTS p " +
                     "JOIN oltp.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "JOIN oltp.ORDERS o ON oi.order_id = o.order_id " +
                     "WHERE o.order_date >= DATEADD(day, -90, GETDATE()) " +
                     "  AND o.status != 'CANCELLED' " +
                     "GROUP BY p.product_id, p.product_name, p.category " +
                     "ORDER BY total_quantity_sold DESC";

        executeAnalyticsQuery(sql, "TopSellingProducts");
    }

    public void getRevenueByPaymentMethod() {
        String sql = "SELECT " +
                     "  o.payment_method, " +
                     "  COUNT(o.order_id) as transaction_count, " +
                     "  SUM(o.total_amount) as total_revenue, " +
                     "  AVG(o.total_amount) as avg_transaction_value, " +
                     "  SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as successful_transactions, " +
                     "  SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END) as failed_transactions, " +
                     "  ROUND(SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / COUNT(o.order_id), 2) as success_rate " +
                     "FROM oltp.ORDERS o " +
                     "LEFT JOIN oltp.TRANSACTIONS t ON o.order_id = t.order_id " +
                     "WHERE o.order_date >= DATEADD(day, -30, GETDATE()) " +
                     "GROUP BY o.payment_method " +
                     "ORDER BY total_revenue DESC";

        executeAnalyticsQuery(sql, "RevenueByPaymentMethod");
    }

    public void getSalesPerformanceByHour() {
        String sql = "SELECT " +
                     "  DATEPART(hour, o.order_date) as hour_of_day, " +
                     "  COUNT(o.order_id) as order_count, " +
                     "  SUM(o.total_amount) as revenue, " +
                     "  AVG(o.total_amount) as avg_order_value " +
                     "FROM oltp.ORDERS o " +
                     "WHERE o.order_date >= DATEADD(day, -7, GETDATE()) " +
                     "GROUP BY DATEPART(hour, o.order_date) " +
                     "ORDER BY hour_of_day";

        executeAnalyticsQuery(sql, "SalesPerformanceByHour");
    }

    private void executeAnalyticsQuery(String sql, String queryName) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("{} query returned {} rows in {}ms", queryName, rowCount, duration);

        } catch (SQLException e) {
            logger.error("Error executing {} query", queryName, e);
            throw new RuntimeException(e);
        }
    }
}
