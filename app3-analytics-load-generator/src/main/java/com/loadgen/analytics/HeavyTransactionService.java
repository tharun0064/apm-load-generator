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
public class HeavyTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(HeavyTransactionService.class);
    private final DatabaseManager dbManager;

    public HeavyTransactionService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Trace
    public void performHeavyTransaction() {
        String sql = buildComplexQuery();
        executeHeavyQuery(sql, "HeavyTransactionAnalysis");
    }

    private String buildComplexQuery() {
        // Complex multi-table JOIN with aggregations designed to take 5-10 seconds
        // This query performs comprehensive analytics across all tables with window functions,
        // subqueries, and multiple aggregations
        StringBuilder sql = new StringBuilder();
        sql.append("WITH OrderMetrics AS ( ");
        sql.append("    SELECT ");
        sql.append("        o.order_id, ");
        sql.append("        o.customer_id, ");
        sql.append("        o.order_date, ");
        sql.append("        o.total_amount, ");
        sql.append("        o.status, ");
        sql.append("        COUNT(oi.order_item_id) as item_count, ");
        sql.append("        SUM(oi.quantity * oi.unit_price) as calculated_total, ");
        sql.append("        AVG(oi.unit_price) as avg_item_price, ");
        sql.append("        MAX(oi.quantity) as max_quantity, ");
        sql.append("        MIN(oi.unit_price) as min_price ");
        sql.append("    FROM oltp_user.ORDERS o ");
        sql.append("    INNER JOIN oltp_user.ORDER_ITEMS oi ON o.order_id = oi.order_id ");
        sql.append("    WHERE o.order_date >= ADD_MONTHS(SYSDATE, -12) ");
        sql.append("    GROUP BY o.order_id, o.customer_id, o.order_date, o.total_amount, o.status ");
        sql.append("), ");
        sql.append("CustomerStats AS ( ");
        sql.append("    SELECT ");
        sql.append("        c.customer_id, ");
        sql.append("        c.first_name, ");
        sql.append("        c.last_name, ");
        sql.append("        c.email, ");
        sql.append("        c.city, ");
        sql.append("        c.state, ");
        sql.append("        COUNT(DISTINCT o.order_id) as total_orders, ");
        sql.append("        SUM(o.total_amount) as lifetime_value, ");
        sql.append("        AVG(o.total_amount) as avg_order_value, ");
        sql.append("        MAX(o.order_date) as last_order_date, ");
        sql.append("        ROUND(MONTHS_BETWEEN(SYSDATE, MAX(o.order_date)), 2) as months_since_last_order ");
        sql.append("    FROM oltp_user.CUSTOMERS c ");
        sql.append("    LEFT JOIN oltp_user.ORDERS o ON c.customer_id = o.customer_id ");
        sql.append("    GROUP BY c.customer_id, c.first_name, c.last_name, c.email, c.city, c.state ");
        sql.append("), ");
        sql.append("ProductPerformance AS ( ");
        sql.append("    SELECT ");
        sql.append("        p.product_id, ");
        sql.append("        p.product_name, ");
        sql.append("        p.category, ");
        sql.append("        p.brand, ");
        sql.append("        p.price as current_price, ");
        sql.append("        COUNT(DISTINCT oi.order_id) as times_ordered, ");
        sql.append("        SUM(oi.quantity) as total_quantity_sold, ");
        sql.append("        SUM(oi.quantity * oi.unit_price) as total_revenue, ");
        sql.append("        AVG(oi.quantity) as avg_quantity_per_order, ");
        sql.append("        RANK() OVER (PARTITION BY p.category ORDER BY SUM(oi.quantity) DESC) as category_rank ");
        sql.append("    FROM oltp_user.PRODUCTS p ");
        sql.append("    LEFT JOIN oltp_user.ORDER_ITEMS oi ON p.product_id = oi.product_id ");
        sql.append("    LEFT JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id AND o.order_date >= ADD_MONTHS(SYSDATE, -12) ");
        sql.append("    GROUP BY p.product_id, p.product_name, p.category, p.brand, p.price ");
        sql.append("), ");
        sql.append("TransactionAnalysis AS ( ");
        sql.append("    SELECT ");
        sql.append("        t.transaction_id, ");
        sql.append("        t.order_id, ");
        sql.append("        t.payment_method, ");
        sql.append("        t.amount, ");
        sql.append("        t.status as payment_status, ");
        sql.append("        t.transaction_date, ");
        sql.append("        CASE ");
        sql.append("            WHEN t.payment_method = 'Credit Card' THEN 'Card Payment' ");
        sql.append("            WHEN t.payment_method = 'PayPal' THEN 'Digital Wallet' ");
        sql.append("            WHEN t.payment_method = 'Debit Card' THEN 'Card Payment' ");
        sql.append("            ELSE 'Other' ");
        sql.append("        END as payment_category, ");
        sql.append("        TRUNC(t.transaction_date) as transaction_day, ");
        sql.append("        TO_CHAR(t.transaction_date, 'HH24') as transaction_hour ");
        sql.append("    FROM oltp_user.TRANSACTIONS t ");
        sql.append("    WHERE t.transaction_date >= ADD_MONTHS(SYSDATE, -12) ");
        sql.append(") ");
        sql.append("SELECT ");
        sql.append("    cs.customer_id, ");
        sql.append("    cs.first_name, ");
        sql.append("    cs.last_name, ");
        sql.append("    cs.email, ");
        sql.append("    cs.city, ");
        sql.append("    cs.state, ");
        sql.append("    cs.total_orders, ");
        sql.append("    cs.lifetime_value, ");
        sql.append("    cs.avg_order_value, ");
        sql.append("    cs.months_since_last_order, ");
        sql.append("    om.order_id, ");
        sql.append("    om.order_date, ");
        sql.append("    om.total_amount as order_amount, ");
        sql.append("    om.status as order_status, ");
        sql.append("    om.item_count, ");
        sql.append("    om.calculated_total, ");
        sql.append("    om.avg_item_price, ");
        sql.append("    om.max_quantity, ");
        sql.append("    om.min_price, ");
        sql.append("    pp.product_id, ");
        sql.append("    pp.product_name, ");
        sql.append("    pp.category, ");
        sql.append("    pp.brand, ");
        sql.append("    pp.current_price, ");
        sql.append("    pp.times_ordered, ");
        sql.append("    pp.total_quantity_sold, ");
        sql.append("    pp.total_revenue, ");
        sql.append("    pp.category_rank, ");
        sql.append("    ta.transaction_id, ");
        sql.append("    ta.payment_method, ");
        sql.append("    ta.payment_category, ");
        sql.append("    ta.payment_status, ");
        sql.append("    ta.transaction_hour, ");
        sql.append("    inv.quantity_on_hand as current_inventory, ");
        sql.append("    inv.reorder_level, ");
        sql.append("    CASE ");
        sql.append("        WHEN inv.quantity_on_hand < inv.reorder_level THEN 'Low Stock' ");
        sql.append("        WHEN inv.quantity_on_hand < inv.reorder_level * 2 THEN 'Medium Stock' ");
        sql.append("        ELSE 'High Stock' ");
        sql.append("    END as stock_level, ");
        sql.append("    ROW_NUMBER() OVER (PARTITION BY cs.customer_id ORDER BY om.order_date DESC) as customer_order_rank, ");
        sql.append("    SUM(om.total_amount) OVER (PARTITION BY cs.state) as state_total_revenue, ");
        sql.append("    AVG(om.total_amount) OVER (PARTITION BY pp.category) as category_avg_order, ");
        sql.append("    COUNT(*) OVER (PARTITION BY ta.payment_category) as payment_category_count, ");
        sql.append("    DENSE_RANK() OVER (ORDER BY cs.lifetime_value DESC) as customer_value_rank ");
        sql.append("FROM CustomerStats cs ");
        sql.append("INNER JOIN OrderMetrics om ON cs.customer_id = om.customer_id ");
        sql.append("INNER JOIN oltp_user.ORDER_ITEMS oi ON om.order_id = oi.order_id ");
        sql.append("INNER JOIN ProductPerformance pp ON oi.product_id = pp.product_id ");
        sql.append("LEFT JOIN TransactionAnalysis ta ON om.order_id = ta.order_id ");
        sql.append("LEFT JOIN oltp_user.INVENTORY inv ON pp.product_id = inv.product_id ");
        sql.append("WHERE cs.total_orders > 0 ");
        sql.append("    AND om.total_amount > 0 ");
        sql.append("    AND pp.total_quantity_sold > 0 ");
        sql.append("ORDER BY ");
        sql.append("    cs.lifetime_value DESC, ");
        sql.append("    om.order_date DESC, ");
        sql.append("    pp.category_rank ASC");
        return sql.toString();
    }

    @Trace
    private void executeHeavyQuery(String sql, String queryName) {
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
