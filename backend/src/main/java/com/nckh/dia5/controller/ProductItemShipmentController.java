package com.nckh.dia5.controller;

import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.service.ProductItemShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho Shipment với item-level tracking
 */
@RestController
@RequestMapping("/api/shipments/items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductItemShipmentController {

    private final ProductItemShipmentService shipmentService;

    /**
     * Tạo shipment với danh sách items
     * POST /api/shipments/items/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR')")
    public ResponseEntity<?> createShipmentWithItems(@RequestBody CreateShipmentRequest request) {
        try {
            log.info("Creating item-level shipment from company {} to {}", 
                    request.getFromCompanyId(), request.getToCompanyId());

            ProductItemShipmentService.CreateShipmentWithItemsRequest serviceRequest = 
                ProductItemShipmentService.CreateShipmentWithItemsRequest.builder()
                    .fromCompanyId(request.getFromCompanyId())
                    .toCompanyId(request.getToCompanyId())
                    .itemIds(request.getItemIds())
                    .expectedDeliveryDate(request.getExpectedDeliveryDate())
                    .transportMethod(request.getTransportMethod())
                    .trackingNumber(request.getTrackingNumber())
                    .notes(request.getNotes())
                    .build();

            ProductItemShipmentService.ShipmentItemResponse response = 
                shipmentService.createShipmentWithItems(serviceRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("shipment", response);
            result.put("message", "Shipment created successfully");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating shipment with items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error creating shipment: " + e.getMessage()
                    ));
        }
    }

    /**
     * Nhận hàng - quét từng sản phẩm
     * POST /api/shipments/items/{shipmentId}/receive
     */
    @PostMapping("/{shipmentId}/receive")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> receiveShipmentItems(
            @PathVariable Long shipmentId,
            @RequestBody ReceiveItemsRequest request
    ) {
        try {
            log.info("Receiving {} items for shipment {}", 
                    request.getItemIds().size(), shipmentId);

            ProductItemShipmentService.ShipmentReceiveResponse response = 
                shipmentService.receiveShipmentItems(shipmentId, request.getItemIds());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("receive", response);
            
            if (response.getIsComplete()) {
                result.put("message", "All items received successfully!");
            } else {
                result.put("message", String.format("Received %d/%d items", 
                        response.getReceivedCount(), response.getTotalCount()));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error receiving shipment items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error receiving items: " + e.getMessage()
                    ));
        }
    }

    /**
     * Quét hàng loạt QR codes
     * POST /api/shipments/items/{shipmentId}/bulk-scan
     */
    @PostMapping("/{shipmentId}/bulk-scan")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> bulkScanItems(
            @PathVariable Long shipmentId,
            @RequestBody BulkScanRequest request
    ) {
        try {
            log.info("Bulk scanning {} items for shipment {}", 
                    request.getItemCodes().size(), shipmentId);

            ProductItemShipmentService.BulkScanResponse response = 
                shipmentService.bulkScanItems(shipmentId, request.getItemCodes());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("scan", response);
            
            if (response.getInvalidCount() > 0 || response.getAlreadyReceivedCount() > 0) {
                result.put("message", String.format(
                        "Scanned %d items: %d valid, %d invalid, %d already received",
                        response.getTotalScanned(),
                        response.getValidCount(),
                        response.getInvalidCount(),
                        response.getAlreadyReceivedCount()
                ));
            } else {
                result.put("message", String.format("All %d items scanned successfully!", 
                        response.getValidCount()));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error bulk scanning items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error scanning items: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get items trong shipment
     * GET /api/shipments/items/{shipmentId}
     */
    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY')")
    public ResponseEntity<?> getShipmentItems(@PathVariable Long shipmentId) {
        try {
            List<ProductItem> items = shipmentService.getShipmentItems(shipmentId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("items", items);
            result.put("count", items.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting shipment items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error getting items: " + e.getMessage()
                    ));
        }
    }

    /**
     * Tạo shipment từ danh sách item codes (quét QR để chọn items)
     * POST /api/shipments/items/create-by-codes
     */
    @PostMapping("/create-by-codes")
    @PreAuthorize("hasAnyRole('MANUFACTURER', 'DISTRIBUTOR')")
    public ResponseEntity<?> createShipmentByItemCodes(@RequestBody CreateShipmentByCodesRequest request) {
        try {
            log.info("Creating shipment with {} scanned items", request.getItemCodes().size());

            // Find items by codes
            List<Long> itemIds = request.getItemCodes().stream()
                    .map(code -> {
                        // This should be done in service, simplified here
                        return 0L; // Placeholder
                    })
                    .toList();

            // TODO: Implement proper item code to ID conversion in service
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of(
                            "success", false,
                            "message", "Use POST /api/shipments/items/create with itemIds instead"
                    ));
        } catch (Exception e) {
            log.error("Error creating shipment by codes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error: " + e.getMessage()
                    ));
        }
    }

    // DTO Classes
    public static class CreateShipmentRequest {
        private Long fromCompanyId;
        private Long toCompanyId;
        private List<Long> itemIds;
        private LocalDateTime expectedDeliveryDate;
        private String transportMethod;
        private String trackingNumber;
        private String notes;

        // Getters and Setters
        public Long getFromCompanyId() { return fromCompanyId; }
        public void setFromCompanyId(Long fromCompanyId) { this.fromCompanyId = fromCompanyId; }
        public Long getToCompanyId() { return toCompanyId; }
        public void setToCompanyId(Long toCompanyId) { this.toCompanyId = toCompanyId; }
        public List<Long> getItemIds() { return itemIds; }
        public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
        public LocalDateTime getExpectedDeliveryDate() { return expectedDeliveryDate; }
        public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) { 
            this.expectedDeliveryDate = expectedDeliveryDate; 
        }
        public String getTransportMethod() { return transportMethod; }
        public void setTransportMethod(String transportMethod) { this.transportMethod = transportMethod; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class ReceiveItemsRequest {
        private List<Long> itemIds;

        public List<Long> getItemIds() { return itemIds; }
        public void setItemIds(List<Long> itemIds) { this.itemIds = itemIds; }
    }

    public static class BulkScanRequest {
        private List<String> itemCodes;

        public List<String> getItemCodes() { return itemCodes; }
        public void setItemCodes(List<String> itemCodes) { this.itemCodes = itemCodes; }
    }

    public static class CreateShipmentByCodesRequest {
        private Long fromCompanyId;
        private Long toCompanyId;
        private List<String> itemCodes;
        private LocalDateTime expectedDeliveryDate;
        private String transportMethod;
        private String trackingNumber;
        private String notes;

        // Getters and Setters
        public Long getFromCompanyId() { return fromCompanyId; }
        public void setFromCompanyId(Long fromCompanyId) { this.fromCompanyId = fromCompanyId; }
        public Long getToCompanyId() { return toCompanyId; }
        public void setToCompanyId(Long toCompanyId) { this.toCompanyId = toCompanyId; }
        public List<String> getItemCodes() { return itemCodes; }
        public void setItemCodes(List<String> itemCodes) { this.itemCodes = itemCodes; }
        public LocalDateTime getExpectedDeliveryDate() { return expectedDeliveryDate; }
        public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) { 
            this.expectedDeliveryDate = expectedDeliveryDate; 
        }
        public String getTransportMethod() { return transportMethod; }
        public void setTransportMethod(String transportMethod) { this.transportMethod = transportMethod; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}

