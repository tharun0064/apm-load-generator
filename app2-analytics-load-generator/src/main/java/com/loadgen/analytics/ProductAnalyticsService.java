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
 * Product Analytics Service - Heavy product analysis queries
 */
@Service
public class ProductAnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(ProductAnalyticsService.class);
    private final DatabaseManager dbManager;

    public ProductAnalyticsService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void getProductPerformanceReport() {
        String sql = "SELECT " +
                     "  p.product_id, " +
                     "  p.product_name, " +
                     "  p.category, " +
                     "  p.price as current_price, " +
                     "  COALESCE(SUM(oi.quantity), 0) as total_units_sold, " +
                     "  COALESCE(SUM(oi.subtotal), 0) as total_revenue, " +
                     "  COALESCE(SUM(oi.quantity * p.cost), 0) as total_cost, " +
                     "  COALESCE(SUM(oi.subtotal) - SUM(oi.quantity * p.cost), 0) as total_profit, " +
                     "  ROUND(COALESCE((SUM(oi.subtotal) - SUM(oi.quantity * p.cost)) / NULLIF(SUM(oi.subtotal), 0) * 100, 0), 2) as profit_margin_pct, " +
                     "  COALESCE(SUM(i.quantity_available), 0) as current_inventory, " +
                     "  COALESCE(SUM(i.quantity_reserved), 0) as reserved_inventory, " +
                     "  COUNT(DISTINCT oi.order_id) as order_count " +
                     "FROM oltp_user.PRODUCTS p " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id AND o.order_date >= SYSDATE - 90 " +
                     "LEFT JOIN oltp_user.INVENTORY i ON p.product_id = i.product_id " +
                     "WHERE p.is_active = 1 " +
                     "GROUP BY p.product_id, p.product_name, p.category, p.price, p.cost " +
                     "ORDER BY total_revenue DESC";

        executeAnalyticsQuery(sql, "ProductPerformanceReport");
    }

    @Trace
    public void getInventoryTurnoverRate() {
        String sql = "SELECT " +
                     "  p.product_id, " +
                     "  p.product_name, " +
                     "  p.category, " +
                     "  SUM(i.quantity_available + i.quantity_reserved) as avg_inventory, " +
                     "  COALESCE(SUM(oi.quantity), 0) as units_sold_90days, " +
                     "  ROUND(COALESCE(SUM(oi.quantity) / NULLIF(SUM(i.quantity_available + i.quantity_reserved), 0), 0), 2) as turnover_rate, " +
                     "  ROUND(90.0 / NULLIF(COALESCE(SUM(oi.quantity) / NULLIF(SUM(i.quantity_available + i.quantity_reserved), 0), 0), 0), 1) as days_to_turnover " +
                     "FROM oltp_user.PRODUCTS p " +
                     "JOIN oltp_user.INVENTORY i ON p.product_id = i.product_id " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "  AND o.order_date >= SYSDATE - 90 " +
                     "WHERE p.is_active = 1 " +
                     "GROUP BY p.product_id, p.product_name, p.category " +
                     "ORDER BY turnover_rate DESC NULLS LAST";

        executeAnalyticsQuery(sql, "InventoryTurnoverRate");
    }

    @Trace
    public void getSlowMovingProducts() {
        String sql = "SELECT " +
                     "  p.product_id, " +
                     "  p.product_name, " +
                     "  p.category, " +
                     "  p.price, " +
                     "  SUM(i.quantity_available) as inventory_on_hand, " +
                     "  COALESCE(SUM(oi.quantity), 0) as units_sold_60days, " +
                     "  SUM(i.quantity_available) * p.cost as inventory_value, " +
                     "  ROUND(60.0 / NULLIF(COALESCE(SUM(oi.quantity), 0), 0), 1) as days_per_sale " +
                     "FROM oltp_user.PRODUCTS p " +
                     "JOIN oltp_user.INVENTORY i ON p.product_id = i.product_id " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "  AND o.order_date >= SYSDATE - 60 " +
                     "WHERE p.is_active = 1 " +
                     "  AND i.quantity_available > 100 " +
                     "GROUP BY p.product_id, p.product_name, p.category, p.price, p.cost " +
                     "HAVING COALESCE(SUM(oi.quantity), 0) < 10 " +
                     "ORDER BY inventory_value DESC";

        executeAnalyticsQuery(sql, "SlowMovingProducts");
    }

    @Trace
    public void getProfitMarginByCategory() {
        String sql = "SELECT " +
                     "  p.category, " +
                     "  COUNT(DISTINCT p.product_id) as product_count, " +
                     "  COUNT(DISTINCT oi.order_id) as order_count, " +
                     "  SUM(oi.quantity) as total_units_sold, " +
                     "  SUM(oi.subtotal) as total_revenue, " +
                     "  SUM(oi.quantity * p.cost) as total_cost, " +
                     "  SUM(oi.subtotal) - SUM(oi.quantity * p.cost) as total_profit, " +
                     "  ROUND((SUM(oi.subtotal) - SUM(oi.quantity * p.cost)) / NULLIF(SUM(oi.subtotal), 0) * 100, 2) as profit_margin_pct, " +
                     "  ROUND(AVG(p.price), 2) as avg_price, " +
                     "  ROUND((SUM(oi.subtotal) - SUM(oi.quantity * p.cost)) / COUNT(DISTINCT p.product_id), 2) as profit_per_product " +
                     "FROM oltp_user.PRODUCTS p " +
                     "JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "WHERE o.order_date >= SYSDATE - 90 " +
                     "  AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY p.category " +
                     "ORDER BY total_profit DESC";

        executeAnalyticsQuery(sql, "ProfitMarginByCategory");
    }

    @Trace
    public void getProductAffinityAnalysis() {
        // Products frequently bought together
        String sql = "SELECT " +
                     "  p1.product_name as product_1, " +
                     "  p2.product_name as product_2, " +
                     "  COUNT(DISTINCT oi1.order_id) as times_bought_together, " +
                     "  ROUND(AVG(oi1.subtotal + oi2.subtotal), 2) as avg_combined_value " +
                     "FROM oltp_user.ORDER_ITEMS oi1 " +
                     "JOIN oltp_user.ORDER_ITEMS oi2 ON oi1.order_id = oi2.order_id " +
                     "  AND oi1.product_id < oi2.product_id " +
                     "JOIN oltp_user.PRODUCTS p1 ON oi1.product_id = p1.product_id " +
                     "JOIN oltp_user.PRODUCTS p2 ON oi2.product_id = p2.product_id " +
                     "JOIN oltp_user.ORDERS o ON oi1.order_id = o.order_id " +
                     "WHERE o.order_date >= SYSDATE - 60 " +
                     "GROUP BY p1.product_name, p2.product_name " +
                     "HAVING COUNT(DISTINCT oi1.order_id) >= 3 " +
                     "ORDER BY times_bought_together DESC " +
                     "FETCH FIRST 50 ROWS ONLY";

        executeAnalyticsQuery(sql, "ProductAffinityAnalysis");
    }

    @Trace
    public void getProductRevenueGrowth() {
        String sql = "WITH monthly_sales AS ( " +
                     "  SELECT " +
                     "    p.product_id, " +
                     "    p.product_name, " +
                     "    TO_CHAR(o.order_date, 'YYYY-MM') as month, " +
                     "    SUM(oi.subtotal) as revenue " +
                     "  FROM oltp_user.PRODUCTS p " +
                     "  JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "  JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "  WHERE o.order_date >= ADD_MONTHS(SYSDATE, -6) " +
                     "  GROUP BY p.product_id, p.product_name, TO_CHAR(o.order_date, 'YYYY-MM') " +
                     "), " +
                     "monthly_comparison AS ( " +
                     "  SELECT " +
                     "    ms1.product_id, " +
                     "    ms1.product_name, " +
                     "    ms1.month as current_month, " +
                     "    ms1.revenue as current_revenue, " +
                     "    ms2.revenue as previous_revenue " +
                     "  FROM monthly_sales ms1 " +
                     "  LEFT JOIN monthly_sales ms2 " +
                     "    ON ms1.product_id = ms2.product_id " +
                     "    AND TO_DATE(ms2.month, 'YYYY-MM') = ADD_MONTHS(TO_DATE(ms1.month, 'YYYY-MM'), -1) " +
                     ") " +
                     "SELECT " +
                     "  product_name, " +
                     "  current_month, " +
                     "  current_revenue, " +
                     "  previous_revenue, " +
                     "  ROUND((current_revenue - COALESCE(previous_revenue, 0)) / NULLIF(previous_revenue, 1) * 100, 2) as growth_pct " +
                     "FROM monthly_comparison " +
                     "WHERE current_revenue > 100 " +
                     "ORDER BY growth_pct DESC NULLS LAST " +
                     "FETCH FIRST 30 ROWS ONLY";

        executeAnalyticsQuery(sql, "ProductRevenueGrowth");
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
