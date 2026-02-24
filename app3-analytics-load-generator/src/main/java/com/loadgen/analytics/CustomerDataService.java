package com.loadgen.analytics;

import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class CustomerDataService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDataService.class);
    private final DatabaseManager dbManager;

    public CustomerDataService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void getCustomerAnalytics() {
        // Comprehensive customer analytics query
        String sql = "WITH OrderMetrics AS ( " +
                     "    SELECT " +
                     "        o.order_id, " +
                     "        o.customer_id, " +
                     "        o.order_date, " +
                     "        o.total_amount, " +
                     "        o.status, " +
                     "        COUNT(oi.order_item_id) as item_count, " +
                     "        SUM(oi.quantity * oi.unit_price) as calculated_total, " +
                     "        AVG(oi.unit_price) as avg_item_price, " +
                     "        MAX(oi.quantity) as max_quantity, " +
                     "        MIN(oi.unit_price) as min_price " +
                     "    FROM oltp_user.ORDERS o " +
                     "    INNER JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id " +
                     "    WHERE o.order_date >= ADD_MONTHS(SYSDATE, -12) " +
                     "    GROUP BY o.order_id, o.customer_id, o.order_date, o.total_amount, o.status " +
                     "), " +
                     "CustomerStats AS ( " +
                     "    SELECT " +
                     "        c.customer_id, " +
                     "        c.first_name, " +
                     "        c.last_name, " +
                     "        c.email, " +
                     "        c.city, " +
                     "        c.state, " +
                     "        COUNT(DISTINCT o.order_id) as total_orders, " +
                     "        SUM(o.total_amount) as lifetime_value, " +
                     "        AVG(o.total_amount) as avg_order_value, " +
                     "        MAX(o.order_date) as last_order_date, " +
                     "        ROUND(MONTHS_BETWEEN(SYSDATE, MAX(o.order_date)), 2) as months_since_last_order " +
                     "    FROM oltp_user.CUSTOMERS c " +
                     "    LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id " +
                     "    GROUP BY c.customer_id, c.first_name, c.last_name, c.email, c.city, c.state " +
                     "), " +
                     "ProductPerformance AS ( " +
                     "    SELECT " +
                     "        p.product_id, " +
                     "        p.product_name, " +
                     "        p.category, " +
                     "        p.brand, " +
                     "        p.price as current_price, " +
                     "        COUNT(DISTINCT oi.order_id) as times_ordered, " +
                     "        SUM(oi.quantity) as total_quantity_sold, " +
                     "        SUM(oi.quantity * oi.unit_price) as total_revenue, " +
                     "        AVG(oi.quantity) as avg_quantity_per_order, " +
                     "        RANK() OVER (PARTITION BY p.category ORDER BY SUM(oi.quantity) DESC) as category_rank " +
                     "    FROM oltp_user.PRODUCTS p " +
                     "    LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id " +
                     "    LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id AND o.order_date >= ADD_MONTHS(SYSDATE, -12) " +
                     "    GROUP BY p.product_id, p.product_name, p.category, p.brand, p.price " +
                     "), " +
                     "TransactionAnalysis AS ( " +
                     "    SELECT " +
                     "        t.transaction_id, " +
                     "        t.order_id, " +
                     "        t.payment_method, " +
                     "        t.amount, " +
                     "        t.status as payment_status, " +
                     "        t.transaction_date, " +
                     "        CASE " +
                     "            WHEN t.payment_method = 'Credit Card' THEN 'Card Payment' " +
                     "            WHEN t.payment_method = 'PayPal' THEN 'Digital Wallet' " +
                     "            WHEN t.payment_method = 'Debit Card' THEN 'Card Payment' " +
                     "            ELSE 'Other' " +
                     "        END as payment_category, " +
                     "        TRUNC(t.transaction_date) as transaction_day, " +
                     "        TO_CHAR(t.transaction_date, 'HH24') as transaction_hour " +
                     "    FROM oltp_user.TRANSACTIONS t " +
                     "    WHERE t.transaction_date >= ADD_MONTHS(SYSDATE, -12) " +
                     ") " +
                     "SELECT " +
                     "    cs.customer_id, " +
                     "    cs.first_name, " +
                     "    cs.last_name, " +
                     "    cs.email, " +
                     "    cs.city, " +
                     "    cs.state, " +
                     "    cs.total_orders, " +
                     "    cs.lifetime_value, " +
                     "    cs.avg_order_value, " +
                     "    cs.months_since_last_order, " +
                     "    om.order_id, " +
                     "    om.order_date, " +
                     "    om.total_amount as order_amount, " +
                     "    om.status as order_status, " +
                     "    om.item_count, " +
                     "    om.calculated_total, " +
                     "    om.avg_item_price, " +
                     "    om.max_quantity, " +
                     "    om.min_price, " +
                     "    pp.product_id, " +
                     "    pp.product_name, " +
                     "    pp.category, " +
                     "    pp.brand, " +
                     "    pp.current_price, " +
                     "    pp.times_ordered, " +
                     "    pp.total_quantity_sold, " +
                     "    pp.total_revenue, " +
                     "    pp.category_rank, " +
                     "    ta.transaction_id, " +
                     "    ta.payment_method, " +
                     "    ta.payment_category, " +
                     "    ta.payment_status, " +
                     "    ta.transaction_hour, " +
                     "    inv.quantity_on_hand as current_inventory, " +
                     "    inv.reorder_level, " +
                     "    CASE " +
                     "        WHEN inv.quantity_on_hand < inv.reorder_level THEN 'Low Stock' " +
                     "        WHEN inv.quantity_on_hand < inv.reorder_level * 2 THEN 'Medium Stock' " +
                     "        ELSE 'High Stock' " +
                     "    END as stock_level, " +
                     "    ROW_NUMBER() OVER (PARTITION BY cs.customer_id ORDER BY om.order_date DESC) as customer_order_rank, " +
                     "    SUM(om.total_amount) OVER (PARTITION BY cs.state) as state_total_revenue, " +
                     "    AVG(om.total_amount) OVER (PARTITION BY pp.category) as category_avg_order, " +
                     "    COUNT(*) OVER (PARTITION BY ta.payment_category) as payment_category_count, " +
                     "    DENSE_RANK() OVER (ORDER BY cs.lifetime_value DESC) as customer_value_rank " +
                     "FROM CustomerStats cs " +
                     "INNER JOIN OrderMetrics om ON cs.customer_id = om.customer_id " +
                     "INNER JOIN oltp_user.ORDER_ITEMS oi ON om.order_id = oi.order_id " +
                     "INNER JOIN ProductPerformance pp ON oi.product_id = pp.product_id " +
                     "LEFT JOIN TransactionAnalysis ta ON om.order_id = ta.order_id " +
                     "LEFT JOIN oltp_user.INVENTORY inv ON pp.product_id = inv.product_id " +
                     "WHERE cs.total_orders > 0 " +
                     "    AND om.total_amount > 0 " +
                     "    AND pp.total_quantity_sold > 0 " +
                     "ORDER BY " +
                     "    cs.lifetime_value DESC, " +
                     "    om.order_date DESC, " +
                     "    pp.category_rank ASC";

        executeAnalyticsQuery(sql, "CustomerAnalytics");
    }

    @Trace
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
