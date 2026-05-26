package com.nckh.dia5.service;

import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.DrugProduct;
import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.DrugProductRepository;
import com.nckh.dia5.repository.ProductItemRepository;
import com.nckh.dia5.util.BlockchainEncodingFixer;
import com.nckh.dia5.util.SafeFunctionEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Service để tạo ProductItem riêng lẻ với blockchain transaction riêng cho từng item
 * Đảm bảo mỗi ProductItem có thể quét QR và theo dõi độc lập
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndividualProductItemService {

    private final ProductItemRepository productItemRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugProductRepository drugProductRepository;
    private final BlockchainService blockchainService;
    private final BlockchainEncodingFixer encodingFixer;
    private final MerkleTreeService merkleTreeService;
    private final SafeFunctionEncoder safeFunctionEncoder;

    /**
     * Tạo ProductItem riêng lẻ với blockchain transaction riêng
     * @param batchId ID của batch
     * @param drugProductId ID của drug product
     * @param itemCode Mã item (nếu null sẽ tự generate)
     * @return ProductItem đã tạo
     */
    @Transactional
    public ProductItem createIndividualProductItem(Long batchId, Long drugProductId, String itemCode) {
        log.info("Creating individual ProductItem for batch: {}, product: {}", batchId, drugProductId);

        // Get batch info
        DrugBatch batch = drugBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        // Get drug product
        DrugProduct drugProduct = drugProductRepository.findById(drugProductId)
                .orElseThrow(() -> new RuntimeException("Drug product not found: " + drugProductId));

        // Generate item code if not provided
        if (itemCode == null || itemCode.trim().isEmpty()) {
            itemCode = generateUniqueItemCode(drugProduct.getName());
        }

        // Validate item code
        String validatedItemCode = encodingFixer.validateItemCode(itemCode);
        encodingFixer.logEncodingIssues(itemCode, validatedItemCode, "individual_item_code");

        // Check if item code already exists
        if (productItemRepository.existsByItemCode(validatedItemCode)) {
            throw new RuntimeException("Item code already exists: " + validatedItemCode);
        }

        // Create ProductItem
        ProductItem item = new ProductItem();
        item.setItemCode(validatedItemCode);
        item.setDrugBatch(batch);
        item.setDrugProduct(drugProduct);
        item.setCurrentStatus(ProductItem.ItemStatus.MANUFACTURED);
        item.setCurrentOwnerType(ProductItem.OwnerType.MANUFACTURER);
        item.setManufactureDate(batch.getManufactureTimestamp());
        item.setExpiryDate(batch.getExpiryDate());

        // Generate QR code data
        String qrCodeData = validatedItemCode;
        item.setQrCodeData(qrCodeData);
        item.setQrGeneratedAt(LocalDateTime.now());

        // Save to database first
        ProductItem savedItem = productItemRepository.save(item);
        log.info("✅ ProductItem saved to database: {}", savedItem.getItemCode());

        // Register on blockchain individually
        try {
            registerIndividualItemOnBlockchain(savedItem, batch);
        } catch (Exception e) {
            log.error("❌ Failed to register individual item on blockchain: {}", e.getMessage(), e);
            // Don't fail the entire operation, just log the error
            // The item is still saved in database and can be synced later
        }

        return savedItem;
    }

    /**
     * Tạo nhiều ProductItem riêng lẻ với blockchain transaction riêng cho từng item
     * @param batchId ID của batch
     * @param drugProductId ID của drug product
     * @param quantity Số lượng items cần tạo
     * @return List of ProductItems đã tạo
     */
    @Transactional
    public List<ProductItem> createMultipleIndividualProductItems(Long batchId, Long drugProductId, Integer quantity) {
        log.info("Creating {} individual ProductItems for batch: {}, product: {}", quantity, batchId, drugProductId);

        List<ProductItem> createdItems = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            try {
                ProductItem item = createIndividualProductItem(batchId, drugProductId, null);
                createdItems.add(item);
                log.info("✅ Created individual ProductItem {}/{}: {}", i + 1, quantity, item.getItemCode());
            } catch (Exception e) {
                log.error("❌ Failed to create individual ProductItem {}/{}: {}", i + 1, quantity, e.getMessage());
                // Continue with next item
            }
        }

        log.info("🎉 Successfully created {}/{} individual ProductItems", createdItems.size(), quantity);
        return createdItems;
    }

    /**
     * Đăng ký ProductItem riêng lẻ trên blockchain
     * @param item ProductItem
     * @param batch DrugBatch
     */
    private void registerIndividualItemOnBlockchain(ProductItem item, DrugBatch batch) {
        log.info("🔗 Registering individual ProductItem on blockchain: {}", item.getItemCode());

        try {
            // Create individual batch for this single item
            BigInteger individualBatchId = generateIndividualBatchId(item);
            
            // Create Merkle tree with single item
            List<String> singleItemCode = List.of(item.getItemCode());
            MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(singleItemCode);
            String merkleRoot = merkleTree.getRoot();

            // Convert expiry date to timestamp
            BigInteger expiryTimestamp = BigInteger.valueOf(
                batch.getExpiryDate().toEpochSecond(java.time.ZoneOffset.UTC)
            );

            // STEP 1: Create individual batch on blockchain
            log.info("📦 Creating individual batch on blockchain for item: {}", item.getItemCode());
            
            BigInteger manufactureTimestamp = BigInteger.valueOf(
                batch.getManufactureTimestamp().toEpochSecond(java.time.ZoneOffset.UTC)
            );

            TransactionReceipt batchReceipt = blockchainService.createBatchWithItems(
                    batch.getDrugName(),
                    batch.getManufacturer(),
                    BigInteger.ONE, // Quantity = 1
                    manufactureTimestamp,
                    expiryTimestamp,
                    merkleRoot
            ).get(30, TimeUnit.SECONDS);

            if (batchReceipt != null && "0x1".equals(batchReceipt.getStatus())) {
                log.info("✅ Individual batch created on blockchain: TX={}", batchReceipt.getTransactionHash());
                
                // Update item with blockchain info
                updateItemBlockchainStatus(item, individualBatchId, batchReceipt.getTransactionHash());
            } else {
                throw new RuntimeException("Individual batch creation failed");
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("❌ Blockchain transaction failed: {}", e.getMessage(), e);
            throw new RuntimeException("Blockchain transaction failed", e);
        } catch (Exception e) {
            log.error("❌ Failed to register individual item on blockchain: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generate unique individual batch ID for single item
     * @param item ProductItem
     * @return Individual batch ID
     */
    private BigInteger generateIndividualBatchId(ProductItem item) {
        // Use item ID + timestamp to ensure uniqueness
        long timestamp = System.currentTimeMillis();
        return BigInteger.valueOf(item.getId() * 1000000 + (timestamp % 1000000));
    }

    /**
     * Generate unique item code from drug name
     * @param drugName Drug name
     * @return Unique item code
     */
    private String generateUniqueItemCode(String drugName) {
        // Clean drug name
        String cleanDrugName = encodingFixer.cleanForBlockchain(drugName);
        
        // Extract first and last character
        String firstWord = cleanDrugName.trim().split("\\s+")[0];
        if (firstWord.length() < 2) {
            firstWord = "DRUG";
        }
        
        char firstChar = Character.toUpperCase(firstWord.charAt(0));
        char lastChar = Character.toUpperCase(firstWord.charAt(firstWord.length() - 1));
        
        // Extract dosage number
        String dosage = "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*mg");
        java.util.regex.Matcher matcher = pattern.matcher(cleanDrugName);
        if (matcher.find()) {
            dosage = matcher.group(1);
        }
        
        // Generate unique suffix
        long timestamp = System.currentTimeMillis() % 10000000;
        String randomSuffix = String.format("%07d", timestamp);
        
        return String.format("%c%c%s-%s", firstChar, lastChar, dosage, randomSuffix);
    }

    /**
     * Update ProductItem with blockchain status
     * @param item ProductItem
     * @param batchId Batch ID
     * @param transactionHash Transaction hash
     */
    private void updateItemBlockchainStatus(ProductItem item, BigInteger batchId, String transactionHash) {
        try {
            item.setBlockchainTokenId(batchId.longValue());
            item.setIsBlockchainSynced(true);
            item.setNotes("Blockchain TX: " + transactionHash);
            
            productItemRepository.save(item);
            log.info("✅ Updated ProductItem blockchain status: {}", item.getItemCode());
        } catch (Exception e) {
            log.error("❌ Failed to update ProductItem blockchain status: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync existing ProductItem to blockchain individually
     * @param itemId ProductItem ID
     * @return Success status
     */
    @Transactional
    public boolean syncIndividualItemToBlockchain(Long itemId) {
        try {
            ProductItem item = productItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("ProductItem not found: " + itemId));

            if (item.getIsBlockchainSynced()) {
                log.info("ProductItem already synced to blockchain: {}", item.getItemCode());
                return true;
            }

            registerIndividualItemOnBlockchain(item, item.getDrugBatch());
            return true;

        } catch (Exception e) {
            log.error("Failed to sync individual item to blockchain: {}", e.getMessage(), e);
            return false;
        }
    }
}
