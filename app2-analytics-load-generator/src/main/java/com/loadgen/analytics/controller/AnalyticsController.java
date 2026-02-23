package com.loadgen.analytics.controller;

import com.loadgen.analytics.*;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for all Analytics operations
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final SalesAnalyticsService salesAnalytics;
    private final CustomerAnalyticsService customerAnalytics;
    private final ProductAnalyticsService productAnalytics;
    private final ReportingService reportingService;
    private final DataWarehouseService dataWarehouseService;
    private final HeavyTransactionService heavyTransactionService;

    public AnalyticsController(SalesAnalyticsService salesAnalytics,
                               CustomerAnalyticsService customerAnalytics,
                               ProductAnalyticsService productAnalytics,
                               ReportingService reportingService,
                               DataWarehouseService dataWarehouseService,
                               HeavyTransactionService heavyTransactionService) {
        this.salesAnalytics = salesAnalytics;
        this.customerAnalytics = customerAnalytics;
        this.productAnalytics = productAnalytics;
        this.reportingService = reportingService;
        this.dataWarehouseService = dataWarehouseService;
        this.heavyTransactionService = heavyTransactionService;
    }

    // ========== Sales Analytics Endpoints ==========

    @GetMapping("/sales/daily")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getDailySales() {
        try {
            salesAnalytics.getDailySalesSummary();
            return createSuccessResponse("daily_sales");
        } catch (Exception e) {
            return handleError(e, "getDailySales");
        }
    }

    @GetMapping("/sales/monthly")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getMonthlySales() {
        try {
            salesAnalytics.getMonthlySalesTrend();
            return createSuccessResponse("monthly_sales");
        } catch (Exception e) {
            return handleError(e, "getMonthlySales");
        }
    }

    @GetMapping("/sales/by-category")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getSalesByCategory() {
        try {
            salesAnalytics.getSalesByCategory();
            return createSuccessResponse("sales_by_category");
        } catch (Exception e) {
            return handleError(e, "getSalesByCategory");
        }
    }

    @GetMapping("/sales/top-products")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getTopProducts(@RequestParam(defaultValue = "20") int limit) {
        try {
            NewRelic.addCustomParameter("limit", limit);
            salesAnalytics.getTopSellingProducts(limit);
            return createSuccessResponse("top_products");
        } catch (Exception e) {
            return handleError(e, "getTopProducts");
        }
    }

    @GetMapping("/sales/by-payment-method")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getRevenueByPaymentMethod() {
        try {
            salesAnalytics.getRevenueByPaymentMethod();
            return createSuccessResponse("revenue_by_payment");
        } catch (Exception e) {
            return handleError(e, "getRevenueByPaymentMethod");
        }
    }

    // ========== Customer Analytics Endpoints ==========

    @GetMapping("/customers/segmentation")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerSegmentation() {
        try {
            customerAnalytics.getCustomerSegmentation();
            return createSuccessResponse("customer_segmentation");
        } catch (Exception e) {
            return handleError(e, "getCustomerSegmentation");
        }
    }

    @GetMapping("/customers/lifetime-value")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerLifetimeValue() {
        try {
            customerAnalytics.getCustomerLifetimeValue();
            return createSuccessResponse("customer_ltv");
        } catch (Exception e) {
            return handleError(e, "getCustomerLifetimeValue");
        }
    }

    @GetMapping("/customers/retention")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerRetention() {
        try {
            customerAnalytics.getCustomerRetentionRate();
            return createSuccessResponse("customer_retention");
        } catch (Exception e) {
            return handleError(e, "getCustomerRetention");
        }
    }

    @GetMapping("/customers/high-value")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getHighValueCustomers(@RequestParam(defaultValue = "50") int limit) {
        try {
            NewRelic.addCustomParameter("limit", limit);
            customerAnalytics.getHighValueCustomers(limit);
            return createSuccessResponse("high_value_customers");
        } catch (Exception e) {
            return handleError(e, "getHighValueCustomers");
        }
    }

    @GetMapping("/customers/purchase-frequency")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerPurchaseFrequency() {
        try {
            customerAnalytics.getCustomerPurchaseFrequency();
            return createSuccessResponse("purchase_frequency");
        } catch (Exception e) {
            return handleError(e, "getCustomerPurchaseFrequency");
        }
    }

    // ========== Product Analytics Endpoints ==========

    @GetMapping("/products/performance")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getProductPerformance() {
        try {
            productAnalytics.getProductPerformanceReport();
            return createSuccessResponse("product_performance");
        } catch (Exception e) {
            return handleError(e, "getProductPerformance");
        }
    }

    @GetMapping("/products/inventory-turnover")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getInventoryTurnover() {
        try {
            productAnalytics.getInventoryTurnoverRate();
            return createSuccessResponse("inventory_turnover");
        } catch (Exception e) {
            return handleError(e, "getInventoryTurnover");
        }
    }

    @GetMapping("/products/slow-moving")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getSlowMovingProducts() {
        try {
            productAnalytics.getSlowMovingProducts();
            return createSuccessResponse("slow_moving");
        } catch (Exception e) {
            return handleError(e, "getSlowMovingProducts");
        }
    }

    @GetMapping("/products/profit-margin")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getProfitMargin() {
        try {
            productAnalytics.getProfitMarginByCategory();
            return createSuccessResponse("profit_margin");
        } catch (Exception e) {
            return handleError(e, "getProfitMargin");
        }
    }

    @GetMapping("/products/affinity")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getProductAffinity() {
        try {
            productAnalytics.getProductAffinityAnalysis();
            return createSuccessResponse("product_affinity");
        } catch (Exception e) {
            return handleError(e, "getProductAffinity");
        }
    }

    // ========== Reporting Endpoints ==========

    @GetMapping("/reports/executive")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getExecutiveDashboard() {
        try {
            reportingService.generateExecutiveDashboard();
            return createSuccessResponse("executive_dashboard");
        } catch (Exception e) {
            return handleError(e, "getExecutiveDashboard");
        }
    }

    @GetMapping("/reports/sales")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getSalesReport() {
        try {
            reportingService.generateSalesReport();
            return createSuccessResponse("sales_report");
        } catch (Exception e) {
            return handleError(e, "getSalesReport");
        }
    }

    @GetMapping("/reports/inventory")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getInventoryReport() {
        try {
            reportingService.generateInventoryReport();
            return createSuccessResponse("inventory_report");
        } catch (Exception e) {
            return handleError(e, "getInventoryReport");
        }
    }

    @GetMapping("/reports/customer")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerReport() {
        try {
            reportingService.generateCustomerReport();
            return createSuccessResponse("customer_report");
        } catch (Exception e) {
            return handleError(e, "getCustomerReport");
        }
    }

    // ========== Data Warehouse Endpoints ==========

    @PostMapping("/warehouse/aggregate-sales")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> aggregateSales() {
        try {
            dataWarehouseService.aggregateSalesData();
            return createSuccessResponse("aggregate_sales");
        } catch (Exception e) {
            return handleError(e, "aggregateSales");
        }
    }

    @PostMapping("/warehouse/aggregate-customers")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> aggregateCustomers() {
        try {
            dataWarehouseService.aggregateCustomerData();
            return createSuccessResponse("aggregate_customers");
        } catch (Exception e) {
            return handleError(e, "aggregateCustomers");
        }
    }

    @PostMapping("/warehouse/aggregate-products")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> aggregateProducts() {
        try {
            dataWarehouseService.aggregateProductData();
            return createSuccessResponse("aggregate_products");
        } catch (Exception e) {
            return handleError(e, "aggregateProducts");
        }
    }

    @GetMapping("/warehouse/full-scan")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> performFullScan() {
        try {
            dataWarehouseService.performFullTableScan();
            return createSuccessResponse("full_table_scan");
        } catch (Exception e) {
            return handleError(e, "performFullScan");
        }
    }

    @GetMapping("/warehouse/complex-join")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> performComplexJoin() {
        try {
            dataWarehouseService.performComplexJoinQuery();
            return createSuccessResponse("complex_join");
        } catch (Exception e) {
            return handleError(e, "performComplexJoin");
        }
    }

    // ========== Customer Data Endpoint ==========

    @GetMapping("/customer-data")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> getCustomerData() {
        try {
            heavyTransactionService.performHeavyTransaction();
            return createSuccessResponse("customer_data");
        } catch (Exception e) {
            return handleError(e, "getCustomerData");
        }
    }

    // ========== Helper Methods ==========

    private ResponseEntity<Map<String, Object>> createSuccessResponse(String operation) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("operation", operation);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> handleError(Exception e, String operation) {
        logger.error("Error in {}: {}", operation, e.getMessage());
        NewRelic.noticeError(e);
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("operation", operation);
        error.put("status", "ERROR");
        return ResponseEntity.internalServerError().body(error);
    }
}
