-- Oracle Database Setup Script for Load Testing
-- This script creates users, tables, and sample data for high-load testing

-- ==================================================
-- STEP 1: Create dedicated users for the applications
-- Run these commands as SYSDBA
-- ==================================================

-- Create App User 1 (for OLTP application)
CREATE USER oltp_user IDENTIFIED BY "OltpPass123!"
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE TO oltp_user;
GRANT CREATE SESSION TO oltp_user;
GRANT CREATE TABLE TO oltp_user;
GRANT CREATE VIEW TO oltp_user;
GRANT CREATE SEQUENCE TO oltp_user;
GRANT CREATE PROCEDURE TO oltp_user;

-- Create App User 2 (for Analytics application)
CREATE USER analytics_user IDENTIFIED BY "AnalyticsPass123!"
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE TO analytics_user;
GRANT CREATE SESSION TO analytics_user;
GRANT CREATE TABLE TO analytics_user;
GRANT CREATE VIEW TO analytics_user;
GRANT CREATE SEQUENCE TO analytics_user;
GRANT CREATE PROCEDURE TO analytics_user;

-- Grant cross-schema access for analytics
GRANT SELECT ON oltp_user.CUSTOMERS TO analytics_user;
GRANT SELECT ON oltp_user.ORDERS TO analytics_user;
GRANT SELECT ON oltp_user.ORDER_ITEMS TO analytics_user;
GRANT SELECT ON oltp_user.PRODUCTS TO analytics_user;
GRANT SELECT ON oltp_user.INVENTORY TO analytics_user;
GRANT SELECT ON oltp_user.TRANSACTIONS TO analytics_user;

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
CREATE INDEX oltp_user.idx_customer_email ON oltp_user.CUSTOMERS(email);
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
CREATE INDEX oltp_user.idx_product_sku ON oltp_user.PRODUCTS(sku);
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
-- STEP 5: Grant monitoring privileges for OTEL receiver
-- Run as SYSDBA
-- ==================================================

-- Create monitoring user for OTEL Oracle receiver
CREATE USER otel_monitor IDENTIFIED BY "OtelMonitor123!"
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP;

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
