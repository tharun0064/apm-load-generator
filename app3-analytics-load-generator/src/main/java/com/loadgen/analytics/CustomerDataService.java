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
        // Heavy multi-table join query
        String sql = "SELECT /*+ FULL(c) FULL(o) FULL(oi) FULL(p) FULL(inv) FULL(t) */ " +
                     "  c.customer_id, " +
                     "  c.first_name || ' ' || c.last_name as customer_name, " +
                     "  c.email, " +
                     "  c.phone, " +
                     "  c.address, " +
                     "  c.city, " +
                     "  c.state, " +
                     "  c.country, " +
                     "  c.postal_code, " +
                     "  c.customer_type, " +
                     "  c.credit_limit, " +
                     "  c.loyalty_points, " +
                     "  o.order_id, " +
                     "  o.order_date, " +
                     "  o.total_amount, " +
                     "  o.status as order_status, " +
                     "  o.payment_method, " +
                     "  o.shipping_cost, " +
                     "  o.tax_amount, " +
                     "  oi.order_item_id, " +
                     "  oi.quantity, " +
                     "  oi.unit_price, " +
                     "  oi.subtotal, " +
                     "  p.product_id, " +
                     "  p.product_name, " +
                     "  p.description, " +
                     "  p.category, " +
                     "  p.subcategory, " +
                     "  p.brand, " +
                     "  p.supplier, " +
                     "  p.price as current_price, " +
                     "  p.cost, " +
                     "  p.weight, " +
                     "  p.dimensions, " +
                     "  p.is_active, " +
                     "  inv.inventory_id, " +
                     "  inv.quantity_on_hand, " +
                     "  inv.quantity_available, " +
                     "  inv.quantity_reserved, " +
                     "  inv.reorder_level, " +
                     "  inv.reorder_quantity, " +
                     "  inv.warehouse_location, " +
                     "  t.transaction_id, " +
                     "  t.payment_method as transaction_payment_method, " +
                     "  t.payment_gateway, " +
                     "  t.transaction_type, " +
                     "  t.amount as transaction_amount, " +
                     "  t.status as transaction_status, " +
                     "  t.transaction_date, " +
                     "  t.processed_at, " +
                     "  t.confirmation_number, " +
                     "  COUNT(*) OVER (PARTITION BY c.customer_id) as customer_total_orders, " +
                     "  SUM(o.total_amount) OVER (PARTITION BY c.customer_id) as customer_lifetime_value, " +
                     "  AVG(o.total_amount) OVER (PARTITION BY c.customer_id) as customer_avg_order, " +
                     "  MAX(o.order_date) OVER (PARTITION BY c.customer_id) as customer_last_order_date, " +
                     "  COUNT(*) OVER (PARTITION BY p.category) as category_order_count, " +
                     "  SUM(oi.subtotal) OVER (PARTITION BY p.category) as category_revenue, " +
                     "  AVG(oi.unit_price) OVER (PARTITION BY p.category) as category_avg_price, " +
                     "  COUNT(*) OVER (PARTITION BY c.state) as state_order_count, " +
                     "  SUM(o.total_amount) OVER (PARTITION BY c.state) as state_total_revenue, " +
                     "  COUNT(*) OVER (PARTITION BY p.brand) as brand_order_count, " +
                     "  SUM(oi.quantity) OVER (PARTITION BY p.brand) as brand_units_sold, " +
                     "  RANK() OVER (PARTITION BY c.customer_id ORDER BY o.order_date DESC) as customer_order_rank, " +
                     "  RANK() OVER (PARTITION BY p.category ORDER BY oi.subtotal DESC) as category_revenue_rank, " +
                     "  DENSE_RANK() OVER (ORDER BY o.total_amount DESC) as overall_order_value_rank, " +
                     "  ROW_NUMBER() OVER (PARTITION BY c.state ORDER BY o.order_date DESC) as state_order_sequence, " +
                     "  ROUND(MONTHS_BETWEEN(SYSDATE, o.order_date), 2) as months_since_order, " +
                     "  ROUND(MONTHS_BETWEEN(SYSDATE, c.created_at), 2) as customer_age_months, " +
                     "  CASE " +
                     "    WHEN o.total_amount > 1000 THEN 'High Value' " +
                     "    WHEN o.total_amount > 500 THEN 'Medium Value' " +
                     "    ELSE 'Low Value' " +
                     "  END as order_value_category, " +
                     "  CASE " +
                     "    WHEN inv.quantity_on_hand < inv.reorder_level THEN 'Critical' " +
                     "    WHEN inv.quantity_on_hand < inv.reorder_level * 2 THEN 'Low' " +
                     "    WHEN inv.quantity_on_hand < inv.reorder_level * 5 THEN 'Medium' " +
                     "    ELSE 'High' " +
                     "  END as inventory_status, " +
                     "  (oi.subtotal - (oi.quantity * p.cost)) as item_profit, " +
                     "  ROUND((oi.subtotal - (oi.quantity * p.cost)) / NULLIF(oi.subtotal, 0) * 100, 2) as item_profit_margin, " +
                     "  (inv.quantity_on_hand * p.cost) as inventory_value_at_cost, " +
                     "  (inv.quantity_on_hand * p.price) as inventory_value_at_retail, " +
                     "  EXTRACT(YEAR FROM o.order_date) as order_year, " +
                     "  EXTRACT(MONTH FROM o.order_date) as order_month, " +
                     "  EXTRACT(DAY FROM o.order_date) as order_day, " +
                     "  TO_CHAR(o.order_date, 'Day') as order_day_name, " +
                     "  TO_CHAR(o.order_date, 'HH24:MI:SS') as order_time, " +
                     "  TRUNC(o.order_date) as order_date_only " +
                     "FROM oltp_user.CUSTOMERS c, oltp_user.ORDERS o, oltp_user.ORDER_ITEMS oi, " +
                     "     oltp_user.PRODUCTS p, oltp_user.INVENTORY inv, oltp_user.TRANSACTIONS t " +
                     "WHERE c.customer_id = o.customer_id " +
                     "  AND o.order_id = oi.order_id " +
                     "  AND oi.product_id = p.product_id " +
                     "  AND p.product_id = inv.product_id " +
                     "  AND o.order_id = t.order_id " +
                     "  AND o.order_date >= ADD_MONTHS(SYSDATE, -24) " +
                     "ORDER BY c.customer_id, o.order_date DESC, oi.subtotal DESC";

        executeAnalyticsQuery(sql, "CustomerAnalytics");
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
