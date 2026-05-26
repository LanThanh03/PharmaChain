package com.nckh.dia5.controller;

import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.service.IndividualProductItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller để tạo ProductItem riêng lẻ với blockchain transaction riêng
 * Đảm bảo mỗi ProductItem có thể quét QR và theo dõi độc lập
 */
@RestController
@RequestMapping("/api/individual-product-items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IndividualProductItemController {

    private final IndividualProductItemService individualProductItemService;

    /**
     * Tạo ProductItem riêng lẻ
     * POST /api/individual-product-items/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('MANUFACTURER')")
    public ResponseEntity<?> createIndividualProductItem(@RequestBody CreateIndividualItemRequest request) {
        try {
            log.info("Creating individual ProductItem: batchId={}, productId={}, itemCode={}", 
                    request.getBatchId(), request.getDrugProductId(), request.getItemCode());

            ProductItem item = individualProductItemService.createIndividualProductItem(
                    request.getBatchId(),
                    request.getDrugProductId(),
                    request.getItemCode()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Individual ProductItem created successfully");
            response.put("item", item);
            response.put("itemCode", item.getItemCode());
            response.put("qrCodeData", item.getQrCodeData());
            response.put("blockchainSynced", item.getIsBlockchainSynced());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating individual ProductItem", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Tạo nhiều ProductItem riêng lẻ
     * POST /api/individual-product-items/create-multiple
     */
    @PostMapping("/create-multiple")
    @PreAuthorize("hasRole('MANUFACTURER')")
    public ResponseEntity<?> createMultipleIndividualProductItems(@RequestBody CreateMultipleItemsRequest request) {
        try {
            log.info("Creating {} individual ProductItems: batchId={}, productId={}", 
                    request.getQuantity(), request.getBatchId(), request.getDrugProductId());

            List<ProductItem> items = individualProductItemService.createMultipleIndividualProductItems(
                    request.getBatchId(),
                    request.getDrugProductId(),
                    request.getQuantity()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Created %d individual ProductItems", items.size()));
            response.put("itemsCreated", items.size());
            response.put("requestedQuantity", request.getQuantity());
            response.put("items", items);

            // Summary of item codes
            List<String> itemCodes = items.stream()
                    .map(ProductItem::getItemCode)
                    .toList();
            response.put("itemCodes", itemCodes);

            // Blockchain sync status
            long syncedCount = items.stream()
                    .mapToLong(item -> item.getIsBlockchainSynced() ? 1 : 0)
                    .sum();
            response.put("blockchainSyncedCount", syncedCount);
            response.put("blockchainSyncRate", items.size() > 0 ? (double) syncedCount / items.size() : 0.0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating multiple individual ProductItems", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Sync ProductItem riêng lẻ lên blockchain
     * POST /api/individual-product-items/{itemId}/sync
     */
    @PostMapping("/{itemId}/sync")
    @PreAuthorize("hasRole('MANUFACTURER')")
    public ResponseEntity<?> syncIndividualItemToBlockchain(@PathVariable Long itemId) {
        try {
            log.info("Syncing individual ProductItem to blockchain: itemId={}", itemId);

            boolean success = individualProductItemService.syncIndividualItemToBlockchain(itemId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "ProductItem synced to blockchain successfully" : "Failed to sync ProductItem to blockchain");
            response.put("itemId", itemId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing individual ProductItem to blockchain", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Request DTOs
     */
    public static class CreateIndividualItemRequest {
        private Long batchId;
        private Long drugProductId;
        private String itemCode;

        // Getters and setters
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public Long getDrugProductId() { return drugProductId; }
        public void setDrugProductId(Long drugProductId) { this.drugProductId = drugProductId; }

        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    }

    public static class CreateMultipleItemsRequest {
        private Long batchId;
        private Long drugProductId;
        private Integer quantity;

        // Getters and setters
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public Long getDrugProductId() { return drugProductId; }
        public void setDrugProductId(Long drugProductId) { this.drugProductId = drugProductId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
