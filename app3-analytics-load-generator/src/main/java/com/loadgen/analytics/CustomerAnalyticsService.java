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
 * Customer Analytics Service - Heavy customer analysis queries
 */
@Service
public class CustomerAnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerAnalyticsService.class);
    private final DatabaseManager dbManager;

    public CustomerAnalyticsService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void getCustomerSegmentation() {
        String sql = "SELECT " +
                     "  CASE " +
                     "    WHEN order_count >= 10 AND total_spent >= 1000 THEN 'VIP' " +
                     "    WHEN order_count >= 5 AND total_spent >= 500 THEN 'Loyal' " +
                     "    WHEN order_count >= 2 THEN 'Regular' " +
                     "    ELSE 'New' " +
                     "  END as customer_segment, " +
                     "  COUNT(*) as customer_count, " +
                     "  AVG(total_spent) as avg_lifetime_value, " +
                     "  AVG(order_count) as avg_order_count, " +
                     "  SUM(total_spent) as segment_revenue " +
                     "FROM ( " +
                     "  SELECT " +
                     "    c.customer_id, " +
                     "    COUNT(o.order_id) as order_count, " +
                     "    COALESCE(SUM(o.total_amount), 0) as total_spent " +
                     "  FROM oltp_user.CUSTOMERS c " +
                     "  LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "  GROUP BY c.customer_id " +
                     ") customer_stats " +
                     "GROUP BY " +
                     "  CASE " +
                     "    WHEN order_count >= 10 AND total_spent >= 1000 THEN 'VIP' " +
                     "    WHEN order_count >= 5 AND total_spent >= 500 THEN 'Loyal' " +
                     "    WHEN order_count >= 2 THEN 'Regular' " +
                     "    ELSE 'New' " +
                     "  END " +
                     "ORDER BY segment_revenue DESC";

        executeAnalyticsQuery(sql, "CustomerSegmentation");
    }

    @Trace
    public void getCustomerLifetimeValue() {
        String sql = "SELECT " +
                     "  c.customer_id, " +
                     "  c.first_name || ' ' || c.last_name as customer_name, " +
                     "  c.customer_type, " +
                     "  c.loyalty_points, " +
                     "  COUNT(o.order_id) as total_orders, " +
                     "  SUM(o.total_amount) as lifetime_value, " +
                     "  AVG(o.total_amount) as avg_order_value, " +
                     "  MAX(o.order_date) as last_order_date, " +
                     "  ROUND(MONTHS_BETWEEN(SYSDATE, c.created_at), 1) as customer_tenure_months, " +
                     "  ROUND(SUM(o.total_amount) / NULLIF(MONTHS_BETWEEN(SYSDATE, c.created_at), 0), 2) as monthly_value " +
                     "FROM oltp_user.CUSTOMERS c " +
                     "LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "  AND o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY c.customer_id, c.first_name, c.last_name, c.customer_type, c.loyalty_points, c.created_at " +
                     "HAVING COUNT(o.order_id) > 0 " +
                     "ORDER BY lifetime_value DESC NULLS LAST " +
                     "FETCH FIRST 100 ROWS ONLY";

        executeAnalyticsQuery(sql, "CustomerLifetimeValue");
    }

    @Trace
    public void getCustomerRetentionRate() {
        String sql = "WITH monthly_customers AS ( " +
                     "  SELECT " +
                     "    TO_CHAR(o.order_date, 'YYYY-MM') as month, " +
                     "    o.customer_id " +
                     "  FROM oltp_user.ORDERS o " +
                     "  WHERE o.order_date >= ADD_MONTHS(SYSDATE, -12) " +
                     "  GROUP BY TO_CHAR(o.order_date, 'YYYY-MM'), o.customer_id " +
                     "), " +
                     "retention_calc AS ( " +
                     "  SELECT " +
                     "    mc1.month, " +
                     "    COUNT(DISTINCT mc1.customer_id) as customers_this_month, " +
                     "    COUNT(DISTINCT mc2.customer_id) as customers_next_month " +
                     "  FROM monthly_customers mc1 " +
                     "  LEFT JOIN monthly_customers mc2 " +
                     "    ON mc1.customer_id = mc2.customer_id " +
                     "    AND TO_DATE(mc2.month, 'YYYY-MM') = ADD_MONTHS(TO_DATE(mc1.month, 'YYYY-MM'), 1) " +
                     "  GROUP BY mc1.month " +
                     ") " +
                     "SELECT " +
                     "  month, " +
                     "  customers_this_month, " +
                     "  customers_next_month, " +
                     "  ROUND(customers_next_month * 100.0 / NULLIF(customers_this_month, 0), 2) as retention_rate " +
                     "FROM retention_calc " +
                     "ORDER BY month DESC";

        executeAnalyticsQuery(sql, "CustomerRetentionRate");
    }

    @Trace
    public void getHighValueCustomers(int limit) {
        String sql = "SELECT " +
                     "  c.customer_id, " +
                     "  c.email, " +
                     "  c.customer_type, " +
                     "  COUNT(o.order_id) as order_count, " +
                     "  SUM(o.total_amount) as total_spent, " +
                     "  AVG(o.total_amount) as avg_order_value, " +
                     "  MAX(o.order_date) as last_order_date, " +
                     "  COUNT(DISTINCT TRUNC(o.order_date, 'MM')) as active_months, " +
                     "  SUM(oi.quantity) as total_items_purchased " +
                     "FROM oltp_user.CUSTOMERS c " +
                     "JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "WHERE o.status IN ('COMPLETED', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY c.customer_id, c.email, c.customer_type " +
                     "ORDER BY total_spent DESC " +
                     "FETCH FIRST " + limit + " ROWS ONLY";

        executeAnalyticsQuery(sql, "HighValueCustomers");
    }

    @Trace
    public void getCustomerPurchaseFrequency() {
        String sql = "SELECT " +
                     "  purchase_frequency_bucket, " +
                     "  COUNT(*) as customer_count, " +
                     "  AVG(total_orders) as avg_orders, " +
                     "  AVG(total_revenue) as avg_revenue " +
                     "FROM ( " +
                     "  SELECT " +
                     "    c.customer_id, " +
                     "    COUNT(o.order_id) as total_orders, " +
                     "    SUM(o.total_amount) as total_revenue, " +
                     "    CASE " +
                     "      WHEN COUNT(o.order_id) >= 20 THEN '20+ orders' " +
                     "      WHEN COUNT(o.order_id) >= 10 THEN '10-19 orders' " +
                     "      WHEN COUNT(o.order_id) >= 5 THEN '5-9 orders' " +
                     "      WHEN COUNT(o.order_id) >= 2 THEN '2-4 orders' " +
                     "      ELSE '1 order' " +
                     "    END as purchase_frequency_bucket " +
                     "  FROM oltp_user.CUSTOMERS c " +
                     "  LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "  GROUP BY c.customer_id " +
                     ") " +
                     "GROUP BY purchase_frequency_bucket " +
                     "ORDER BY " +
                     "  CASE purchase_frequency_bucket " +
                     "    WHEN '20+ orders' THEN 1 " +
                     "    WHEN '10-19 orders' THEN 2 " +
                     "    WHEN '5-9 orders' THEN 3 " +
                     "    WHEN '2-4 orders' THEN 4 " +
                     "    ELSE 5 " +
                     "  END";

        executeAnalyticsQuery(sql, "CustomerPurchaseFrequency");
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
