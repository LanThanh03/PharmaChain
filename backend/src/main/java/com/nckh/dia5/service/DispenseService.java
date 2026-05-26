package com.nckh.dia5.service;

import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.model.ProductItemMovement;
import com.nckh.dia5.repository.ProductItemMovementRepository;
import com.nckh.dia5.repository.ProductItemRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service to handle pharmacy dispensing flow (DB-first + async blockchain)
 *
 * Steps:
 * 1) Scan QR (itemCode) -> Parse batch + sequence for logs
 * 2) Query DB via uk_item_code (<10ms)
 * 3) Check state: SOLD => warning, else OK to sell
 * 4) Update DB: set status = SOLD and create ProductItemMovement (SALE)
 * 5) Async blockchain: recordItemSale(itemCode) -> emits ItemSold event
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DispenseService {

    private final ProductItemRepository productItemRepository;
    private final ProductItemMovementRepository movementRepository;
    private final BlockchainService blockchainService;
    private final MerkleTreeService merkleTreeService;

    /**
     * Dispense an item to consumer (DB-first, async blockchain)
     * @param itemCode Item code from QR
     * @param customerPhone Optional customer identifier
     * @param notes Optional notes at counter
     * @return DispenseResult with updated status and movement info
     */
    @Transactional
    public DispenseResult dispenseItem(String itemCode, String customerPhone, String notes) {
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Item code is required");
        }

        log.info("BƯỚC 1: Quét QR -> itemCode='{}'", itemCode);
        ParsedCode parsed = parseItemCode(itemCode);
        if (parsed != null) {
            log.info("Parse: batchId='{}', sequence='{}'", parsed.batchId, parsed.sequence);
        }

        log.info("BƯỚC 2: Query DATABASE (uk_item_code)");
        Optional<ProductItem> itemOpt = productItemRepository.findByItemCode(itemCode);
        if (itemOpt.isEmpty()) {
            log.warn("Không tìm thấy sản phẩm với mã: {}", itemCode);
            return DispenseResult.builder()
                    .success(false)
                    .statusMessage("Không tìm thấy sản phẩm")
                    .itemCode(itemCode)
                    .build();
        }

        ProductItem item = itemOpt.get();

        log.info("BƯỚC 3: Kiểm tra trạng thái hiện tại = {}", item.getCurrentStatus());
        if (item.getCurrentStatus() == ProductItem.ItemStatus.SOLD) {
            log.warn("CẢNH BÁO: Thuốc đã bán! itemCode={}", item.getItemCode());
            return DispenseResult.builder()
                    .success(false)
                    .itemCode(item.getItemCode())
                    .currentStatus(item.getCurrentStatus().name())
                    .statusMessage("Thuốc đã bán!")
                    .alreadyDispensed(true)
                    .build();
        }

        // Capture previous owner to record movement correctly
        Long prevOwnerId = item.getCurrentOwnerId();
        ProductItem.OwnerType prevOwnerType = item.getCurrentOwnerType();

        log.info("BƯỚC 4: Update DATABASE (SYNC) -> set status=SOLD, owner=CONSUMER");
        item.setCurrentStatus(ProductItem.ItemStatus.SOLD);
        item.setCurrentOwnerType(ProductItem.OwnerType.CONSUMER);
        item.setCurrentOwnerId(null); // Consumer not tracked by companyId
        productItemRepository.save(item);

        // Create movement
        ProductItemMovement saleMovement = buildSaleMovement(item, prevOwnerId, prevOwnerType, customerPhone, notes);
        movementRepository.save(saleMovement);

        // ========================================
        // BLOCKCHAIN INTEGRATION
        // ========================================
        String txHash = null;
        try {
            BigInteger batchId = item.getDrugBatch().getBatchId();
            String proofJson = item.getBlockchainMerkleProof();
            
            if (proofJson != null && !proofJson.isEmpty() && item.getIsBlockchainSynced()) {
                java.util.List<String> merkleProof = new com.fasterxml.jackson.databind.ObjectMapper().readValue(proofJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                
                // 1 maps to ItemStatus.SOLD in the Solidity Enum
                BigInteger customStatusSold = BigInteger.valueOf(1); 
                
                log.info("🔗 Recording SALE item on blockchain: itemCode={}, batchId={}", item.getItemCode(), batchId);
                
                TransactionReceipt receipt = blockchainService.updateItemStatus(
                        batchId,
                        item.getItemCode(),
                        customStatusSold,
                        "Dispensed to consumer",
                        merkleProof
                ).get(30, java.util.concurrent.TimeUnit.SECONDS);

                if (receipt != null && "0x1".equals(receipt.getStatus())) {
                    txHash = receipt.getTransactionHash();
                    log.info("✅ Blockchain SALE update success for {}. Tx: {}", item.getItemCode(), txHash);
                } else {
                    log.error("❌ Blockchain SALE update failed for {}!", item.getItemCode());
                }
            } else {
                log.warn("⚠️ Item {} is not synced to blockchain. Skipping on-chain update.", item.getItemCode());
            }
        } catch (Exception e) {
            log.error("❌ Failed to initiate blockchain SALE status update: {}", e.getMessage());
        }

        return DispenseResult.builder()
                .success(true)
                .itemCode(itemCode)
                .currentStatus(ProductItem.ItemStatus.SOLD.name())
                .statusMessage("OK, có thể bán")
                .movementId(saleMovement.getId())
                .transactionHash(txHash)
                .build();
    }

    /**
     * Create SALE movement entry
     */
    private ProductItemMovement buildSaleMovement(
            ProductItem item,
            Long prevOwnerId,
            ProductItem.OwnerType prevOwnerType,
            String customerPhone,
            String notes
    ) {
        ProductItemMovement movement = new ProductItemMovement();
        movement.setProductItem(item);
        movement.setDrugBatch(item.getDrugBatch());
        movement.setMovementType(ProductItemMovement.MovementType.SALE);

        // From = previous owner (usually PHARMACY)
        movement.setFromCompanyId(prevOwnerId);
        movement.setFromCompanyType(prevOwnerType);
        movement.setFromCompanyName(prevOwnerType != null ? prevOwnerType.name() : null);

        // To = CONSUMER (use pharmacy ID as placeholder since toCompanyId is @NotNull)
        // The toCompanyType=CONSUMER indicates this is a sale to end consumer
        movement.setToCompanyId(prevOwnerId != null ? prevOwnerId : 1L); // Use pharmacy ID as reference
        movement.setToCompanyType(ProductItem.OwnerType.CONSUMER);
        movement.setToCompanyName("Nguoi tieu dung"); // Remove Vietnamese characters
        movement.setToAddressDetail(customerPhone != null ? ("Khach hang: " + customerPhone) : null);

        movement.setMovementTimestamp(LocalDateTime.now());
        movement.setVerificationMethod(ProductItemMovement.VerificationMethod.QR_SCAN);

        String baseNotes = "Dispensed to consumer";
        if (notes != null && !notes.isBlank()) {
            baseNotes += " - " + notes;
        }
        movement.setNotes(baseNotes);

        movement.setIsBlockchainSynced(false);
        return movement;
    }

    /**
     * Parse itemCode into batchId and sequence
     * Expected format: PREFIX-BATCH000001-0007
     */
    private ParsedCode parseItemCode(String itemCode) {
        try {
            // Split by '-'
            String[] parts = itemCode.split("-");
            if (parts.length < 3) return null;

            // parts[1] = BATCH000001
            String batchStr = parts[1];
            if (!batchStr.startsWith("BATCH")) return null;
            String batchNum = batchStr.substring("BATCH".length());

            // parts[2] = 0007
            String seqStr = parts[2];

            Long batchId = Long.parseLong(batchNum);
            Integer sequence = Integer.parseInt(seqStr);
            return new ParsedCode(batchId, sequence);
        } catch (Exception e) {
            log.debug("Cannot parse itemCode '{}': {}", itemCode, e.getMessage());
            return null;
        }
    }

    @Data
    private static class ParsedCode {
        private final Long batchId;
        private final Integer sequence;

        public ParsedCode(Long batchId, Integer sequence) {
            this.batchId = batchId;
            this.sequence = sequence;
        }
    }

    @Data
    @Builder
    public static class DispenseResult {
        private boolean success;
        private String itemCode;
        private String currentStatus;
        private String statusMessage;
        private boolean alreadyDispensed;
        private Long movementId;
        private String transactionHash;
    }

    /**
     * Read-only status check for an item code.
     * Fast path: does not modify DB or trigger blockchain calls.
     * Returns whether the item can be sold and its current status.
     */
    public DispenseResult checkItemStatus(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Item code is required");
        }

        log.info("Kiểm tra trạng thái (READ-ONLY): itemCode='{}'", itemCode);
        ParsedCode parsed = parseItemCode(itemCode);
        if (parsed != null) {
            log.info("Parse: batchId='{}', sequence='{}'", parsed.batchId, parsed.sequence);
        }

        Optional<ProductItem> itemOpt = productItemRepository.findByItemCode(itemCode);
        if (itemOpt.isEmpty()) {
            return DispenseResult.builder()
                    .success(false)
                    .statusMessage("Không tìm thấy sản phẩm")
                    .itemCode(itemCode)
                    .build();
        }

        ProductItem item = itemOpt.get();
        if (item.getCurrentStatus() == ProductItem.ItemStatus.SOLD) {
            return DispenseResult.builder()
                    .success(false)
                    .itemCode(item.getItemCode())
                    .currentStatus(item.getCurrentStatus().name())
                    .statusMessage("Thuốc đã bán!")
                    .alreadyDispensed(true)
                    .build();
        }

        return DispenseResult.builder()
                .success(true)
                .itemCode(item.getItemCode())
                .currentStatus(item.getCurrentStatus().name())
                .statusMessage("OK, có thể bán")
                .alreadyDispensed(false)
                .build();
    }
}