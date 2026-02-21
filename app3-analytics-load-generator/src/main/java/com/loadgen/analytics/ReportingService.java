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
 * Reporting Service - Complex multi-table reporting queries
 */
@Service
public class ReportingService {
    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    private final DatabaseManager dbManager;

    public ReportingService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void generateExecutiveDashboard() {
        String sql = "SELECT " +
                     "  'Orders' as metric, " +
                     "  (SELECT COUNT(*) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE)) as today_count, " +
                     "  (SELECT COUNT(*) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE - 1)) as yesterday_count, " +
                     "  (SELECT COUNT(*) FROM oltp_user.ORDERS WHERE order_date >= TRUNC(SYSDATE, 'MM')) as month_to_date " +
                     "FROM DUAL " +
                     "UNION ALL " +
                     "SELECT " +
                     "  'Revenue' as metric, " +
                     "  ROUND((SELECT SUM(total_amount) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE)), 0) as today_count, " +
                     "  ROUND((SELECT SUM(total_amount) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE - 1)), 0) as yesterday_count, " +
                     "  ROUND((SELECT SUM(total_amount) FROM oltp_user.ORDERS WHERE order_date >= TRUNC(SYSDATE, 'MM')), 0) as month_to_date " +
                     "FROM DUAL " +
                     "UNION ALL " +
                     "SELECT " +
                     "  'New Customers' as metric, " +
                     "  (SELECT COUNT(*) FROM oltp_user.CUSTOMERS WHERE TRUNC(created_at) = TRUNC(SYSDATE)) as today_count, " +
                     "  (SELECT COUNT(*) FROM oltp_user.CUSTOMERS WHERE TRUNC(created_at) = TRUNC(SYSDATE - 1)) as yesterday_count, " +
                     "  (SELECT COUNT(*) FROM oltp_user.CUSTOMERS WHERE created_at >= TRUNC(SYSDATE, 'MM')) as month_to_date " +
                     "FROM DUAL " +
                     "UNION ALL " +
                     "SELECT " +
                     "  'Avg Order Value' as metric, " +
                     "  ROUND((SELECT AVG(total_amount) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE)), 2) as today_count, " +
                     "  ROUND((SELECT AVG(total_amount) FROM oltp_user.ORDERS WHERE TRUNC(order_date) = TRUNC(SYSDATE - 1)), 2) as yesterday_count, " +
                     "  ROUND((SELECT AVG(total_amount) FROM oltp_user.ORDERS WHERE order_date >= TRUNC(SYSDATE, 'MM')), 2) as month_to_date " +
                     "FROM DUAL";

        executeReportQuery(sql, "ExecutiveDashboard");
    }

    @Trace
    public void generateSalesReport() {
        String sql = "SELECT " +
                     "  TO_CHAR(o.order_date, 'YYYY-MM-DD') as order_date, " +
                     "  o.status, " +
                     "  COUNT(DISTINCT o.order_id) as order_count, " +
                     "  COUNT(DISTINCT o.customer_id) as unique_customers, " +
                     "  ROUND(SUM(o.total_amount), 2) as gross_revenue, " +
                     "  ROUND(SUM(o.tax_amount), 2) as tax_collected, " +
                     "  ROUND(SUM(o.shipping_cost), 2) as shipping_revenue, " +
                     "  ROUND(SUM(o.total_amount - o.tax_amount - COALESCE(o.shipping_cost, 0)), 2) as net_revenue, " +
                     "  ROUND(AVG(o.total_amount), 2) as avg_order_value, " +
                     "  COALESCE(SUM(oi.quantity), 0) as total_items_sold, " +
                     "  COUNT(DISTINCT oi.product_id) as unique_products_sold " +
                     "FROM oltp_user.ORDERS o " +
                     "LEFT JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "WHERE o.order_date >= SYSDATE - 30 " +
                     "GROUP BY TO_CHAR(o.order_date, 'YYYY-MM-DD'), o.status " +
                     "ORDER BY order_date DESC, status";

        executeReportQuery(sql, "SalesReport");
    }

    @Trace
    public void generateInventoryReport() {
        String sql = "SELECT " +
                     "  p.category, " +
                     "  p.subcategory, " +
                     "  COUNT(DISTINCT p.product_id) as product_count, " +
                     "  SUM(i.quantity_available) as total_available, " +
                     "  SUM(i.quantity_reserved) as total_reserved, " +
                     "  SUM(i.quantity_available + i.quantity_reserved) as total_inventory, " +
                     "  SUM((i.quantity_available + i.quantity_reserved) * p.cost) as inventory_value_cost, " +
                     "  SUM((i.quantity_available + i.quantity_reserved) * p.price) as inventory_value_retail, " +
                     "  ROUND(AVG(i.quantity_available), 0) as avg_available_per_product, " +
                     "  COUNT(CASE WHEN i.quantity_available < i.reorder_level THEN 1 END) as products_below_reorder, " +
                     "  COUNT(CASE WHEN i.quantity_available = 0 THEN 1 END) as out_of_stock_count, " +
                     "  COALESCE(SUM(recent_sales.units_sold), 0) as units_sold_30days " +
                     "FROM oltp_user.PRODUCTS p " +
                     "JOIN oltp_user.INVENTORY i ON p.product_id = i.product_id " +
                     "LEFT JOIN ( " +
                     "  SELECT oi.product_id, SUM(oi.quantity) as units_sold " +
                     "  FROM oltp_user.ORDER_ITEMS oi " +
                     "  JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id " +
                     "  WHERE o.order_date >= SYSDATE - 30 " +
                     "  GROUP BY oi.product_id " +
                     ") recent_sales ON p.product_id = recent_sales.product_id " +
                     "WHERE p.is_active = 1 " +
                     "GROUP BY p.category, p.subcategory " +
                     "ORDER BY inventory_value_cost DESC";

        executeReportQuery(sql, "InventoryReport");
    }

    @Trace
    public void generateCustomerReport() {
        String sql = "SELECT " +
                     "  c.customer_type, " +
                     "  COUNT(DISTINCT c.customer_id) as customer_count, " +
                     "  ROUND(AVG(c.loyalty_points), 0) as avg_loyalty_points, " +
                     "  COALESCE(SUM(order_stats.order_count), 0) as total_orders, " +
                     "  COALESCE(SUM(order_stats.order_value), 0) as total_revenue, " +
                     "  ROUND(COALESCE(AVG(order_stats.order_count), 0), 2) as avg_orders_per_customer, " +
                     "  ROUND(COALESCE(AVG(order_stats.order_value), 0), 2) as avg_revenue_per_customer, " +
                     "  ROUND(COALESCE(AVG(order_stats.avg_order_value), 0), 2) as avg_order_value, " +
                     "  COUNT(CASE WHEN order_stats.last_order_date >= SYSDATE - 30 THEN 1 END) as active_last_30_days, " +
                     "  COUNT(CASE WHEN order_stats.last_order_date >= SYSDATE - 90 THEN 1 END) as active_last_90_days, " +
                     "  COUNT(CASE WHEN order_stats.last_order_date < SYSDATE - 90 OR order_stats.last_order_date IS NULL THEN 1 END) as inactive " +
                     "FROM oltp_user.CUSTOMERS c " +
                     "LEFT JOIN ( " +
                     "  SELECT " +
                     "    o.customer_id, " +
                     "    COUNT(o.order_id) as order_count, " +
                     "    SUM(o.total_amount) as order_value, " +
                     "    AVG(o.total_amount) as avg_order_value, " +
                     "    MAX(o.order_date) as last_order_date " +
                     "  FROM oltp_user.ORDERS o " +
                     "  WHERE o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "  GROUP BY o.customer_id " +
                     ") order_stats ON c.customer_id = order_stats.customer_id " +
                     "GROUP BY c.customer_type " +
                     "ORDER BY total_revenue DESC";

        executeReportQuery(sql, "CustomerReport");
    }

    @Trace
    public void generateTransactionReport() {
        String sql = "SELECT " +
                     "  t.payment_gateway, " +
                     "  t.transaction_type, " +
                     "  t.status, " +
                     "  COUNT(t.transaction_id) as transaction_count, " +
                     "  SUM(t.amount) as total_amount, " +
                     "  AVG(t.amount) as avg_amount, " +
                     "  MIN(t.amount) as min_amount, " +
                     "  MAX(t.amount) as max_amount, " +
                     "  ROUND(COUNT(t.transaction_id) * 100.0 / SUM(COUNT(t.transaction_id)) OVER (PARTITION BY t.payment_gateway), 2) as pct_of_gateway_total " +
                     "FROM oltp_user.TRANSACTIONS t " +
                     "WHERE t.processed_at >= SYSDATE - 30 " +
                     "GROUP BY t.payment_gateway, t.transaction_type, t.status " +
                     "ORDER BY t.payment_gateway, total_amount DESC";

        executeReportQuery(sql, "TransactionReport");
    }

    private void executeReportQuery(String sql, String reportName) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("{} generated {} rows in {}ms", reportName, rowCount, duration);

        } catch (SQLException e) {
            logger.error("Error generating {}", reportName, e);
            throw new RuntimeException(e);
        }
    }
}
