package com.loadgen.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@Service
public class StoredProcedureService {
    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureService.class);
    private final DatabaseManager dbManager;

    public StoredProcedureService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void executeSalesTrendAnalysis(int daysBack) {
        String sql = "{call analytics_user.proc_sales_trend_analysis(?, ?, ?, ?)}";
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, daysBack);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.registerOutParameter(4, Types.NUMERIC);
            cstmt.execute();

            double totalRevenue = cstmt.getDouble(2);
            double avgDailyRevenue = cstmt.getDouble(3);
            long orderCount = cstmt.getLong(4);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("SalesTrendAnalysis: revenue={}, avgDaily={}, orders={}, duration={}ms",
                    totalRevenue, avgDailyRevenue, orderCount, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_sales_trend_analysis", e);
            throw new RuntimeException(e);
        }
    }

    public void executeCustomerCohortAnalysis(int monthsBack) {
        String sql = "{call analytics_user.proc_customer_cohort_analysis(?, ?, ?, ?)}";
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, monthsBack);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.registerOutParameter(4, Types.NUMERIC);
            cstmt.execute();

            long activeCustomers = cstmt.getLong(2);
            long churnedCustomers = cstmt.getLong(3);
            long newCustomers = cstmt.getLong(4);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("CustomerCohortAnalysis: active={}, churned={}, new={}, duration={}ms",
                    activeCustomers, churnedCustomers, newCustomers, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_customer_cohort_analysis", e);
            throw new RuntimeException(e);
        }
    }

    public void executeProductRecommendation(long productId) {
        String sql = "{call analytics_user.proc_product_recommendation(?, ?)}";
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setLong(1, productId);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.execute();

            long recommendations = cstmt.getLong(2);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("ProductRecommendation: productId={}, recommendations={}, duration={}ms",
                    productId, recommendations, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_product_recommendation", e);
            throw new RuntimeException(e);
        }
    }

    public void executeInventoryForecast(int daysHorizon) {
        String sql = "{call analytics_user.proc_inventory_forecast(?, ?, ?)}";
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, daysHorizon);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.execute();

            long itemsAtRisk = cstmt.getLong(2);
            long totalChecked = cstmt.getLong(3);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("InventoryForecast: atRisk={}, total={}, duration={}ms",
                    itemsAtRisk, totalChecked, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_inventory_forecast", e);
            throw new RuntimeException(e);
        }
    }

    public void executeRevenueReconciliation(int daysBack) {
        String sql = "{call analytics_user.proc_revenue_reconciliation(?, ?, ?, ?)}";
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, daysBack);
            cstmt.registerOutParameter(2, Types.NUMERIC);
            cstmt.registerOutParameter(3, Types.NUMERIC);
            cstmt.registerOutParameter(4, Types.NUMERIC);
            cstmt.execute();

            double orderTotal = cstmt.getDouble(2);
            double transactionTotal = cstmt.getDouble(3);
            double discrepancy = cstmt.getDouble(4);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("RevenueReconciliation: orders={}, transactions={}, discrepancy={}, duration={}ms",
                    orderTotal, transactionTotal, discrepancy, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_revenue_reconciliation", e);
            throw new RuntimeException(e);
        }
    }
}
