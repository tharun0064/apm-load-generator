package com.loadgen.oltp.controller;

import com.loadgen.oltp.StoredProcedureService;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/procedures")
public class StoredProcedureController {
    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureController.class);
    private final StoredProcedureService storedProcedureService;

    public StoredProcedureController(StoredProcedureService storedProcedureService) {
        this.storedProcedureService = storedProcedureService;
    }

    @PostMapping("/create-order")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> createOrderWithItems(
            @RequestParam long customerId,
            @RequestParam(defaultValue = "3") int numItems) {
        try {
            NewRelic.setTransactionName("StoredProcedure", "CreateOrderWithItems");
            NewRelic.addCustomParameter("customerId", customerId);
            NewRelic.addCustomParameter("numItems", numItems);

            Map<String, Object> result = storedProcedureService.createOrderWithItems(customerId, numItems);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in createOrderWithItems procedure", e);
            NewRelic.noticeError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/restock-inventory")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> restockLowInventory(
            @RequestParam(defaultValue = "500") int restockQuantity) {
        try {
            NewRelic.setTransactionName("StoredProcedure", "RestockLowInventory");
            NewRelic.addCustomParameter("restockQuantity", restockQuantity);

            Map<String, Object> result = storedProcedureService.restockLowInventory(restockQuantity);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in restockLowInventory procedure", e);
            NewRelic.noticeError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/calculate-loyalty")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> calculateCustomerLoyalty(
            @RequestParam long customerId) {
        try {
            NewRelic.setTransactionName("StoredProcedure", "CalculateCustomerLoyalty");
            NewRelic.addCustomParameter("customerId", customerId);

            Map<String, Object> result = storedProcedureService.calculateCustomerLoyalty(customerId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in calculateCustomerLoyalty procedure", e);
            NewRelic.noticeError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/purge-old-data")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> purgeOldData(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            NewRelic.setTransactionName("StoredProcedure", "PurgeOldData");
            NewRelic.addCustomParameter("daysToKeep", daysToKeep);

            Map<String, Object> result = storedProcedureService.purgeOldData(daysToKeep);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in purgeOldData procedure", e);
            NewRelic.noticeError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/daily-summary")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<String, Object>> dailySalesSummary() {
        try {
            NewRelic.setTransactionName("StoredProcedure", "DailySalesSummary");

            Map<String, Object> result = storedProcedureService.dailySalesSummary();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in dailySalesSummary procedure", e);
            NewRelic.noticeError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
