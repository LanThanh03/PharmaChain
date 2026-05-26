package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.model.ProductItemMovement;
import com.nckh.dia5.model.ProductItemVerification;
import com.nckh.dia5.model.Shipment;
import com.nckh.dia5.model.DispenseInstruction;
import com.nckh.dia5.service.ProductItemService;
import com.nckh.dia5.service.BlockchainService;
import com.nckh.dia5.repository.ProductItemRepository;
import com.nckh.dia5.repository.ProductItemMovementRepository;
import com.nckh.dia5.repository.ProductItemVerificationRepository;
import com.nckh.dia5.repository.ShipmentRepository;
import com.nckh.dia5.repository.BlockchainTransactionRepository;
import com.nckh.dia5.repository.DispenseInstructionRepository;
import com.nckh.dia5.model.BlockchainTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Public API để verify sản phẩm (không cần authentication)
 * Dùng cho mobile app khi người dùng cuối quét QR
 */
@Slf4j
@RestController
@RequestMapping("/api/public/verify")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicProductVerificationController {

    private final ProductItemRepository productItemRepository;
    private final ProductItemMovementRepository movementRepository;
    private final ProductItemVerificationRepository verificationRepository;
    private final ShipmentRepository shipmentRepository;
    private final BlockchainService blockchainService;
    private final com.nckh.dia5.repository.DrugBatchRepository drugBatchRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final DispenseInstructionRepository dispenseInstructionRepository;
    private final com.nckh.dia5.repository.DrugProductRepository drugProductRepository;

    /**
     * Verify product by item code
     * GET /api/public/verify/{itemCode}
     */
    @GetMapping("/{itemCode}")
    public ResponseEntity<Map<String, Object>> verifyProduct(@PathVariable String itemCode) {
        try {
            log.info("Public verification request for item: {}", itemCode);

            // Find product item
            Optional<ProductItem> itemOpt = productItemRepository.findByItemCode(itemCode);
            
            if (itemOpt.isEmpty()) {
                return ResponseEntity.ok(buildErrorResponse("Không tìm thấy sản phẩm với mã: " + itemCode));
            }

            ProductItem item = itemOpt.get();

            // Record verification
            recordVerification(item);

            // Get movements
            List<ProductItemMovement> movements = movementRepository.findByProductItemIdOrderByMovementTimestampAsc(item.getId());

            // Check expiry
            LocalDateTime now = LocalDateTime.now();
            long daysUntilExpiry = ChronoUnit.DAYS.between(now, item.getExpiryDate());
            String expiryStatus;
            if (daysUntilExpiry < 0) {
                expiryStatus = "EXPIRED";
            } else if (daysUntilExpiry <= 30) {
                expiryStatus = "EXPIRING_SOON";
            } else {
                expiryStatus = "VALID";
            }

            // Determine verification result
            String verificationResult;
            boolean isAuthentic;
            String message;

            if (item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED) {
                verificationResult = "RECALLED";
                isAuthentic = false;
                message = "⚠️ SẢN PHẨM ĐÃ BỊ THU HỒI! Vui lòng không sử dụng.";
            } else if ("EXPIRED".equals(expiryStatus)) {
                verificationResult = "EXPIRED";
                isAuthentic = true; // Authentic but expired
                message = "⚠️ Sản phẩm chính hãng nhưng đã hết hạn sử dụng.";
            } else {
                verificationResult = "AUTHENTIC";
                isAuthentic = true;
                message = "✅ Sản phẩm chính hãng, an toàn sử dụng.";
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isAuthentic", isAuthentic);
            response.put("verificationResult", verificationResult);
            response.put("message", message);

            // Product info
            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("itemCode", item.getItemCode());
            productInfo.put("name", item.getDrugBatch().getDrugName());
            productInfo.put("activeIngredient", item.getDrugProduct() != null ? item.getDrugProduct().getActiveIngredient() : null);
            productInfo.put("dosage", item.getDrugProduct() != null ? item.getDrugProduct().getDosage() : null);
            productInfo.put("manufacturer", item.getDrugBatch().getManufacturer());
            productInfo.put("batchNumber", item.getDrugBatch().getBatchNumber());
            
            if (item.getDrugProduct() != null) {
                productInfo.put("imageUrl", item.getDrugProduct().getImageUrl());
                productInfo.put("description", item.getDrugProduct().getDescription());
                productInfo.put("category", item.getDrugProduct().getCategory());
                productInfo.put("usage", item.getDrugProduct().getDescription());
            }

            productInfo.put("manufactureDate", item.getManufactureDate());
            productInfo.put("expiryDate", item.getExpiryDate());
            productInfo.put("storageConditions", item.getDrugBatch().getStorageConditions());
            productInfo.put("expiryStatus", expiryStatus);
            productInfo.put("daysUntilExpiry", daysUntilExpiry);
            response.put("productInfo", productInfo);

            // Ownership history (journey) - from database movements
            List<Map<String, Object>> ownershipHistory = movements.stream()
                    .map(this::mapMovementToJourney)
                    .collect(Collectors.toList());
            response.put("ownershipHistory", ownershipHistory);
            response.put("journeySteps", ownershipHistory.size());

            // ✅ THÊM: Lấy blockchain shipment history với checkpoints chi tiết
            List<Map<String, Object>> blockchainShipmentHistory = getBlockchainShipmentHistory(item);
            response.put("blockchainShipmentHistory", blockchainShipmentHistory);
            response.put("blockchainCheckpointCount", blockchainShipmentHistory.size());

            // Additional info
            response.put("blockchainVerified", item.getIsBlockchainSynced() != null && item.getIsBlockchainSynced());
            response.put("recallStatus", item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED ? "RECALLED" : "");
            
            // Count verifications
            long scanCount = verificationRepository.countByItemId(item.getId());
            response.put("scanCount", scanCount + 1); // +1 for current scan
            
            // Last scanned (excluding current)
            List<ProductItemVerification> recentScans = verificationRepository
                    .findByProductItemIdOrderByScanTimestampDesc(item.getId());
            response.put("lastScanned", !recentScans.isEmpty() ? recentScans.get(0).getScanTimestamp() : null);

            log.info("Verification successful for item: {} - Result: {} - Blockchain checkpoints: {}", 
                    itemCode, verificationResult, blockchainShipmentHistory.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying product: {}", itemCode, e);
            return ResponseEntity.ok(buildErrorResponse("Lỗi khi xác thực sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * ✅ MỚI: Verify batch by batchNumber (Số lô) hoặc itemCode (Mã sản phẩm riêng lẻ)
     * GET /api/public/verify/batch/{identifier}
     * Dùng cho hiệu thuốc xác thực lô thuốc hoặc sản phẩm trước khi bán
     * Hỗ trợ:
     * - Mã lô (batch number): VD: BT202512121857
     * - Mã sản phẩm riêng lẻ (item code): VD: PL500-2004139, PARA-BATCH001-0001
     */
    @GetMapping("/batch/{identifier}")
    public ResponseEntity<Map<String, Object>> verifyBatch(@PathVariable String identifier) {
        try {
            log.info("Public batch/item verification request for identifier: {}", identifier);

            // ✅ BƯỚC 1: Thử tìm trong product_items trước (cho mã sản phẩm riêng lẻ như PL500-*)
            Optional<ProductItem> itemOpt = productItemRepository.findByItemCode(identifier);
            if (itemOpt.isPresent()) {
                log.info("Found product item with code: {}", identifier);
                ProductItem item = itemOpt.get();
                com.nckh.dia5.model.DrugBatch batch = item.getDrugBatch();
                
                // Record verification
                recordVerification(item);
                
                // Check expiry from item
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, item.getExpiryDate());
                String expiryStatus;
                if (daysUntilExpiry < 0) {
                    expiryStatus = "EXPIRED";
                } else if (daysUntilExpiry <= 30) {
                    expiryStatus = "EXPIRING_SOON";
                } else {
                    expiryStatus = "VALID";
                }

                // Check for recall
                boolean isAuthentic = true;
                String verificationResult;
                String message;

                if (item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED) {
                    verificationResult = "RECALLED";
                    isAuthentic = false;
                    message = "⚠️ SẢN PHẨM ĐÃ BỊ THU HỒI! Vui lòng không sử dụng.";
                } else if ("EXPIRED".equals(expiryStatus)) {
                    verificationResult = "EXPIRED";
                    message = "⚠️ Sản phẩm chính hãng nhưng đã hết hạn sử dụng.";
                } else {
                    verificationResult = "AUTHENTIC";
                    message = "✅ Sản phẩm chính hãng, an toàn sử dụng.";
                }

                // Build response with item + batch info
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", buildItemVerificationData(item, batch, isAuthentic, verificationResult, message, daysUntilExpiry, expiryStatus));

                log.info("Item verification successful: itemCode={}, batchNumber={}, result={}", 
                        identifier, batch.getBatchNumber(), verificationResult);
                return ResponseEntity.ok(response);
            }

            // ✅ BƯỚC 2: Nếu không phải item code, thử tìm trong drug_batches (mã lô)
            Optional<com.nckh.dia5.model.DrugBatch> batchOpt = drugBatchRepository.findByBatchNumber(identifier);
            
            if (batchOpt.isEmpty()) {
                // Try partial match
                List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findByBatchNumberContaining(identifier);
                if (!batches.isEmpty()) {
                    batchOpt = Optional.of(batches.get(0));
                }
            }
            
            if (batchOpt.isEmpty()) {
                // ✅ BƯỚC 3: Cuối cùng, thử tìm trong product_items bằng partial match
                List<ProductItem> items = productItemRepository.findByItemCodeContaining(identifier);
                if (!items.isEmpty()) {
                    ProductItem item = items.get(0);
                    log.info("Found product item (partial match) with code: {}", item.getItemCode());
                    com.nckh.dia5.model.DrugBatch batch = item.getDrugBatch();
                    
                    // Check expiry
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, item.getExpiryDate());
                    String expiryStatus = daysUntilExpiry < 0 ? "EXPIRED" : (daysUntilExpiry <= 30 ? "EXPIRING_SOON" : "VALID");
                    
                    boolean isAuthentic = item.getCurrentStatus() != ProductItem.ItemStatus.RECALLED;
                    String verificationResult = !isAuthentic ? "RECALLED" : ("EXPIRED".equals(expiryStatus) ? "EXPIRED" : "AUTHENTIC");
                    String message = !isAuthentic ? "⚠️ SẢN PHẨM ĐÃ BỊ THU HỒI!" : 
                                    ("EXPIRED".equals(expiryStatus) ? "⚠️ Sản phẩm đã hết hạn." : "✅ Sản phẩm chính hãng.");
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", buildItemVerificationData(item, batch, isAuthentic, verificationResult, message, daysUntilExpiry, expiryStatus));
                    return ResponseEntity.ok(response);
                }
                
                return ResponseEntity.ok(buildBatchErrorResponse("Không tìm thấy lô thuốc hoặc sản phẩm với mã: " + identifier));
            }

            com.nckh.dia5.model.DrugBatch batch = batchOpt.get();

            // Check expiry
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now, batch.getExpiryDate());
            String expiryStatus;
            if (daysUntilExpiry < 0) {
                expiryStatus = "EXPIRED";
            } else if (daysUntilExpiry <= 30) {
                expiryStatus = "EXPIRING_SOON";
            } else {
                expiryStatus = "VALID";
            }

            // Determine verification result
            boolean isAuthentic = true;
            String verificationResult;
            String message;

            if ("EXPIRED".equals(expiryStatus)) {
                verificationResult = "EXPIRED";
                message = "⚠️ Lô thuốc chính hãng nhưng đã hết hạn sử dụng.";
            } else {
                verificationResult = "AUTHENTIC";
                message = "✅ Lô thuốc chính hãng, an toàn sử dụng.";
            }

            // Build response (compatible with frontend)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", buildBatchVerificationData(batch, isAuthentic, verificationResult, message, daysUntilExpiry, expiryStatus));

            log.info("Batch verification successful: batchNumber={}, result={}", identifier, verificationResult);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying batch/item: {}", identifier, e);
            return ResponseEntity.ok(buildBatchErrorResponse("Lỗi khi xác thực lô thuốc/sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * Build item verification response data (for product items)
     * ✅ ENHANCED: Now includes traceabilityHistory for full supply chain tracking
     */
    private Map<String, Object> buildItemVerificationData(ProductItem item, com.nckh.dia5.model.DrugBatch batch, 
            boolean isAuthentic, String verificationResult, String message, 
            long daysUntilExpiry, String expiryStatus) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("verified", isAuthentic);
        data.put("isItem", true); // Flag để biết đây là sản phẩm riêng lẻ
        
        // Batch info (giữ nguyên định dạng để frontend hoạt động)
        Map<String, Object> batchInfo = new HashMap<>();
        batchInfo.put("batchId", batch.getBatchId());
        batchInfo.put("batchNumber", batch.getBatchNumber());
        batchInfo.put("drugName", batch.getDrugName());
        batchInfo.put("manufacturer", batch.getManufacturer());
        batchInfo.put("manufactureTimestamp", item.getManufactureDate()); // Use item date
        batchInfo.put("expiryDate", item.getExpiryDate()); // Use item date
        batchInfo.put("quantity", batch.getQuantity());
        batchInfo.put("status", item.getCurrentStatus().name());
        batchInfo.put("storageConditions", batch.getStorageConditions());
        batchInfo.put("qrCode", item.getQrCodeData());
        batchInfo.put("transactionHash", batch.getTransactionHash());
        batchInfo.put("blockNumber", batch.getBlockNumber());
        batchInfo.put("createdAt", batch.getCreatedAt());
        // Item-specific info
        batchInfo.put("itemCode", item.getItemCode());
        batchInfo.put("currentOwner", item.getCurrentOwnerType() != null ? item.getCurrentOwnerType().name() : "N/A");
        batchInfo.put("soldAt", item.getSoldAt());
        // ✅ Drug product details from manufacturer (description, usage info)
        if (item.getDrugProduct() != null) {
            batchInfo.put("imageUrl", item.getDrugProduct().getImageUrl());
            batchInfo.put("activeIngredient", item.getDrugProduct().getActiveIngredient());
            batchInfo.put("dosage", item.getDrugProduct().getDosage());
            batchInfo.put("drugDosage", item.getDrugProduct().getDosage());
            batchInfo.put("unit", item.getDrugProduct().getUnit());
            batchInfo.put("category", item.getDrugProduct().getCategory());
            batchInfo.put("description", item.getDrugProduct().getDescription());
        } else {
            batchInfo.put("imageUrl", null);
            batchInfo.put("activeIngredient", null);
            batchInfo.put("dosage", null);
            batchInfo.put("drugDosage", null);
            batchInfo.put("description", null);
        }
        data.put("batch", batchInfo);
        
        // Blockchain info
        Map<String, Object> blockchain = new HashMap<>();
        blockchain.put("verified", item.getIsBlockchainSynced() != null && item.getIsBlockchainSynced());
        blockchain.put("transactionHash", batch.getTransactionHash());
        blockchain.put("blockNumber", batch.getBlockNumber());
        blockchain.put("timestamp", batch.getCreatedAt());
        blockchain.put("merkleProof", item.getBlockchainMerkleProof());
        data.put("blockchain", blockchain);
        
        // ✅ NEW: Include full traceability history from batch
        List<Map<String, Object>> traceabilityHistory = buildTraceabilityHistory(batch);
        
        // Add item-specific sale event if item is SOLD
        if (item.getCurrentStatus() == ProductItem.ItemStatus.SOLD) {
            Map<String, Object> saleStep = new HashMap<>();
            saleStep.put("step", traceabilityHistory.size() + 1);
            saleStep.put("event", "Đã bán");
            saleStep.put("eventType", "SALE");
            saleStep.put("actor", "Quầy thuốc");
            saleStep.put("actorType", "pharmacy");
            saleStep.put("location", "Hiệu thuốc");
            saleStep.put("timestamp", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : LocalDateTime.now().toString());
            saleStep.put("txHash", null); // Offchain transaction
            saleStep.put("itemCode", item.getItemCode());
            saleStep.put("details", "Sản phẩm đã được bán cho khách hàng");
            traceabilityHistory.add(saleStep);
        }
        
        data.put("traceabilityHistory", traceabilityHistory);
        
        // ✅ NEW: Include pharmacy dispense instructions if available
        Optional<DispenseInstruction> instructionOpt = dispenseInstructionRepository.findByItemCode(item.getItemCode());
        if (instructionOpt.isPresent()) {
            DispenseInstruction instruction = instructionOpt.get();
            Map<String, Object> pharmacyInstructions = new HashMap<>();
            pharmacyInstructions.put("dosage", instruction.getDosage());
            pharmacyInstructions.put("frequency", instruction.getFrequency());
            pharmacyInstructions.put("mealRelation", instruction.getMealRelation());
            pharmacyInstructions.put("mealRelationDisplay", instruction.getMealRelationDisplay());
            pharmacyInstructions.put("specificTimes", instruction.getSpecificTimes());
            pharmacyInstructions.put("durationDays", instruction.getDurationDays());
            pharmacyInstructions.put("specialNotes", instruction.getSpecialNotes());
            pharmacyInstructions.put("warnings", instruction.getWarnings());
            pharmacyInstructions.put("pharmacistName", instruction.getPharmacistName());
            pharmacyInstructions.put("customerName", instruction.getCustomerName());
            pharmacyInstructions.put("customerPhone", instruction.getCustomerPhone());
            pharmacyInstructions.put("dispensedAt", instruction.getDispensedAt());
            data.put("pharmacyInstructions", pharmacyInstructions);
        }
        
        // Extra info
        data.put("verificationResult", verificationResult);
        data.put("message", message);
        data.put("expiryStatus", expiryStatus);
        data.put("daysUntilExpiry", daysUntilExpiry);
        
        return data;
    }

    /**
     * Build batch verification response data
     * ✅ ENHANCED: Now includes full traceability history with TX hash for each step
     */
    private Map<String, Object> buildBatchVerificationData(com.nckh.dia5.model.DrugBatch batch, 
            boolean isAuthentic, String verificationResult, String message, 
            long daysUntilExpiry, String expiryStatus) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("verified", isAuthentic);
        
        // Batch info (compatible with frontend expectations)
        Map<String, Object> batchInfo = new HashMap<>();
        batchInfo.put("batchId", batch.getBatchId());
        batchInfo.put("batchNumber", batch.getBatchNumber());
        batchInfo.put("drugName", batch.getDrugName());
        batchInfo.put("manufacturer", batch.getManufacturer());
        batchInfo.put("manufactureTimestamp", batch.getManufactureTimestamp());
        batchInfo.put("expiryDate", batch.getExpiryDate());
        batchInfo.put("quantity", batch.getQuantity());
        batchInfo.put("status", batch.getStatus().name());
        batchInfo.put("storageConditions", batch.getStorageConditions());
        batchInfo.put("qrCode", batch.getQrCode());
        batchInfo.put("transactionHash", batch.getTransactionHash());
        batchInfo.put("blockNumber", batch.getBlockNumber());
        batchInfo.put("createdAt", batch.getCreatedAt());
        
        // ✅ FIX: Look up DrugProduct by name to get imageUrl and other details
        try {
            java.util.List<com.nckh.dia5.model.DrugProduct> products = drugProductRepository.findByName(batch.getDrugName());
            if (!products.isEmpty()) {
                com.nckh.dia5.model.DrugProduct product = products.get(0);
                batchInfo.put("imageUrl", product.getImageUrl());
                batchInfo.put("description", product.getDescription());
                batchInfo.put("activeIngredient", product.getActiveIngredient());
                batchInfo.put("dosage", product.getDosage());
                batchInfo.put("category", product.getCategory());
                log.info("Found DrugProduct for batch {}: imageUrl={}", batch.getBatchNumber(), product.getImageUrl());
            } else {
                log.info("No DrugProduct found for drugName: {}", batch.getDrugName());
                batchInfo.put("imageUrl", null);
            }
        } catch (Exception e) {
            log.warn("Error looking up DrugProduct for batch {}: {}", batch.getBatchNumber(), e.getMessage());
            batchInfo.put("imageUrl", null);
        }
        
        data.put("batch", batchInfo);
        
        // Blockchain info
        Map<String, Object> blockchain = new HashMap<>();
        blockchain.put("verified", batch.getIsSynced());
        blockchain.put("transactionHash", batch.getTransactionHash());
        blockchain.put("blockNumber", batch.getBlockNumber());
        blockchain.put("timestamp", batch.getCreatedAt());
        data.put("blockchain", blockchain);
        
        // ✅ NEW: Build full traceability history with TX hash for each step
        List<Map<String, Object>> traceabilityHistory = buildTraceabilityHistory(batch);
        data.put("traceabilityHistory", traceabilityHistory);
        
        // Extra info
        data.put("verificationResult", verificationResult);
        data.put("message", message);
        data.put("expiryStatus", expiryStatus);
        data.put("daysUntilExpiry", daysUntilExpiry);
        
        return data;
    }
    
    /**
     * ✅ NEW: Build complete traceability history from blockchain transactions and shipments
     * Returns a list of events with TX hash for: manufacturing, shipping, receiving
     */
    private List<Map<String, Object>> buildTraceabilityHistory(com.nckh.dia5.model.DrugBatch batch) {
        List<Map<String, Object>> history = new ArrayList<>();
        int stepNumber = 1;
        
        try {
            // Step 1: Manufacturing (batch creation)
            Map<String, Object> manufactureStep = new HashMap<>();
            manufactureStep.put("step", stepNumber++);
            manufactureStep.put("event", "Sản xuất");
            manufactureStep.put("eventType", "MANUFACTURE");
            manufactureStep.put("actor", batch.getManufacturer());
            manufactureStep.put("actorType", "manufacturer");
            manufactureStep.put("location", "Nhà máy sản xuất");
            manufactureStep.put("timestamp", batch.getManufactureTimestamp() != null ? 
                    batch.getManufactureTimestamp().toString() : batch.getCreatedAt().toString());
            manufactureStep.put("txHash", batch.getTransactionHash());
            manufactureStep.put("blockNumber", batch.getBlockNumber());
            manufactureStep.put("details", "Lô thuốc được sản xuất và ghi nhận lên blockchain");
            history.add(manufactureStep);
            
            // Get all transactions for this batch to find manufacture and recall TXs
            List<BlockchainTransaction> batchTxs = blockchainTransactionRepository.findByDrugBatch(batch);
            
            // Manufacture TX
            String manufactureTx = batchTxs.stream()
                .filter(t -> "createBatchWithItems".equals(t.getFunctionName()) || "issueBatch".equals(t.getFunctionName()))
                .map(BlockchainTransaction::getTransactionHash)
                .findFirst()
                .orElse(batch.getTransactionHash());

            // Update manufacture step with correct TX
            if (!history.isEmpty() && "Sản xuất".equals(history.get(0).get("event"))) {
                history.get(0).put("txHash", manufactureTx);
            }

            // Get all shipments for this batch
            List<Shipment> shipments = shipmentRepository.findByDrugBatch(batch);
            log.info("Building traceability: Found {} shipments for batch {}", shipments.size(), batch.getBatchNumber());
            
            for (Shipment shipment : shipments) {
                // ... (shipment logic stays same)
                // Shipment creation step
                Map<String, Object> shipStep = new HashMap<>();
                shipStep.put("step", stepNumber++);
                shipStep.put("event", "Vận chuyển");
                shipStep.put("eventType", "SHIP");
                
                // Get sender info
                String fromName = "Nhà sản xuất";
                if (shipment.getFromCompany() != null) {
                    fromName = shipment.getFromCompany().getName();
                }
                shipStep.put("actor", fromName);
                shipStep.put("actorType", "distributor");
                
                // Get receiver info
                String toName = "Đơn vị nhận";
                if (shipment.getToCompany() != null) {
                    toName = shipment.getToCompany().getName();
                }
                shipStep.put("location", "Từ " + fromName + " đến " + toName);
                shipStep.put("toLocation", toName);
                shipStep.put("timestamp", shipment.getShipmentDate() != null ? 
                        shipment.getShipmentDate().toString() : shipment.getCreatedAt().toString());
                shipStep.put("txHash", shipment.getCreateTxHash());
                shipStep.put("shipmentCode", shipment.getShipmentCode());
                shipStep.put("trackingInfo", shipment.getTrackingInfo());
                shipStep.put("quantity", shipment.getQuantity());
                shipStep.put("details", String.format("Vận chuyển %d đơn vị từ %s đến %s", 
                        shipment.getQuantity() != null ? shipment.getQuantity() : 0, fromName, toName));
                history.add(shipStep);
                
                // If shipment is delivered, add receiving step
                if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
                    Map<String, Object> receiveStep = new HashMap<>();
                    receiveStep.put("step", stepNumber++);
                    receiveStep.put("event", "Nhận hàng");
                    receiveStep.put("eventType", "RECEIVE");
                    receiveStep.put("actor", toName);
                    receiveStep.put("actorType", getActorType(shipment.getToCompany()));
                    receiveStep.put("location", toName);
                    receiveStep.put("timestamp", shipment.getActualDeliveryDate() != null ? 
                            shipment.getActualDeliveryDate().toString() : shipment.getUpdatedAt().toString());
                    
                    // Try to find the receive transaction
                    String receiveTxHash = findReceiveTransactionHash(shipment);
                    receiveStep.put("txHash", receiveTxHash);
                    receiveStep.put("shipmentCode", shipment.getShipmentCode());
                    receiveStep.put("details", "Đã nhận hàng tại " + toName);
                    history.add(receiveStep);
                }
            }

            // ✅ NEW: Add RECALL step if batch is recalled
            if (batch.getStatus() == com.nckh.dia5.model.DrugBatch.BatchStatus.RECALLED) {
                // Find recall TX
                String recallTx = batchTxs.stream()
                    .filter(t -> "recallBatch".equals(t.getFunctionName()))
                    .map(BlockchainTransaction::getTransactionHash)
                    .findFirst()
                    .orElse(batch.getTransactionHash()); // fallback

                Map<String, Object> recallStep = new HashMap<>();
                recallStep.put("step", stepNumber++);
                recallStep.put("event", "THU HỒI");
                recallStep.put("eventType", "RECALL");
                recallStep.put("actor", batch.getManufacturer());
                recallStep.put("actorType", "manufacturer");
                recallStep.put("location", "Toàn hệ thống");
                recallStep.put("timestamp", batch.getUpdatedAt() != null ? 
                        batch.getUpdatedAt().toString() : java.time.LocalDateTime.now().toString());
                recallStep.put("txHash", recallTx);
                recallStep.put("details", "Lô thuốc đã bị thu hồi khẩn cấp. Vui lòng KHÔNG SỬ DỤNG.");
                history.add(recallStep);
            }
            
        } catch (Exception e) {
            log.error("Error building traceability history for batch {}: {}", batch.getBatchNumber(), e.getMessage());
        }
        
        return history;
    }
    
    /**
     * Find the receive transaction hash for a shipment
     */
    private String findReceiveTransactionHash(Shipment shipment) {
        try {
            // Look for receiveShipment transaction in blockchain_transactions table
            List<BlockchainTransaction> transactions = blockchainTransactionRepository.findByShipment(shipment);
            for (BlockchainTransaction tx : transactions) {
                if ("receiveShipment".equals(tx.getFunctionName())) {
                    return tx.getTransactionHash();
                }
            }
            
            // Fallback: Return the shipment's receiveTxHash if stored separately
            if (shipment.getReceiveTxHash() != null) {
                return shipment.getReceiveTxHash();
            }
        } catch (Exception e) {
            log.debug("Could not find receive transaction for shipment {}: {}", 
                    shipment.getShipmentCode(), e.getMessage());
        }
        return null;
    }
    
    /**
     * Get actor type based on company type
     */
    private String getActorType(com.nckh.dia5.model.PharmaCompany company) {
        if (company == null) return "unknown";
        return switch (company.getCompanyType()) {
            case MANUFACTURER -> "manufacturer";
            case DISTRIBUTOR -> "distributor";
            case PHARMACY -> "pharmacy";
            default -> "unknown";
        };
    }

    /**
     * Build batch error response
     */
    private Map<String, Object> buildBatchErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("data", Map.of("verified", false));
        response.put("message", message);
        return response;
    }

    /**
     * ✅ MỚI: Lấy blockchain shipment history với checkpoints
     * Trả về timeline đầy đủ của sản phẩm từ blockchain
     */
    private List<Map<String, Object>> getBlockchainShipmentHistory(ProductItem item) {
        List<Map<String, Object>> allCheckpoints = new ArrayList<>();
        
        try {
            // Get all shipments related to this batch
            List<Shipment> shipments = shipmentRepository.findByDrugBatch(item.getDrugBatch());
            
            log.info("Found {} shipments for batch {}", shipments.size(), item.getDrugBatch().getBatchNumber());
            
            for (Shipment shipment : shipments) {
                // Get blockchain shipment ID
                BigInteger blockchainShipmentId = extractBlockchainShipmentId(shipment);

                if (blockchainShipmentId != null && blockchainShipmentId.compareTo(BigInteger.ZERO) > 0) {
                    log.info("Fetching blockchain history for shipment ID: {}", blockchainShipmentId);

                    try {
                        // ✅ Wrap blockchain call in CompletableFuture with 5s timeout
                        // to avoid blocking the entire verification request
                        final BigInteger shipmentId = blockchainShipmentId;
                        final Shipment s = shipment;
                        List<Map<String, Object>> checkpoints = CompletableFuture
                                .supplyAsync(() -> blockchainService.getShipmentHistory(shipmentId))
                                .get(5, TimeUnit.SECONDS);

                        // Enrich checkpoints with additional info
                        for (Map<String, Object> checkpoint : checkpoints) {
                            checkpoint.put("shipmentCode", s.getShipmentCode());
                            checkpoint.put("trackingNumber", s.getShipmentCode());
                            checkpoint.put("fromCompany", s.getFromCompany() != null ?
                                    s.getFromCompany().getName() : "Unknown");
                            checkpoint.put("toCompany", s.getToCompany() != null ?
                                    s.getToCompany().getName() : "Unknown");

                            // Format timestamp for display
                            if (checkpoint.containsKey("timestampMs")) {
                                long timestampMs = ((Number) checkpoint.get("timestampMs")).longValue();
                                LocalDateTime dateTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(timestampMs),
                                    ZoneId.systemDefault()
                                );
                                checkpoint.put("dateTime", dateTime);
                                checkpoint.put("displayTime", dateTime.toString());
                            }

                            allCheckpoints.add(checkpoint);
                        }
                        log.info("Added {} checkpoints from shipment {}", checkpoints.size(), blockchainShipmentId);

                    } catch (TimeoutException te) {
                        log.warn("Blockchain query timed out for shipment {} (5s limit) - skipping",
                                shipment.getShipmentCode());
                    } catch (Exception be) {
                        log.warn("Blockchain query failed for shipment {}: {} - skipping",
                                shipment.getShipmentCode(), be.getMessage());
                    }
                } else {
                    log.debug("Shipment {} has no blockchain ID", shipment.getShipmentCode());
                }
            }
            
            // Sort by timestamp (oldest first)
            allCheckpoints.sort((a, b) -> {
                long timeA = a.containsKey("timestamp") ? ((Number) a.get("timestamp")).longValue() : 0;
                long timeB = b.containsKey("timestamp") ? ((Number) b.get("timestamp")).longValue() : 0;
                return Long.compare(timeA, timeB);
            });
            
            log.info("Total blockchain checkpoints retrieved: {}", allCheckpoints.size());
            
        } catch (Exception e) {
            log.error("Failed to get blockchain shipment history", e);
        }
        
        return allCheckpoints;
    }
    
    /**
     * Extract blockchain shipment ID from Shipment entity
     */
    private BigInteger extractBlockchainShipmentId(Shipment shipment) {
        try {
            // Try to get from shipmentCode (format: SHIP-{id})
            if (shipment.getShipmentCode() != null && shipment.getShipmentCode().startsWith("SHIP-")) {
                String idPart = shipment.getShipmentCode().substring(5);
                return new BigInteger(idPart);
            }
            
            // Try to get from notes JSON (blockchain_id field)
            if (shipment.getNotes() != null && shipment.getNotes().contains("blockchain_id")) {
                // Simple JSON parsing (you might want to use a proper JSON library)
                String notes = shipment.getNotes();
                int idIndex = notes.indexOf("\"blockchain_id\":");
                if (idIndex > 0) {
                    int startQuote = notes.indexOf("\"", idIndex + 16);
                    int endQuote = notes.indexOf("\"", startQuote + 1);
                    if (startQuote > 0 && endQuote > startQuote) {
                        String idStr = notes.substring(startQuote + 1, endQuote);
                        return new BigInteger(idStr);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract blockchain ID from shipment {}: {}", 
                    shipment.getShipmentCode(), e.getMessage());
        }
        
        return null;
    }

    /**
     * Record verification scan
     */
    private void recordVerification(ProductItem item) {
        try {
            ProductItemVerification verification = new ProductItemVerification();
            verification.setProductItem(item);
            verification.setScanTimestamp(LocalDateTime.now());
            verification.setScannerType(ProductItemVerification.ScannerType.CONSUMER);
            verification.setVerificationResult(ProductItemVerification.VerificationResult.AUTHENTIC);
            verification.setBlockchainVerified(item.getIsBlockchainSynced() != null && item.getIsBlockchainSynced());
            
            verificationRepository.save(verification);
        } catch (Exception e) {
            log.warn("Failed to record verification: {}", e.getMessage());
        }
    }

    /**
     * Map movement to journey step
     */
    private Map<String, Object> mapMovementToJourney(ProductItemMovement movement) {
        Map<String, Object> step = new HashMap<>();
        
        step.put("stage", getStageText(movement.getMovementType()));
        step.put("company", movement.getToCompanyName());
        step.put("address", movement.getToAddressDetail());
        step.put("timestamp", movement.getMovementTimestamp());
        step.put("verified", true);
        step.put("icon", getStageIcon(movement.getMovementType()));
        
        return step;
    }

    /**
     * Get stage text for movement type
     */
    private String getStageText(ProductItemMovement.MovementType type) {
        return switch (type) {
            case MANUFACTURE -> "Sản xuất";
            case TRANSFER, SHIP -> "Vận chuyển";
            case RECEIVE -> "Nhận hàng";
            case SALE -> "Bán hàng";
            case RETURN -> "Trả lại";
            default -> type.toString();
        };
    }

    /**
     * Get icon for movement type
     */
    private String getStageIcon(ProductItemMovement.MovementType type) {
        return switch (type) {
            case MANUFACTURE -> "🏭";
            case TRANSFER, SHIP -> "🚚";
            case RECEIVE -> "📦";
            case SALE -> "🏪";
            case RETURN -> "↩️";
            default -> "•";
        };
    }

    /**
     * Build error response
     */
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("isAuthentic", false);
        response.put("verificationResult", "ERROR");
        response.put("message", message);
        response.put("productInfo", null);
        response.put("ownershipHistory", Collections.emptyList());
        response.put("journeySteps", 0);
        response.put("blockchainVerified", false);
        response.put("recallStatus", "");
        response.put("scanCount", 0);
        response.put("lastScanned", null);
        return response;
    }
}

