
package com.nckh.dia5.service;

import com.google.zxing.WriterException;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.DrugProduct;
import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.model.ProductItemMovement;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.DrugProductRepository;
import com.nckh.dia5.repository.ProductItemMovementRepository;
import com.nckh.dia5.repository.ProductItemRepository;
import com.nckh.dia5.repository.DispenseInstructionRepository;
import com.nckh.dia5.repository.UserMedicationRecordRepository;
import com.nckh.dia5.util.VietnameseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service để quản lý product items (sản phẩm riêng lẻ)
 * OPTIMIZED FLOW: Generate items → Calculate Merkle root → Register to blockchain
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductItemService {

    private final ProductItemRepository productItemRepository;
    private final ProductItemMovementRepository movementRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugProductRepository drugProductRepository;
    private final QRCodeService qrCodeService;
    private final BlockchainService blockchainService;
    private final MerkleTreeService merkleTreeService;
    private final DispenseInstructionRepository dispenseInstructionRepository;
    private final UserMedicationRecordRepository userMedicationRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate items cho một batch
     */
    @Transactional
    public List<ProductItem> generateItemsForBatch(Long batchId, Integer quantity, String prefix) {
        log.info("Generating {} items for batch {}", quantity, batchId);

        DrugBatch batch = drugBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        DrugProduct drugProduct = drugProductRepository.findByName(batch.getDrugName())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Drug product not found: " + batch.getDrugName()));

        List<ProductItem> items = new ArrayList<>();

        for (int i = 1; i <= quantity; i++) {
            String itemCode = generateItemCode(prefix, batchId, i);

            if (productItemRepository.existsByItemCode(itemCode)) {
                log.warn("Item code already exists: {}, skipping...", itemCode);
                continue;
            }

            ProductItem item = new ProductItem();
            item.setItemCode(itemCode);
            item.setDrugBatch(batch);
            item.setDrugProduct(drugProduct);
            item.setCurrentStatus(ProductItem.ItemStatus.MANUFACTURED);
            item.setCurrentOwnerType(ProductItem.OwnerType.MANUFACTURER);
            item.setManufactureDate(batch.getManufactureTimestamp());
            item.setExpiryDate(batch.getExpiryDate());
            item.setQrCodeData(itemCode);
            item.setQrGeneratedAt(LocalDateTime.now());

            items.add(item);
        }

        List<ProductItem> savedItems = productItemRepository.saveAll(items);
        log.info("Successfully created {} items for batch {}", savedItems.size(), batchId);

        createManufactureMovements(savedItems, batch);

        return savedItems;
    }

    /**
     * Generate items with QR images (async)
     */
    @Transactional
    public List<ProductItem> generateItemsWithQRImages(Long batchId, Integer quantity, String prefix) {
        List<ProductItem> items = generateItemsForBatch(batchId, quantity, prefix);

        for (ProductItem item : items) {
            try {
                generateQRImageForItem(item);
            } catch (Exception e) {
                log.error("Failed to generate QR image for item: {}", item.getItemCode(), e);
            }
        }

        return items;
    }

    /**
     * Generate QR image cho item
     */
    @Transactional
    public void generateQRImageForItem(ProductItem item) throws WriterException, IOException {
        byte[] qrImageBytes = qrCodeService.generateQRCodeBytes(item.getItemCode());
        String imagePath = qrCodeService.getQRCodeFilePath(item.getItemCode());
        
        item.setQrImagePath(imagePath);
        productItemRepository.save(item);
        
        log.debug("Generated QR image for item: {}", item.getItemCode());
    }

    /**
     * Create MANUFACTURE movements for items
     */
    private void createManufactureMovements(List<ProductItem> items, DrugBatch batch) {
        List<ProductItemMovement> movements = new ArrayList<>();
        Long manufacturerCompanyId = 1L;

        for (ProductItem item : items) {
            ProductItemMovement movement = new ProductItemMovement();
            movement.setProductItem(item);
            movement.setDrugBatch(batch);
            movement.setMovementType(ProductItemMovement.MovementType.MANUFACTURE);
            movement.setFromCompanyId(null);
            movement.setFromCompanyType(null);
            movement.setToCompanyId(manufacturerCompanyId);
            movement.setToCompanyType(ProductItem.OwnerType.MANUFACTURER);
            movement.setToCompanyName(batch.getManufacturer());
            movement.setMovementTimestamp(batch.getManufactureTimestamp());
            movement.setVerificationMethod(ProductItemMovement.VerificationMethod.AUTO);
            movement.setNotes("Item manufactured");

            movements.add(movement);
        }

        movementRepository.saveAll(movements);
        log.info("Created {} MANUFACTURE movements", movements.size());
    }

    /**
     * Generate item code
     */
    private String generateItemCode(String prefix, Long batchId, int sequence) {
        return String.format("%s-BATCH%06d-%04d", prefix, batchId, sequence);
    }
    
    /**
     * Generate item code từ drug name và dosage (BỎ DẤU TIẾNG VIỆT)
     */
    private String generateItemCodeFromDrugName(String drugName) {
        String normalizedName = VietnameseUtils.removeVietnameseDiacritics(drugName);
        
        String cleanName = normalizedName.trim().split("\\s+")[0];
        char firstChar = Character.toUpperCase(cleanName.charAt(0));
        char lastChar = Character.toUpperCase(cleanName.charAt(cleanName.length() - 1));
        
        String dosage = "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*mg");
        java.util.regex.Matcher matcher = pattern.matcher(normalizedName);
        if (matcher.find()) {
            dosage = matcher.group(1);
        }
        
        String randomDigits = String.format("%07d", new java.util.Random().nextInt(10000000));
        
        return String.format("%c%c%s-%s", firstChar, lastChar, dosage, randomDigits);
    }
    
    /**
     * Auto-generate items khi tạo batch (gọi từ DrugTraceabilityService)
     * OPTIMIZED FLOW: Generate items → Calculate Merkle root → Register to blockchain
     */
    @Transactional
    public List<ProductItem> autoGenerateItemsForNewBatch(DrugBatch batch, Long quantity) {
        log.info("Auto-generating {} items for batch: {}", quantity, batch.getBatchNumber());
        
        // Find or create DrugProduct
        DrugProduct drugProduct = drugProductRepository.findByName(batch.getDrugName())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    log.warn("DrugProduct not found for '{}', creating placeholder", batch.getDrugName());
                    DrugProduct newProduct = new DrugProduct();
                    newProduct.setName(batch.getDrugName());
                    newProduct.setStatus("ACTIVE");
                    return drugProductRepository.save(newProduct);
                });
        
        List<ProductItem> items = new ArrayList<>();
        
        for (int i = 0; i < quantity; i++) {
            String itemCode;
            int attempts = 0;
            do {
                itemCode = generateItemCodeFromDrugName(batch.getDrugName());
                attempts++;
            } while (productItemRepository.existsByItemCode(itemCode) && attempts < 10);
            
            if (attempts >= 10) {
                log.error("Failed to generate unique item code after 10 attempts");
                throw new RuntimeException("Cannot generate unique item code");
            }
            
            ProductItem item = new ProductItem();
            item.setItemCode(itemCode);
            item.setDrugBatch(batch);
            item.setDrugProduct(drugProduct);
            item.setCurrentStatus(ProductItem.ItemStatus.MANUFACTURED);
            item.setCurrentOwnerType(ProductItem.OwnerType.MANUFACTURER);
            item.setManufactureDate(batch.getManufactureTimestamp());
            item.setExpiryDate(batch.getExpiryDate());
            item.setQrCodeData(itemCode);
            item.setQrGeneratedAt(LocalDateTime.now());
            
            items.add(item);
        }
        
        if (items.isEmpty()) {
            log.error("No items were generated!");
            throw new RuntimeException("Failed to generate items - list is empty");
        }
        
        // Batch save
        List<ProductItem> savedItems = productItemRepository.saveAll(items);
        log.info("Successfully auto-generated {} items for batch {}", savedItems.size(), batch.getBatchNumber());
        
        // Create MANUFACTURE movements
        createManufactureMovements(savedItems, batch);
        
        // ========================================
        // REGISTER BATCH & ITEMS ON BLOCKCHAIN
        // ========================================
        try {
            log.info("🔗 Registering batch and {} items on blockchain for batch {}", savedItems.size(), batch.getBatchNumber());
            
            // Extract item codes
            List<String> itemCodes = savedItems.stream()
                    .map(ProductItem::getItemCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toList());
            
            log.info("📋 Item codes to register: {}", itemCodes);
            
            if (itemCodes.isEmpty()) {
                log.error("❌ Item codes list is EMPTY! Cannot register to blockchain.");
                throw new RuntimeException("No item codes to register");
            }
            
            // STEP 1: Create Merkle Tree
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(itemCodes);
            String merkleRoot = merkleTree.getRoot();
            log.info("🌳 Created Merkle tree with root: {}", merkleRoot);
            
            // STEP 2: Create batch on PharmaLedgerOptimized contract with Merkle root
            log.info("📦 Creating batch on blockchain with Merkle root...");
            BigInteger expiryTimestamp = BigInteger.valueOf(
                batch.getExpiryDate().toEpochSecond(java.time.ZoneOffset.UTC)
            );
            BigInteger manufactureTimestamp = BigInteger.valueOf(
                batch.getManufactureTimestamp().toEpochSecond(java.time.ZoneOffset.UTC)
            );
            
            TransactionReceipt batchReceipt = blockchainService.createBatchWithItems(
                    batch.getDrugName(),
                    batch.getManufacturer(),
                    BigInteger.valueOf(quantity),
                    manufactureTimestamp,
                    expiryTimestamp,
                    merkleRoot
            ).get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            if (batchReceipt != null && "0x1".equals(batchReceipt.getStatus())) {
                log.info("✅ Batch created on blockchain successfully!");
                log.info("📝 Batch TX Hash: {}", batchReceipt.getTransactionHash());
                
                // Update batch with blockchain info
                batch.setTransactionHash(batchReceipt.getTransactionHash());
                batch.setBlockNumber(batchReceipt.getBlockNumber() != null ? batchReceipt.getBlockNumber() : BigInteger.ONE);
                
                // ✅ CRITICAL FIX: Extract the REAL batchId assigned by the blockchain counter
                blockchainService.extractBatchId(batchReceipt).ifPresent(realId -> {
                    log.info("🎯 EXTRACTED REAL blockchain batchId: {}", realId);
                    batch.setBatchId(realId);
                });
                
                batch.setItemsMerkleRoot(merkleRoot); // ← Save Merkle Root
                batch.setIsSynced(true);
                drugBatchRepository.save(batch);
                log.info("✅ Updated batch with blockchain TX: {}", batchReceipt.getTransactionHash());
                
                // Update items with blockchain info AND Merkle Proofs
                updateItemsBlockchainStatus(savedItems, batch.getBatchId(), batchReceipt.getTransactionHash());
                
            } else {
                log.error("❌ Batch creation failed on blockchain");
                throw new RuntimeException("Batch creation failed");
            }
            
        } catch (java.util.concurrent.TimeoutException te) {
            log.warn("⏱️ Blockchain registration timed out after 30 seconds. Items saved in database but not synced to blockchain yet.");
            log.warn("💡 You can retry blockchain sync later using the batch sync endpoint.");
        } catch (Exception e) {
            log.error("❌ Failed to register items on blockchain: {}", e.getMessage(), e);
            log.warn("📊 Items are saved in database but not yet synced to blockchain.");
            log.warn("💡 You can retry blockchain sync later.");
        }
        
        return savedItems;
    }
    
    /**
     * Register items to blockchain (deprecated)
     */
    public TransactionReceipt registerItemsToBlockchain(BigInteger batchId, List<String> itemCodes) throws Exception {
        log.warn("⚠️ registerItemsToBlockchain is deprecated in optimized mode. Items are verified via Merkle Proof.");
        return null;
    }
    
    /**
     * Update items with blockchain sync status and Merkle proof
     */
    @Transactional
    public void updateItemsBlockchainStatus(List<ProductItem> items, BigInteger batchId, String transactionHash) {
        try {
            log.info("Updating {} items with blockchain sync status", items.size());
            
            // Get all item codes for Merkle tree
            List<String> allItemCodes = items.stream()
                    .map(ProductItem::getItemCode)
                    .collect(Collectors.toList());
            
            // Create Merkle tree
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(allItemCodes);
            
            // Update each item
            for (ProductItem item : items) {
                // Generate Merkle proof for this item
                List<String> proof = merkleTreeService.generateProof(merkleTree, item.getItemCode());
                
                // Convert proof to JSON string
                String proofJson = String.join(",", proof);
                
                // Update item
                item.setBlockchainMerkleProof(proofJson);
                item.setIsBlockchainSynced(true);
                
                // Add blockchain info to notes
                String notes = item.getNotes() != null ? item.getNotes() : "";
                notes += String.format("\n[Blockchain] Registered in TX: %s at block %s", 
                        transactionHash, 
                        java.time.LocalDateTime.now());
                item.setNotes(notes);
            }
            
            // Save updates
            productItemRepository.saveAll(items);
            log.info("✅ Updated {} items with blockchain sync status", items.size());
            
        } catch (Exception e) {
            log.error("Failed to update items blockchain status: {}", e.getMessage(), e);
        }
    }

    /**
     * Find item by code
     */
    public Optional<ProductItem> findByItemCode(String itemCode) {
        return productItemRepository.findByItemCode(itemCode);
    }

    /**
     * Find items by batch
     */
    public List<ProductItem> findByBatchId(Long batchId) {
        return productItemRepository.findByDrugBatchId(batchId);
    }

    /**
     * Find items by batch (paginated)
     */
    public Page<ProductItem> findByBatchId(Long batchId, Pageable pageable) {
        return productItemRepository.findByDrugBatchId(batchId, pageable);
    }

    /**
     * Find items by owner
     */
    public List<ProductItem> findByOwner(Long ownerId, ProductItem.OwnerType ownerType) {
        return productItemRepository.findByCurrentOwnerIdAndCurrentOwnerType(ownerId, ownerType);
    }

    /**
     * Count all items
     */
    public long countAllItems() {
        return productItemRepository.count();
    }

    /**
     * Count blockchain synced items
     */
    public long countBlockchainSyncedItems() {
        return productItemRepository.countByIsBlockchainSynced(true);
    }

    /**
     * Get status counts
     */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        
        for (ProductItem.ItemStatus status : ProductItem.ItemStatus.values()) {
            long count = productItemRepository.countByCurrentStatus(status);
            counts.put(status.name(), count);
        }
        
        return counts;
    }

    /**
     * Find items by owner (paginated)
     */
    public Page<ProductItem> findByOwner(Long ownerId, ProductItem.OwnerType ownerType, Pageable pageable) {
        return productItemRepository.findByCurrentOwnerIdAndCurrentOwnerType(ownerId, ownerType, pageable);
    }

    /**
     * Search items
     */
    public Page<ProductItem> searchItems(String keyword, Pageable pageable) {
        return productItemRepository.searchItems(keyword, pageable);
    }

    /**
     * Search items by owner
     */
    public Page<ProductItem> searchItemsByOwner(
            Long ownerId,
            ProductItem.OwnerType ownerType,
            String keyword,
            Pageable pageable
    ) {
        return productItemRepository.searchItemsByOwner(ownerId, ownerType, keyword, pageable);
    }

    /**
     * Find expiring soon items
     */
    public List<ProductItem> findExpiringSoonItems(int daysThreshold) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(daysThreshold);
        return productItemRepository.findExpiringSoonItems(now, futureDate);
    }

    /**
     * Find expiring soon items by owner
     */
    public List<ProductItem> findExpiringSoonItemsByOwner(
            Long ownerId,
            ProductItem.OwnerType ownerType,
            int daysThreshold
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(daysThreshold);
        return productItemRepository.findExpiringSoonItemsByOwner(ownerId, ownerType, now, futureDate);
    }

    /**
     * Update item status
     */
    @Transactional
    public ProductItem updateItemStatus(Long itemId, ProductItem.ItemStatus newStatus) {
        ProductItem item = productItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        item.setCurrentStatus(newStatus);
        return productItemRepository.save(item);
    }

    /**
     * Update item owner
     */
    @Transactional
    public ProductItem updateItemOwner(
            Long itemId,
            Long newOwnerId,
            ProductItem.OwnerType newOwnerType
    ) {
        ProductItem item = productItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        item.setCurrentOwnerId(newOwnerId);
        item.setCurrentOwnerType(newOwnerType);
        return productItemRepository.save(item);
    }

    /**
     * Get statistics by owner
     */
    public Long countByOwner(Long ownerId, ProductItem.OwnerType ownerType) {
        return productItemRepository.countByOwner(ownerId, ownerType);
    }

    /**
     * Get statistics by owner and status
     */
    public Long countByOwnerAndStatus(
            Long ownerId,
            ProductItem.OwnerType ownerType,
            ProductItem.ItemStatus status
    ) {
        return productItemRepository.countByOwnerAndStatus(ownerId, ownerType, status);
    }

    /**
     * Get item codes by batch (for Merkle Tree)
     */
    public List<String> getItemCodesByBatchId(Long batchId) {
        return productItemRepository.findItemCodesByBatchId(batchId);
    }

    /**
     * Delete item (admin only)
     */
    @Transactional
    public void deleteItem(Long itemId) {
        // First delete from user medication records
        userMedicationRecordRepository.deleteByProductItemId(itemId);
        // Then delete from dispense instructions
        dispenseInstructionRepository.deleteByProductItemId(itemId);
        // Finally delete the item (movements and verifications are cascaded)
        productItemRepository.deleteById(itemId);
        log.info("Deleted item: {}", itemId);
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SellResult {
        private ProductItem item;
        private String transactionHash;
    }

    /**
     * ✅ NEW: Sell item at pharmacy counter (offchain + onchain)
     * Updates item status to SOLD and creates SALE movement
     */
    @Transactional
    public SellResult sellItem(Long itemId, Long pharmacyId, String pharmacyName, String buyerInfo) {
        ProductItem item = productItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
            
        // Check if item is already sold
        if (item.getCurrentStatus() == ProductItem.ItemStatus.SOLD) {
            throw new RuntimeException("Sản phẩm này đã được bán trước đó!");
        }
        
        // Check if item is valid for sale
        if (item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED) {
            throw new RuntimeException("Sản phẩm này đã bị thu hồi, không thể bán!");
        }
        
        if (item.getCurrentStatus() == ProductItem.ItemStatus.EXPIRED || item.isExpired()) {
            throw new RuntimeException("Sản phẩm này đã hết hạn, không thể bán!");
        }
        
        // Update item status
        item.setCurrentStatus(ProductItem.ItemStatus.SOLD);
        item.setCurrentOwnerType(ProductItem.OwnerType.CONSUMER);
        item.setCurrentOwnerId(null); // Consumer doesn't have an ID in system
        item.setSoldAt(LocalDateTime.now());
        
        // Add sale info to notes
        String saleNotes = String.format("[SALE] Sold at %s on %s. Buyer: %s", 
                pharmacyName != null ? pharmacyName : "Pharmacy ID " + pharmacyId,
                LocalDateTime.now(),
                buyerInfo != null ? buyerInfo : "Walk-in customer");
        item.setNotes(item.getNotes() != null ? item.getNotes() + "\n" + saleNotes : saleNotes);
        
        ProductItem savedItem = productItemRepository.save(item);
        
        // Create SALE movement
        ProductItemMovement saleMovement = new ProductItemMovement();
        saleMovement.setProductItem(savedItem);
        saleMovement.setDrugBatch(savedItem.getDrugBatch());
        saleMovement.setMovementType(ProductItemMovement.MovementType.SALE);
        saleMovement.setFromCompanyId(pharmacyId);
        saleMovement.setFromCompanyType(ProductItem.OwnerType.PHARMACY);
        saleMovement.setToCompanyId(pharmacyId != null ? pharmacyId : 1L);
        saleMovement.setToCompanyType(ProductItem.OwnerType.CONSUMER);
        saleMovement.setToCompanyName(buyerInfo != null ? buyerInfo : "Khach hang");
        saleMovement.setMovementTimestamp(LocalDateTime.now());
        saleMovement.setVerificationMethod(ProductItemMovement.VerificationMethod.QR_SCAN);
        saleMovement.setNotes("Counter sale at " + (pharmacyName != null ? pharmacyName : "pharmacy"));
        
        movementRepository.save(saleMovement);
        
        log.info("✅ Item {} sold at pharmacy {} - Status updated to SOLD", item.getItemCode(), pharmacyId);

        // ========================================
        // BLOCKCHAIN INTEGRATION
        // ========================================
        String txHash = null;
        try {
            BigInteger batchId = item.getDrugBatch().getBatchId();
            String proofJson = item.getBlockchainMerkleProof();
            
            if (proofJson != null && !proofJson.isEmpty() && item.getIsBlockchainSynced()) {
                List<String> merkleProof = objectMapper.readValue(proofJson, new TypeReference<List<String>>() {});
                
                // 1 maps to ItemStatus.SOLD in the Solidity Enum (AVAILABLE=0, SOLD=1, RECALLED=2, DAMAGED=3)
                BigInteger customStatusSold = BigInteger.valueOf(1); 
                
                log.info("🔗 Recording SALE item on blockchain: itemCode={}, batchId={}", item.getItemCode(), batchId);
                
                TransactionReceipt receipt = blockchainService.updateItemStatus(
                        batchId,
                        item.getItemCode(),
                        customStatusSold,
                        "Sold to consumer",
                        merkleProof
                ).get(30, java.util.concurrent.TimeUnit.SECONDS); // Block synchronously for the demo

                if (receipt != null && "0x1".equals(receipt.getStatus())) {
                    txHash = receipt.getTransactionHash();
                    log.info("✅ Blockchain SALE update success for {}. Tx: {}", item.getItemCode(), txHash);
                } else {
                    log.error("❌ Blockchain SALE update failed for {}!", item.getItemCode());
                }
            } else {
                log.warn("⚠️ Item {} is not synced to blockchain or missing Merkle proof. Skipping on-chain update.", item.getItemCode());
            }
        } catch (Exception e) {
            log.error("❌ Failed to initiate blockchain SALE status update: {}", e.getMessage());
        }
        
        return SellResult.builder()
                .item(savedItem)
                .transactionHash(txHash)
                .build();
    }
    
    /**
     * ✅ NEW: Sell item by item code (for counter sales via QR scan)
     */
    @Transactional
    public SellResult sellItemByCode(String itemCode, Long pharmacyId, String pharmacyName, String buyerInfo) {
        ProductItem item = productItemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với mã: " + itemCode));
        
        return sellItem(item.getId(), pharmacyId, pharmacyName, buyerInfo);
    }
    
    /**
     * ✅ NEW: Report item as damaged (Báo hỏng / Hoàn thuốc)
     */
    @Transactional
    public ProductItem reportDamagedItem(String itemCode, Long pharmacyId, String reason, String imageUrl) {
        ProductItem item = productItemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với mã: " + itemCode));
        
        // Prevent re-reporting if already damaged/recalled/sold
        if (item.getCurrentStatus() == ProductItem.ItemStatus.DAMAGED || 
            item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED ||
            item.getCurrentStatus() == ProductItem.ItemStatus.SOLD) {
            throw new RuntimeException("Không thể báo hỏng sản phẩm có trạng thái: " + item.getCurrentStatus());
        }

        // 1. Update ProductItem Status in DB
        item.setCurrentStatus(ProductItem.ItemStatus.DAMAGED);
        
        String notes = item.getNotes() != null ? item.getNotes() : "";
        notes += String.format("\n[DAMAGED REPORT] Time: %s. Reason: %s. Image: %s", 
                LocalDateTime.now(), reason, imageUrl != null ? imageUrl : "None");
        item.setNotes(notes);
        
        ProductItem savedItem = productItemRepository.save(item);

        // 2. Create Movement History
        ProductItemMovement movement = new ProductItemMovement();
        movement.setProductItem(savedItem);
        movement.setDrugBatch(savedItem.getDrugBatch());
        movement.setMovementType(ProductItemMovement.MovementType.DAMAGE);
        movement.setFromCompanyId(pharmacyId);
        movement.setFromCompanyType(ProductItem.OwnerType.PHARMACY);
        // Reporting back to the manufacturer / central system
        movement.setToCompanyId(1L); // Assuming Manufacturer 1
        movement.setToCompanyType(ProductItem.OwnerType.MANUFACTURER);
        movement.setToCompanyName(savedItem.getDrugBatch().getManufacturer());
        movement.setMovementTimestamp(LocalDateTime.now());
        movement.setVerificationMethod(ProductItemMovement.VerificationMethod.QR_SCAN);
        movement.setNotes(String.format("Damaged at Pharmacy. Reason: %s. Image: %s", reason, imageUrl));
        
        movementRepository.save(movement);
        
        // 3. Register to Blockchain via Smart Contract (Asynchronous)
        try {
            BigInteger batchId = item.getDrugBatch().getBatchId();
            String proofJson = item.getBlockchainMerkleProof();
            
            if (proofJson != null && !proofJson.isEmpty() && item.getIsBlockchainSynced()) {
                List<String> merkleProof = objectMapper.readValue(proofJson, new TypeReference<List<String>>() {});
                
                // 3 maps to ItemStatus.DAMAGED in the Solidity Enum (AVAILABLE=0, SOLD=1, RECALLED=2, DAMAGED=3)
                BigInteger customStatusDamaged = BigInteger.valueOf(3); 
                
                log.info("🔗 Reporting DAMAGED item on blockchain: itemCode={}, batchId={}", itemCode, batchId);
                
                blockchainService.updateItemStatus(
                        batchId,
                        itemCode,
                        customStatusDamaged,
                        reason,
                        merkleProof
                ).thenAccept(receipt -> {
                    if ("0x1".equals(receipt.getStatus())) {
                        log.info("✅ Blockchain DAMAGED update success for {}. Tx: {}", itemCode, receipt.getTransactionHash());
                    } else {
                        log.error("❌ Blockchain DAMAGED update failed for {}!", itemCode);
                    }
                }).exceptionally(ex -> {
                    log.error("❌ Blockchain TX threw exception: ", ex);
                    return null;
                });
            } else {
                log.warn("⚠️ Item {} is not synced to blockchain or missing Merkle proof. Skipping on-chain update.", itemCode);
            }
        } catch (Exception e) {
            log.error("❌ Failed to initiate blockchain DAMAGED status update: {}", e.getMessage());
        }

        return savedItem;
    }
    /**
     * ✅ NEW: Thu hồi toàn bộ sản phẩm trong một lô
     */
    @Transactional
    public void recallBatchItems(DrugBatch batch) {
        log.info("Recalling all items for batch: {}", batch.getBatchNumber());
        List<ProductItem> items = productItemRepository.findByDrugBatch(batch);
        for (ProductItem item : items) {
            item.setCurrentStatus(ProductItem.ItemStatus.RECALLED);
        }
        productItemRepository.saveAll(items);
        log.info("Successfully recalled {} items for batch {}", items.size(), batch.getBatchNumber());
    }
}
