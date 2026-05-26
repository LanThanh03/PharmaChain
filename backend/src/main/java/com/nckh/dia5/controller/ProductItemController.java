package com.nckh.dia5.controller;

import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.service.MerkleTreeService;
import com.nckh.dia5.service.ProductItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho Product Items
 */
@RestController
@RequestMapping("/api/product-items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductItemController {

    private final ProductItemService productItemService;
    private final MerkleTreeService merkleTreeService;

    /**
     * Generate items cho một batch
     * POST /api/product-items/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('MANUFACTURER')")
    public ResponseEntity<?> generateItemsForBatch(@RequestBody GenerateItemsRequest request) {
        try {
            log.info("Generating items for batch: {}, quantity: {}", 
                    request.getBatchId(), request.getQuantity());

            List<ProductItem> items = productItemService.generateItemsForBatch(
                    request.getBatchId(),
                    request.getQuantity(),
                    request.getPrefix()
            );

            // Create Merkle Tree
            List<String> itemCodes = items.stream()
                    .map(ProductItem::getItemCode)
                    .toList();
            
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(itemCodes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemsCreated", items.size());
            response.put("batchId", request.getBatchId());
            response.put("merkleRoot", merkleTree.getRoot());
            response.put("items", items);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Generate items với QR images
     * POST /api/product-items/generate-with-qr
     */
    @PostMapping("/generate-with-qr")
    @PreAuthorize("hasRole('MANUFACTURER')")
    public ResponseEntity<?> generateItemsWithQR(@RequestBody GenerateItemsRequest request) {
        try {
            log.info("Generating items with QR for batch: {}", request.getBatchId());

            List<ProductItem> items = productItemService.generateItemsWithQRImages(
                    request.getBatchId(),
                    request.getQuantity(),
                    request.getPrefix()
            );

            // Create Merkle Tree
            List<String> itemCodes = items.stream()
                    .map(ProductItem::getItemCode)
                    .toList();
            
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(itemCodes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemsCreated", items.size());
            response.put("batchId", request.getBatchId());
            response.put("merkleRoot", merkleTree.getRoot());
            response.put("items", items);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating items with QR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get Merkle proof cho một item
     * GET /api/product-items/{itemCode}/merkle-proof
     */
    @GetMapping("/{itemCode}/merkle-proof")
    public ResponseEntity<?> getMerkleProof(@PathVariable String itemCode) {
        try {
            ProductItem item = productItemService.findByItemCode(itemCode)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemCode));

            // Get all items in same batch
            List<String> itemCodes = productItemService.getItemCodesByBatchId(
                    item.getDrugBatch().getId()
            );

            // Create Merkle Tree
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(itemCodes);

            // Generate proof
            List<String> proof = merkleTreeService.generateProof(merkleTree, itemCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemCode", itemCode);
            response.put("batchId", item.getDrugBatch().getId());
            response.put("merkleRoot", merkleTree.getRoot());
            response.put("merkleProof", proof);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting Merkle proof", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get item by item code
     * GET /api/product-items/code/{itemCode}
     */
    @GetMapping("/code/{itemCode}")
    public ResponseEntity<?> getItemByCode(@PathVariable String itemCode) {
        try {
            ProductItem item = productItemService.findByItemCode(itemCode)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemCode));

            return ResponseEntity.ok(Map.of("success", true, "item", item));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get items by batch
     * GET /api/product-items/batch/{batchId}
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<?> getItemsByBatch(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "itemCode") String sortBy
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
            Page<ProductItem> items = productItemService.findByBatchId(batchId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", items.getContent());
            response.put("currentPage", items.getNumber());
            response.put("totalItems", items.getTotalElements());
            response.put("totalPages", items.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get items by owner
     * GET /api/product-items/owner
     */
    @GetMapping("/owner")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> getItemsByOwner(
            @RequestParam Long ownerId,
            @RequestParam String ownerType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            ProductItem.OwnerType type = ProductItem.OwnerType.valueOf(ownerType);
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ProductItem> items = productItemService.findByOwner(ownerId, type, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", items.getContent());
            response.put("currentPage", items.getNumber());
            response.put("totalItems", items.getTotalElements());
            response.put("totalPages", items.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Search items
     * GET /api/product-items/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductItem> items = productItemService.searchItems(keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", items.getContent());
            response.put("currentPage", items.getNumber());
            response.put("totalItems", items.getTotalElements());
            response.put("totalPages", items.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get expiring soon items
     * GET /api/product-items/expiring-soon
     */
    @GetMapping("/expiring-soon")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> getExpiringSoonItems(
            @RequestParam(defaultValue = "90") int daysThreshold
    ) {
        try {
            List<ProductItem> items = productItemService.findExpiringSoonItems(daysThreshold);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "items", items,
                    "count", items.size(),
                    "daysThreshold", daysThreshold
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get statistics by owner
     * GET /api/product-items/stats/owner
     */
    @GetMapping("/stats/owner")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> getOwnerStats(
            @RequestParam Long ownerId,
            @RequestParam String ownerType
    ) {
        try {
            ProductItem.OwnerType type = ProductItem.OwnerType.valueOf(ownerType);
            
            Long total = productItemService.countByOwner(ownerId, type);
            Long manufactured = productItemService.countByOwnerAndStatus(
                    ownerId, type, ProductItem.ItemStatus.MANUFACTURED);
            Long inWarehouse = productItemService.countByOwnerAndStatus(
                    ownerId, type, ProductItem.ItemStatus.IN_WAREHOUSE);
            Long inTransit = productItemService.countByOwnerAndStatus(
                    ownerId, type, ProductItem.ItemStatus.IN_TRANSIT);
            Long sold = productItemService.countByOwnerAndStatus(
                    ownerId, type, ProductItem.ItemStatus.SOLD);

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("manufactured", manufactured);
            stats.put("inWarehouse", inWarehouse);
            stats.put("inTransit", inTransit);
            stats.put("sold", sold);

            return ResponseEntity.ok(Map.of("success", true, "stats", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update item status
     * PUT /api/product-items/{itemId}/status
     */
    @PutMapping("/{itemId}/status")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> updateItemStatus(
            @PathVariable Long itemId,
            @RequestBody UpdateStatusRequest request
    ) {
        try {
            ProductItem.ItemStatus status = ProductItem.ItemStatus.valueOf(request.getStatus());
            ProductItem item = productItemService.updateItemStatus(itemId, status);

            return ResponseEntity.ok(Map.of("success", true, "item", item));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Report item as damaged
     * POST /api/product-items/{itemCode}/report-damage
     */
    @PostMapping("/{itemCode}/report-damage")
    public ResponseEntity<?> reportDamagedItem(
            @PathVariable String itemCode,
            @RequestBody ReportDamageRequest request
    ) {
        try {
            ProductItem item = productItemService.reportDamagedItem(
                    itemCode, 
                    request.getPharmacyId(), 
                    request.getReason(), 
                    request.getImageUrl()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "Báo hỏng thành công. Đang ghi nhận lên Blockchain.",
                    "item", item
            ));
        } catch (Exception e) {
            log.error("Failed to report damaged item: {}", itemCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // DTO Classes
    public static class GenerateItemsRequest {
        private Long batchId;
        private Integer quantity;
        private String prefix;

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    public static class UpdateStatusRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ReportDamageRequest {
        private Long pharmacyId;
        private String reason;
        private String imageUrl;

        public Long getPharmacyId() { return pharmacyId; }
        public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}

