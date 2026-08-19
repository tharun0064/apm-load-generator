package com.loadgen.analytics.controller;

import com.loadgen.analytics.StoredProcedureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/procedures")
public class StoredProcedureController {
    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureController.class);
    private final StoredProcedureService storedProcedureService;

    public StoredProcedureController(StoredProcedureService storedProcedureService) {
        this.storedProcedureService = storedProcedureService;
    }

    @PostMapping("/sales-trend")
    public ResponseEntity<Map<String, Object>> salesTrendAnalysis(
            @RequestParam(defaultValue = "30") int daysBack) {
        try {
            storedProcedureService.executeSalesTrendAnalysis(daysBack);
            return createSuccessResponse("sales_trend_analysis");
        } catch (Exception e) {
            return handleError(e, "salesTrendAnalysis");
        }
    }

    @PostMapping("/customer-cohort")
    public ResponseEntity<Map<String, Object>> customerCohortAnalysis(
            @RequestParam(defaultValue = "6") int monthsBack) {
        try {
            storedProcedureService.executeCustomerCohortAnalysis(monthsBack);
            return createSuccessResponse("customer_cohort_analysis");
        } catch (Exception e) {
            return handleError(e, "customerCohortAnalysis");
        }
    }

    @PostMapping("/product-recommendation")
    public ResponseEntity<Map<String, Object>> productRecommendation(
            @RequestParam long productId) {
        try {
            storedProcedureService.executeProductRecommendation(productId);
            return createSuccessResponse("product_recommendation");
        } catch (Exception e) {
            return handleError(e, "productRecommendation");
        }
    }

    @PostMapping("/inventory-forecast")
    public ResponseEntity<Map<String, Object>> inventoryForecast(
            @RequestParam(defaultValue = "14") int daysHorizon) {
        try {
            storedProcedureService.executeInventoryForecast(daysHorizon);
            return createSuccessResponse("inventory_forecast");
        } catch (Exception e) {
            return handleError(e, "inventoryForecast");
        }
    }

    @PostMapping("/revenue-reconciliation")
    public ResponseEntity<Map<String, Object>> revenueReconciliation(
            @RequestParam(defaultValue = "7") int daysBack) {
        try {
            storedProcedureService.executeRevenueReconciliation(daysBack);
            return createSuccessResponse("revenue_reconciliation");
        } catch (Exception e) {
            return handleError(e, "revenueReconciliation");
        }
    }

    private ResponseEntity<Map<String, Object>> createSuccessResponse(String operation) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("operation", operation);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> handleError(Exception e, String operation) {
        logger.error("Error in {}: {}", operation, e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("operation", operation);
        error.put("status", "ERROR");
        return ResponseEntity.internalServerError().body(error);
    }
}
