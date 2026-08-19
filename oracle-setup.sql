-- Oracle Database Setup Script for Load Testing
-- This script creates users, tables, and sample data for high-load testing
-- This script is idempotent - you can run it multiple times

-- ==================================================
-- STEP 0: Drop existing users if they exist (cleanup)
-- Run these commands as SYSDBA
-- ==================================================

-- Drop users if they exist (CASCADE removes all their objects)
BEGIN
  EXECUTE IMMEDIATE 'DROP USER oltp_user CASCADE';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1918 THEN -- -1918 = user does not exist
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'DROP USER analytics_user CASCADE';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1918 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'DROP USER otel_monitor CASCADE';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1918 THEN
      RAISE;
    END IF;
END;
/

-- ==================================================
-- STEP 1: Create dedicated users for the applications
-- Run these commands as SYSDBA
-- ==================================================

-- Create App User 1 (for OLTP application)
CREATE USER oltp_user IDENTIFIED BY "OltpPass123!";

GRANT CONNECT, RESOURCE TO oltp_user;
GRANT CREATE SESSION TO oltp_user;
GRANT CREATE TABLE TO oltp_user;
GRANT CREATE VIEW TO oltp_user;
GRANT CREATE SEQUENCE TO oltp_user;
GRANT CREATE PROCEDURE TO oltp_user;

-- Grant unlimited quota on the default tablespace
-- Note: Adjust tablespace name if needed (check with: SELECT property_value FROM database_properties WHERE property_name = 'DEFAULT_PERMANENT_TABLESPACE';)
DECLARE
  v_tablespace VARCHAR2(30);
BEGIN
  SELECT property_value INTO v_tablespace
  FROM database_properties
  WHERE property_name = 'DEFAULT_PERMANENT_TABLESPACE';
  EXECUTE IMMEDIATE 'ALTER USER oltp_user QUOTA UNLIMITED ON ' || v_tablespace;
END;
/

-- Create App User 2 (for Analytics application)
CREATE USER analytics_user IDENTIFIED BY "AnalyticsPass123!";

GRANT CONNECT, RESOURCE TO analytics_user;
GRANT CREATE SESSION TO analytics_user;
GRANT CREATE TABLE TO analytics_user;
GRANT CREATE VIEW TO analytics_user;
GRANT CREATE SEQUENCE TO analytics_user;
GRANT CREATE PROCEDURE TO analytics_user;

-- Grant unlimited quota on the default tablespace
DECLARE
  v_tablespace VARCHAR2(30);
BEGIN
  SELECT property_value INTO v_tablespace
  FROM database_properties
  WHERE property_name = 'DEFAULT_PERMANENT_TABLESPACE';
  EXECUTE IMMEDIATE 'ALTER USER analytics_user QUOTA UNLIMITED ON ' || v_tablespace;
END;
/

-- ==================================================
-- STEP 2: Create tables under oltp_user
-- Connect as oltp_user to run these
-- ==================================================

-- Customers Table
CREATE TABLE oltp_user.CUSTOMERS (
    customer_id NUMBER PRIMARY KEY,
    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(100) NOT NULL,
    email VARCHAR2(255) UNIQUE NOT NULL,
    phone VARCHAR2(20),
    address VARCHAR2(500),
    city VARCHAR2(100),
    state VARCHAR2(50),
    zip_code VARCHAR2(20),
    country VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    loyalty_points NUMBER DEFAULT 0,
    customer_type VARCHAR2(20) DEFAULT 'REGULAR'
);

CREATE SEQUENCE oltp_user.customer_seq START WITH 1 INCREMENT BY 1;
-- Note: email column already has UNIQUE constraint which creates an index automatically
CREATE INDEX oltp_user.idx_customer_type ON oltp_user.CUSTOMERS(customer_type);
CREATE INDEX oltp_user.idx_customer_created ON oltp_user.CUSTOMERS(created_at);

-- Products Table
CREATE TABLE oltp_user.PRODUCTS (
    product_id NUMBER PRIMARY KEY,
    product_name VARCHAR2(200) NOT NULL,
    category VARCHAR2(100),
    subcategory VARCHAR2(100),
    description VARCHAR2(1000),
    price NUMBER(10, 2) NOT NULL,
    cost NUMBER(10, 2),
    weight NUMBER(10, 3),
    dimensions VARCHAR2(100),
    manufacturer VARCHAR2(200),
    sku VARCHAR2(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active NUMBER(1) DEFAULT 1
);

CREATE SEQUENCE oltp_user.product_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_product_category ON oltp_user.PRODUCTS(category);
-- Note: sku column already has UNIQUE constraint which creates an index automatically
CREATE INDEX oltp_user.idx_product_active ON oltp_user.PRODUCTS(is_active);

-- Inventory Table
CREATE TABLE oltp_user.INVENTORY (
    inventory_id NUMBER PRIMARY KEY,
    product_id NUMBER NOT NULL,
    warehouse_location VARCHAR2(100),
    quantity_available NUMBER NOT NULL,
    quantity_reserved NUMBER DEFAULT 0,
    reorder_level NUMBER,
    last_restock_date TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES oltp_user.PRODUCTS(product_id)
);

CREATE SEQUENCE oltp_user.inventory_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_inv_product ON oltp_user.INVENTORY(product_id);
CREATE INDEX oltp_user.idx_inv_location ON oltp_user.INVENTORY(warehouse_location);

-- Orders Table
CREATE TABLE oltp_user.ORDERS (
    order_id NUMBER PRIMARY KEY,
    customer_id NUMBER NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR2(50) NOT NULL,
    total_amount NUMBER(12, 2),
    tax_amount NUMBER(10, 2),
    shipping_cost NUMBER(8, 2),
    payment_method VARCHAR2(50),
    shipping_address VARCHAR2(500),
    tracking_number VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES oltp_user.CUSTOMERS(customer_id)
);

CREATE SEQUENCE oltp_user.order_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_order_customer ON oltp_user.ORDERS(customer_id);
CREATE INDEX oltp_user.idx_order_date ON oltp_user.ORDERS(order_date);
CREATE INDEX oltp_user.idx_order_status ON oltp_user.ORDERS(status);

-- Order Items Table
CREATE TABLE oltp_user.ORDER_ITEMS (
    order_item_id NUMBER PRIMARY KEY,
    order_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    quantity NUMBER NOT NULL,
    unit_price NUMBER(10, 2) NOT NULL,
    discount NUMBER(8, 2) DEFAULT 0,
    subtotal NUMBER(12, 2),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES oltp_user.ORDERS(order_id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES oltp_user.PRODUCTS(product_id)
);

CREATE SEQUENCE oltp_user.order_item_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_item_order ON oltp_user.ORDER_ITEMS(order_id);
CREATE INDEX oltp_user.idx_item_product ON oltp_user.ORDER_ITEMS(product_id);

-- Transactions Table (for payment tracking)
CREATE TABLE oltp_user.TRANSACTIONS (
    transaction_id NUMBER PRIMARY KEY,
    order_id NUMBER NOT NULL,
    transaction_type VARCHAR2(50),
    amount NUMBER(12, 2),
    currency VARCHAR2(10) DEFAULT 'USD',
    payment_gateway VARCHAR2(100),
    gateway_transaction_id VARCHAR2(200),
    status VARCHAR2(50),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message VARCHAR2(500),
    CONSTRAINT fk_trans_order FOREIGN KEY (order_id) REFERENCES oltp_user.ORDERS(order_id)
);

CREATE SEQUENCE oltp_user.transaction_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_trans_order ON oltp_user.TRANSACTIONS(order_id);
CREATE INDEX oltp_user.idx_trans_status ON oltp_user.TRANSACTIONS(status);
CREATE INDEX oltp_user.idx_trans_date ON oltp_user.TRANSACTIONS(processed_at);

-- Audit Log Table
CREATE TABLE oltp_user.AUDIT_LOG (
    audit_id NUMBER PRIMARY KEY,
    table_name VARCHAR2(100),
    operation VARCHAR2(20),
    record_id NUMBER,
    old_value CLOB,
    new_value CLOB,
    changed_by VARCHAR2(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE oltp_user.audit_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX oltp_user.idx_audit_table ON oltp_user.AUDIT_LOG(table_name);
CREATE INDEX oltp_user.idx_audit_date ON oltp_user.AUDIT_LOG(changed_at);

-- Session Data Table (for web session tracking)
CREATE TABLE oltp_user.SESSION_DATA (
    session_id VARCHAR2(100) PRIMARY KEY,
    customer_id NUMBER,
    login_time TIMESTAMP,
    last_activity TIMESTAMP,
    ip_address VARCHAR2(50),
    user_agent VARCHAR2(500),
    session_data CLOB,
    is_active NUMBER(1) DEFAULT 1
);

CREATE INDEX oltp_user.idx_session_customer ON oltp_user.SESSION_DATA(customer_id);
CREATE INDEX oltp_user.idx_session_active ON oltp_user.SESSION_DATA(is_active);

-- ==================================================
-- STEP 3: Create analytics tables under analytics_user
-- Connect as analytics_user to run these
-- ==================================================

-- Aggregated Sales Summary
CREATE TABLE analytics_user.SALES_SUMMARY (
    summary_id NUMBER PRIMARY KEY,
    summary_date DATE,
    total_orders NUMBER,
    total_revenue NUMBER(15, 2),
    total_customers NUMBER,
    avg_order_value NUMBER(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE analytics_user.sales_summary_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX analytics_user.idx_sales_date ON analytics_user.SALES_SUMMARY(summary_date);

-- Customer Analytics
CREATE TABLE analytics_user.CUSTOMER_ANALYTICS (
    analytics_id NUMBER PRIMARY KEY,
    customer_id NUMBER,
    total_orders NUMBER,
    total_spent NUMBER(12, 2),
    avg_order_value NUMBER(10, 2),
    last_order_date TIMESTAMP,
    customer_segment VARCHAR2(50),
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE analytics_user.customer_analytics_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX analytics_user.idx_cust_analytics ON analytics_user.CUSTOMER_ANALYTICS(customer_id);

-- Product Performance
CREATE TABLE analytics_user.PRODUCT_PERFORMANCE (
    performance_id NUMBER PRIMARY KEY,
    product_id NUMBER,
    period_start DATE,
    period_end DATE,
    units_sold NUMBER,
    revenue NUMBER(12, 2),
    profit NUMBER(12, 2),
    return_count NUMBER,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE analytics_user.product_perf_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX analytics_user.idx_prod_perf ON analytics_user.PRODUCT_PERFORMANCE(product_id);

-- ==================================================
-- STEP 4: Insert sample seed data (run as oltp_user)
-- ==================================================

-- Insert sample customers (1000 records)
BEGIN
    FOR i IN 1..1000 LOOP
        INSERT INTO oltp_user.CUSTOMERS (
            customer_id, first_name, last_name, email, phone,
            city, state, country, customer_type, loyalty_points
        ) VALUES (
            oltp_user.customer_seq.NEXTVAL,
            'FirstName' || i,
            'LastName' || i,
            'customer' || i || '@example.com',
            '555-' || LPAD(i, 7, '0'),
            'City' || MOD(i, 50),
            'State' || MOD(i, 50),
            'USA',
            CASE WHEN MOD(i, 10) = 0 THEN 'PREMIUM' ELSE 'REGULAR' END,
            MOD(i * 100, 10000)
        );
    END LOOP;
    COMMIT;
END;
/

-- Insert sample products (500 records)
BEGIN
    FOR i IN 1..500 LOOP
        INSERT INTO oltp_user.PRODUCTS (
            product_id, product_name, category, subcategory,
            price, cost, sku, is_active
        ) VALUES (
            oltp_user.product_seq.NEXTVAL,
            'Product ' || i,
            'Category' || MOD(i, 10),
            'SubCat' || MOD(i, 20),
            19.99 + (i * 1.5),
            10.00 + (i * 0.75),
            'SKU-' || LPAD(i, 8, '0'),
            1
        );
    END LOOP;
    COMMIT;
END;
/

-- Insert inventory records
BEGIN
    FOR i IN 1..500 LOOP
        INSERT INTO oltp_user.INVENTORY (
            inventory_id, product_id, warehouse_location,
            quantity_available, reorder_level
        ) VALUES (
            oltp_user.inventory_seq.NEXTVAL,
            i,
            'WH-' || MOD(i, 5),
            1000 + MOD(i * 137, 5000),
            100
        );
    END LOOP;
    COMMIT;
END;
/

-- ==================================================
-- STEP 5: Grant cross-schema access for analytics user
-- Run as SYSDBA (or as oltp_user if they have grant privileges)
-- ==================================================

-- Grant analytics_user read access to oltp_user tables
GRANT SELECT ON oltp_user.CUSTOMERS TO analytics_user;
GRANT SELECT ON oltp_user.ORDERS TO analytics_user;
GRANT SELECT ON oltp_user.ORDER_ITEMS TO analytics_user;
GRANT SELECT ON oltp_user.PRODUCTS TO analytics_user;
GRANT SELECT ON oltp_user.INVENTORY TO analytics_user;
GRANT SELECT ON oltp_user.TRANSACTIONS TO analytics_user;

-- ==================================================
-- STEP 6: Grant monitoring privileges for OTEL receiver
-- Run as SYSDBA
-- ==================================================

-- Create monitoring user for OTEL Oracle receiver
CREATE USER otel_monitor IDENTIFIED BY "OtelMonitor123!";

GRANT CONNECT TO otel_monitor;
GRANT SELECT_CATALOG_ROLE TO otel_monitor;
GRANT SELECT ANY DICTIONARY TO otel_monitor;

-- Grant specific monitoring views
GRANT SELECT ON v_$session TO otel_monitor;
GRANT SELECT ON v_$sesstat TO otel_monitor;
GRANT SELECT ON v_$statname TO otel_monitor;
GRANT SELECT ON v_$sysstat TO otel_monitor;
GRANT SELECT ON v_$sql TO otel_monitor;
GRANT SELECT ON v_$sql_plan TO otel_monitor;
GRANT SELECT ON v_$sqlarea TO otel_monitor;
GRANT SELECT ON v_$sqlstats TO otel_monitor;
GRANT SELECT ON v_$database TO otel_monitor;
GRANT SELECT ON v_$instance TO otel_monitor;
GRANT SELECT ON v_$datafile TO otel_monitor;
GRANT SELECT ON v_$tablespace TO otel_monitor;
GRANT SELECT ON dba_tablespaces TO otel_monitor;
GRANT SELECT ON dba_data_files TO otel_monitor;
GRANT SELECT ON dba_free_space TO otel_monitor;
GRANT SELECT ON v_$system_event TO otel_monitor;
GRANT SELECT ON v_$session_wait TO otel_monitor;
GRANT SELECT ON v_$system_wait_class TO otel_monitor;
GRANT SELECT ON v_$waitstat TO otel_monitor;
GRANT SELECT ON v_$enqueue_stat TO otel_monitor;
GRANT SELECT ON v_$filestat TO otel_monitor;
GRANT SELECT ON v_$tempstat TO otel_monitor;
GRANT SELECT ON v_$segment_statistics TO otel_monitor;
GRANT SELECT ON v_$lock TO otel_monitor;
GRANT SELECT ON v_$locked_object TO otel_monitor;
GRANT SELECT ON v_$transaction TO otel_monitor;
GRANT SELECT ON v_$rollstat TO otel_monitor;
GRANT SELECT ON v_$undostat TO otel_monitor;
GRANT SELECT ON v_$sysmetric TO otel_monitor;
GRANT SELECT ON v_$sysmetric_history TO otel_monitor;

COMMIT;

-- ==================================================
-- STEP 7: Create Stored Procedures under oltp_user
-- ==================================================

-- Grant cross-schema INSERT for daily sales summary procedure
GRANT INSERT ON analytics_user.SALES_SUMMARY TO oltp_user;
GRANT SELECT ON analytics_user.sales_summary_seq TO oltp_user;

-- Procedure 1: Atomic order creation with items
CREATE OR REPLACE PROCEDURE oltp_user.proc_create_order_with_items(
    p_customer_id IN NUMBER,
    p_num_items   IN NUMBER,
    p_order_id    OUT NUMBER,
    p_total       OUT NUMBER
) AS
    v_product_id NUMBER;
    v_price      NUMBER(10,2);
    v_quantity   NUMBER;
    v_subtotal   NUMBER(12,2);
BEGIN
    SELECT oltp_user.order_seq.NEXTVAL INTO p_order_id FROM DUAL;

    INSERT INTO oltp_user.ORDERS (order_id, customer_id, order_date, status, payment_method, created_at)
    VALUES (p_order_id, p_customer_id, CURRENT_TIMESTAMP, 'PENDING', 'CREDIT_CARD', CURRENT_TIMESTAMP);

    p_total := 0;

    FOR i IN 1..p_num_items LOOP
        v_product_id := TRUNC(DBMS_RANDOM.VALUE(1, 501));
        v_quantity := TRUNC(DBMS_RANDOM.VALUE(1, 6));

        BEGIN
            SELECT price INTO v_price FROM oltp_user.PRODUCTS WHERE product_id = v_product_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                v_price := 29.99;
        END;

        v_subtotal := v_price * v_quantity;

        INSERT INTO oltp_user.ORDER_ITEMS (order_item_id, order_id, product_id, quantity, unit_price, subtotal)
        VALUES (oltp_user.order_item_seq.NEXTVAL, p_order_id, v_product_id, v_quantity, v_price, v_subtotal);

        p_total := p_total + v_subtotal;
    END LOOP;

    UPDATE oltp_user.ORDERS
    SET total_amount = p_total,
        tax_amount = p_total * 0.08,
        updated_at = CURRENT_TIMESTAMP
    WHERE order_id = p_order_id;

    INSERT INTO oltp_user.AUDIT_LOG (audit_id, table_name, operation, record_id, changed_by, changed_at)
    VALUES (oltp_user.audit_seq.NEXTVAL, 'ORDERS', 'INSERT', p_order_id, 'STORED_PROC', CURRENT_TIMESTAMP);

    COMMIT;
END;
/

-- Procedure 2: Restock low inventory
CREATE OR REPLACE PROCEDURE oltp_user.proc_restock_low_inventory(
    p_restock_quantity IN NUMBER DEFAULT 500,
    p_items_restocked  OUT NUMBER
) AS
BEGIN
    UPDATE oltp_user.INVENTORY
    SET quantity_available = quantity_available + p_restock_quantity,
        last_restock_date = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE quantity_available < reorder_level;

    p_items_restocked := SQL%ROWCOUNT;
    COMMIT;
END;
/

-- Procedure 3: Calculate customer loyalty tier
CREATE OR REPLACE PROCEDURE oltp_user.proc_calculate_customer_loyalty(
    p_customer_id IN NUMBER,
    p_new_points  OUT NUMBER,
    p_tier        OUT VARCHAR2
) AS
    v_total_spent  NUMBER(12,2);
    v_order_count  NUMBER;
BEGIN
    SELECT NVL(SUM(total_amount), 0), COUNT(*)
    INTO v_total_spent, v_order_count
    FROM oltp_user.ORDERS
    WHERE customer_id = p_customer_id
      AND status IN ('COMPLETED', 'DELIVERED');

    p_new_points := TRUNC(v_total_spent * 10) + (v_order_count * 50);

    IF p_new_points >= 50000 THEN
        p_tier := 'PLATINUM';
    ELSIF p_new_points >= 20000 THEN
        p_tier := 'GOLD';
    ELSIF p_new_points >= 5000 THEN
        p_tier := 'SILVER';
    ELSE
        p_tier := 'BRONZE';
    END IF;

    UPDATE oltp_user.CUSTOMERS
    SET loyalty_points = p_new_points,
        customer_type = p_tier,
        updated_at = CURRENT_TIMESTAMP
    WHERE customer_id = p_customer_id;

    COMMIT;
END;
/

-- Procedure 4: Purge old data (cascading delete)
CREATE OR REPLACE PROCEDURE oltp_user.proc_purge_old_data(
    p_days_to_keep   IN NUMBER,
    p_orders_deleted OUT NUMBER,
    p_items_deleted  OUT NUMBER,
    p_logs_deleted   OUT NUMBER
) AS
BEGIN
    DELETE FROM oltp_user.ORDER_ITEMS
    WHERE order_id IN (
        SELECT order_id FROM oltp_user.ORDERS
        WHERE status IN ('COMPLETED', 'DELIVERED')
          AND order_date < SYSDATE - p_days_to_keep
    );
    p_items_deleted := SQL%ROWCOUNT;

    DELETE FROM oltp_user.TRANSACTIONS
    WHERE order_id IN (
        SELECT order_id FROM oltp_user.ORDERS
        WHERE status IN ('COMPLETED', 'DELIVERED')
          AND order_date < SYSDATE - p_days_to_keep
    );

    DELETE FROM oltp_user.ORDERS
    WHERE status IN ('COMPLETED', 'DELIVERED')
      AND order_date < SYSDATE - p_days_to_keep;
    p_orders_deleted := SQL%ROWCOUNT;

    DELETE FROM oltp_user.AUDIT_LOG
    WHERE changed_at < SYSDATE - p_days_to_keep;
    p_logs_deleted := SQL%ROWCOUNT;

    COMMIT;
END;
/

-- Procedure 5: Daily sales summary (cross-schema)
CREATE OR REPLACE PROCEDURE oltp_user.proc_daily_sales_summary(
    p_summary_date  IN DATE DEFAULT TRUNC(SYSDATE),
    p_total_orders  OUT NUMBER,
    p_total_revenue OUT NUMBER
) AS
    v_total_customers NUMBER;
    v_avg_order_value NUMBER(10,2);
BEGIN
    SELECT COUNT(*), NVL(SUM(total_amount), 0), COUNT(DISTINCT customer_id)
    INTO p_total_orders, p_total_revenue, v_total_customers
    FROM oltp_user.ORDERS
    WHERE TRUNC(order_date) = p_summary_date
      AND status NOT IN ('CANCELLED', 'PAYMENT_FAILED');

    IF p_total_orders > 0 THEN
        v_avg_order_value := p_total_revenue / p_total_orders;
    ELSE
        v_avg_order_value := 0;
    END IF;

    DELETE FROM analytics_user.SALES_SUMMARY WHERE summary_date = p_summary_date;

    INSERT INTO analytics_user.SALES_SUMMARY (
        summary_id, summary_date, total_orders, total_revenue,
        total_customers, avg_order_value, created_at
    ) VALUES (
        analytics_user.sales_summary_seq.NEXTVAL, p_summary_date, p_total_orders,
        p_total_revenue, v_total_customers, v_avg_order_value, CURRENT_TIMESTAMP
    );

    COMMIT;
END;
/

-- Grant execute on procedures to analytics_user
GRANT EXECUTE ON oltp_user.proc_daily_sales_summary TO analytics_user;

-- ============================================================
-- STEP 8: Analytics Stored Procedures (under analytics_user)
-- ============================================================

-- Procedure 1: Sales Trend Analysis - Rolling averages and growth rates
CREATE OR REPLACE PROCEDURE analytics_user.proc_sales_trend_analysis(
    p_days_back IN NUMBER DEFAULT 30,
    p_total_revenue OUT NUMBER,
    p_avg_daily_revenue OUT NUMBER,
    p_order_count OUT NUMBER
) AS
    v_min_date DATE;
BEGIN
    v_min_date := TRUNC(SYSDATE) - p_days_back;

    SELECT NVL(SUM(daily_total), 0),
           NVL(AVG(daily_total), 0),
           NVL(SUM(daily_count), 0)
    INTO p_total_revenue, p_avg_daily_revenue, p_order_count
    FROM (
        SELECT TRUNC(order_date) AS order_day,
               SUM(total_amount) AS daily_total,
               COUNT(*) AS daily_count
        FROM oltp_user.ORDERS
        WHERE order_date >= v_min_date
          AND status IN ('COMPLETED', 'SHIPPED')
        GROUP BY TRUNC(order_date)
    );

    MERGE INTO analytics_user.SALES_SUMMARY ss
    USING (SELECT TRUNC(SYSDATE) AS summary_date FROM DUAL) src
    ON (ss.summary_date = src.summary_date)
    WHEN MATCHED THEN
        UPDATE SET total_revenue = p_total_revenue,
                   total_orders = p_order_count,
                   avg_order_value = CASE WHEN p_order_count > 0 THEN p_total_revenue / p_order_count ELSE 0 END
    WHEN NOT MATCHED THEN
        INSERT (summary_id, summary_date, total_orders, total_revenue, total_customers, avg_order_value, created_at)
        VALUES (analytics_user.sales_summary_seq.NEXTVAL, TRUNC(SYSDATE), p_order_count, p_total_revenue, 0,
                CASE WHEN p_order_count > 0 THEN p_total_revenue / p_order_count ELSE 0 END, CURRENT_TIMESTAMP);

    COMMIT;
END;
/

-- Procedure 2: Customer Cohort Analysis - Segments customers by activity
CREATE OR REPLACE PROCEDURE analytics_user.proc_customer_cohort_analysis(
    p_months_back IN NUMBER DEFAULT 6,
    p_active_customers OUT NUMBER,
    p_churned_customers OUT NUMBER,
    p_new_customers OUT NUMBER
) AS
    v_cutoff_date DATE;
    v_new_cutoff DATE;
BEGIN
    v_cutoff_date := ADD_MONTHS(TRUNC(SYSDATE), -p_months_back);
    v_new_cutoff := ADD_MONTHS(TRUNC(SYSDATE), -1);

    SELECT COUNT(DISTINCT customer_id)
    INTO p_active_customers
    FROM oltp_user.ORDERS
    WHERE order_date >= v_cutoff_date;

    SELECT COUNT(*)
    INTO p_churned_customers
    FROM oltp_user.CUSTOMERS c
    WHERE EXISTS (SELECT 1 FROM oltp_user.ORDERS o WHERE o.customer_id = c.customer_id)
      AND NOT EXISTS (SELECT 1 FROM oltp_user.ORDERS o WHERE o.customer_id = c.customer_id AND o.order_date >= v_cutoff_date);

    SELECT COUNT(*)
    INTO p_new_customers
    FROM oltp_user.CUSTOMERS
    WHERE created_at >= v_new_cutoff;
END;
/

-- Procedure 3: Product Recommendation Score - Finds co-purchased products
CREATE OR REPLACE PROCEDURE analytics_user.proc_product_recommendation(
    p_product_id IN NUMBER,
    p_recommendations OUT NUMBER
) AS
BEGIN
    SELECT COUNT(DISTINCT oi2.product_id)
    INTO p_recommendations
    FROM oltp_user.ORDER_ITEMS oi1
    JOIN oltp_user.ORDER_ITEMS oi2 ON oi1.order_id = oi2.order_id AND oi1.product_id != oi2.product_id
    WHERE oi1.product_id = p_product_id;

    MERGE INTO analytics_user.PRODUCT_PERFORMANCE pp
    USING (SELECT p_product_id AS product_id FROM DUAL) src
    ON (pp.product_id = src.product_id)
    WHEN MATCHED THEN
        UPDATE SET recommendation_score = p_recommendations,
                   last_analyzed = CURRENT_TIMESTAMP
    WHEN NOT MATCHED THEN
        INSERT (perf_id, product_id, total_sold, total_revenue, avg_rating, recommendation_score, last_analyzed)
        VALUES (analytics_user.product_perf_seq.NEXTVAL, p_product_id, 0, 0, 0, p_recommendations, CURRENT_TIMESTAMP);

    COMMIT;
END;
/

-- Procedure 4: Inventory Forecast - Predicts reorder needs based on sales velocity
CREATE OR REPLACE PROCEDURE analytics_user.proc_inventory_forecast(
    p_days_horizon IN NUMBER DEFAULT 14,
    p_items_at_risk OUT NUMBER,
    p_total_items_checked OUT NUMBER
) AS
BEGIN
    p_items_at_risk := 0;
    p_total_items_checked := 0;

    SELECT COUNT(*),
           NVL(SUM(CASE WHEN current_qty < (daily_sales * p_days_horizon) THEN 1 ELSE 0 END), 0)
    INTO p_total_items_checked, p_items_at_risk
    FROM (
        SELECT i.product_id,
               i.quantity AS current_qty,
               NVL(sales.avg_daily, 0) AS daily_sales
        FROM oltp_user.INVENTORY i
        LEFT JOIN (
            SELECT oi.product_id,
                   SUM(oi.quantity) / GREATEST(1, (SYSDATE - MIN(o.order_date))) AS avg_daily
            FROM oltp_user.ORDER_ITEMS oi
            JOIN oltp_user.ORDERS o ON oi.order_id = o.order_id
            WHERE o.order_date >= SYSDATE - 30
            GROUP BY oi.product_id
        ) sales ON i.product_id = sales.product_id
    );
END;
/

-- Procedure 5: Revenue Reconciliation - Cross-checks orders vs transactions
CREATE OR REPLACE PROCEDURE analytics_user.proc_revenue_reconciliation(
    p_days_back IN NUMBER DEFAULT 7,
    p_order_total OUT NUMBER,
    p_transaction_total OUT NUMBER,
    p_discrepancy OUT NUMBER
) AS
    v_start_date DATE;
BEGIN
    v_start_date := TRUNC(SYSDATE) - p_days_back;

    SELECT NVL(SUM(total_amount), 0)
    INTO p_order_total
    FROM oltp_user.ORDERS
    WHERE order_date >= v_start_date
      AND status IN ('COMPLETED', 'SHIPPED');

    SELECT NVL(SUM(amount), 0)
    INTO p_transaction_total
    FROM oltp_user.TRANSACTIONS
    WHERE transaction_date >= v_start_date
      AND status = 'COMPLETED';

    p_discrepancy := p_order_total - p_transaction_total;
END;
/
