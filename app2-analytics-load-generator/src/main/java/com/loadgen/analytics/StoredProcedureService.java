package com.loadgen.analytics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
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

    @Trace
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
            NewRelic.addCustomParameter("proc_sales_trend_days_back", daysBack);
            NewRelic.addCustomParameter("proc_sales_trend_total_revenue", totalRevenue);
            NewRelic.addCustomParameter("proc_sales_trend_order_count", orderCount);
            logger.debug("SalesTrendAnalysis: revenue={}, avgDaily={}, orders={}, duration={}ms",
                    totalRevenue, avgDailyRevenue, orderCount, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_sales_trend_analysis", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
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
            NewRelic.addCustomParameter("proc_cohort_months_back", monthsBack);
            NewRelic.addCustomParameter("proc_cohort_active", activeCustomers);
            NewRelic.addCustomParameter("proc_cohort_churned", churnedCustomers);
            NewRelic.addCustomParameter("proc_cohort_new", newCustomers);
            logger.debug("CustomerCohortAnalysis: active={}, churned={}, new={}, duration={}ms",
                    activeCustomers, churnedCustomers, newCustomers, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_customer_cohort_analysis", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
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
            NewRelic.addCustomParameter("proc_recommendation_product_id", productId);
            NewRelic.addCustomParameter("proc_recommendation_count", recommendations);
            logger.debug("ProductRecommendation: productId={}, recommendations={}, duration={}ms",
                    productId, recommendations, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_product_recommendation", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
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
            NewRelic.addCustomParameter("proc_forecast_horizon_days", daysHorizon);
            NewRelic.addCustomParameter("proc_forecast_items_at_risk", itemsAtRisk);
            NewRelic.addCustomParameter("proc_forecast_total_checked", totalChecked);
            logger.debug("InventoryForecast: atRisk={}, total={}, duration={}ms",
                    itemsAtRisk, totalChecked, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_inventory_forecast", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }

    @Trace
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
            NewRelic.addCustomParameter("proc_reconciliation_days_back", daysBack);
            NewRelic.addCustomParameter("proc_reconciliation_order_total", orderTotal);
            NewRelic.addCustomParameter("proc_reconciliation_discrepancy", discrepancy);
            logger.debug("RevenueReconciliation: orders={}, transactions={}, discrepancy={}, duration={}ms",
                    orderTotal, transactionTotal, discrepancy, duration);

        } catch (Exception e) {
            logger.error("Error executing proc_revenue_reconciliation", e);
            NewRelic.noticeError(e);
            throw new RuntimeException(e);
        }
    }
}
