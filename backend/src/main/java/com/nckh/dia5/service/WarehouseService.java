    package com.nckh.dia5.service;

import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.repository.DrugBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để quản lý kho hàng (Warehouse Inventory)
 * Tích hợp với Smart Contract PharmaLedgerOptimized
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final DrugBatchRepository drugBatchRepository;
    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"; // Updated after re-deploy

    /**
     * ✅ LẤY DANH SÁCH HÀNG TRONG KHO - QUERY TỪ DATABASE (NHANH)
     * 
     * @param walletAddress Địa chỉ ví blockchain của NPP/Hiệu thuốc (có thể null để lấy tất cả)
     * @return List of batches trong kho
     */
    @Transactional(readOnly = true)
    public List<DrugBatch> getWarehouseInventory(String walletAddress) {
        log.info("🏪 Getting warehouse inventory for wallet: {}", walletAddress);
        
        List<DrugBatch> batches;
        
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            // Nếu không có wallet, lấy tất cả batches MANUFACTURED hoặc IN_TRANSIT hoặc DELIVERED
            log.info("📦 No wallet specified, getting all available batches");
            List<DrugBatch> manufactured = drugBatchRepository.findByStatus(DrugBatch.BatchStatus.MANUFACTURED);
            List<DrugBatch> inTransit = drugBatchRepository.findByStatus(DrugBatch.BatchStatus.IN_TRANSIT);
            List<DrugBatch> delivered = drugBatchRepository.findByStatus(DrugBatch.BatchStatus.DELIVERED);
            
            batches = new java.util.ArrayList<>();
            batches.addAll(manufactured);
            batches.addAll(inTransit);
            batches.addAll(delivered);
        } else {
            // Try cả hai cách: với và không lowercase
            batches = drugBatchRepository.findByCurrentOwner(walletAddress);
            
            if (batches.isEmpty()) {
                log.info("🔍 Try with lowercase");
                batches = drugBatchRepository.findByCurrentOwner(walletAddress.toLowerCase());
            }
            
            // Lọc: không lấy SOLD
            batches = batches.stream()
                .filter(b -> b.getStatus() != DrugBatch.BatchStatus.SOLD)
                .collect(java.util.stream.Collectors.toList());
        }
        
        log.info("✅ Found {} batches in warehouse", batches.size());
        
        // Log chi tiết để debug
        batches.forEach(b -> log.debug("  - Batch {}: {} (owner: {})", 
            b.getBatchId(), b.getDrugName(), b.getCurrentOwner()));
        
        return batches;
    }

    /**
     * ✅ LẤY DANH SÁCH HÀNG CÓ THỂ XUẤT KHO
     * Lọc ra những lô đủ điều kiện để xuất
     * 
     * @param walletAddress Địa chỉ ví blockchain (có thể null)
     * @return List of exportable batches
     */
    @Transactional(readOnly = true)
    public List<DrugBatch> getExportableBatches(String walletAddress) {
        log.info("📦 Getting exportable batches for wallet: {}", walletAddress);
        
        List<DrugBatch> allBatches = getWarehouseInventory(walletAddress);
        
        log.info("📊 Total batches before filtering: {}", allBatches.size());
        
        // ✅ LỌC: QUANTITY > 0 VÀ CHƯA HẾT HẠN
        // Không cho phép xuất kho thuốc đã hết hạn
        List<DrugBatch> exportable = allBatches.stream()
            .filter(batch -> {
                boolean hasQuantity = batch.getQuantity() != null && batch.getQuantity() > 0;
                
                // Check if expired
                boolean isExpired = false;
                if (batch.getExpiryDate() != null) {
                    isExpired = batch.getExpiryDate().isBefore(java.time.LocalDateTime.now());
                    log.debug("Batch {}: quantity={}, hasQuantity={}, expiryDate={}, isExpired={}", 
                        batch.getBatchId(), batch.getQuantity(), hasQuantity, 
                        batch.getExpiryDate(), isExpired);
                } else {
                    log.debug("Batch {}: quantity={}, hasQuantity={}, expiryDate=null", 
                        batch.getBatchId(), batch.getQuantity(), hasQuantity);
                }
                
                // ✅ Chỉ cho phép xuất kho nếu: có số lượng VÀ chưa hết hạn
                return hasQuantity && !isExpired;
            })
            .collect(Collectors.toList());
        
        log.info("✅ Found {} exportable batches (filtered from {})", 
            exportable.size(), allBatches.size());
        
        return exportable;
    }

    /**
     * ✅ ĐỒNG BỘ TỪ BLOCKCHAIN VỀ DATABASE
     * Gọi smart contract để lấy danh sách batches của owner
     * và cập nhật vào database
     * 
     * @param walletAddress Địa chỉ ví blockchain
     */
    @Transactional
    public void syncFromBlockchain(String walletAddress) {
        log.info("🔄 Syncing batches from blockchain for wallet: {}", walletAddress);
        
        try {
            // Gọi function getBatchesByOwner(address) trên smart contract
            List<BigInteger> batchIds = getBatchesByOwnerFromBlockchain(walletAddress);
            
            log.info("📊 Blockchain reports {} batches owned by {}", batchIds.size(), walletAddress);
            
            // Cập nhật database
            int updated = 0;
            for (BigInteger batchId : batchIds) {
                Optional<DrugBatch> batchOpt = drugBatchRepository.findByBatchId(batchId);
                
                if (batchOpt.isPresent()) {
                    DrugBatch batch = batchOpt.get();
                    String currentOwner = batch.getCurrentOwner();
                    
                    if (currentOwner == null || !currentOwner.equalsIgnoreCase(walletAddress)) {
                        log.info("🔄 Updating ownership: Batch {} -> {}", batchId, walletAddress);
                        batch.setCurrentOwner(walletAddress.toLowerCase());
                        batch.setUpdatedAt(java.time.LocalDateTime.now());
                        drugBatchRepository.save(batch);
                        updated++;
                    }
                } else {
                    log.warn("⚠️ Batch {} exists on blockchain but not in database", batchId);
                }
            }
            
            log.info("✅ Sync completed: {} batches updated", updated);
            
        } catch (Exception e) {
            log.error("❌ Failed to sync from blockchain", e);
            throw new RuntimeException("Sync failed: " + e.getMessage());
        }
    }

    /**
     * ✅ KIỂM TRA BATCH CÓ THỂ XUẤT KHO KHÔNG
     * Gọi smart contract để verify
     * 
     * @param batchId ID của batch
     * @param walletAddress Địa chỉ ví
     * @return CanExportResult
     */
    public CanExportResult canExportBatch(BigInteger batchId, String walletAddress) {
        log.info("🔍 Checking if batch {} can be exported by {}", batchId, walletAddress);
        
        // Step 1: Quick check từ database
        Optional<DrugBatch> batchOpt = drugBatchRepository.findByBatchId(batchId);
        
        if (batchOpt.isEmpty()) {
            return new CanExportResult(false, "Batch not found in database");
        }
        
        DrugBatch batch = batchOpt.get();
        
        if (!batch.getCurrentOwner().equalsIgnoreCase(walletAddress)) {
            return new CanExportResult(false, "You are not the owner of this batch");
        }
        
        if (batch.getQuantity() <= 0) {
            return new CanExportResult(false, "Batch has no quantity");
        }
        
        if (batch.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            return new CanExportResult(false, "Batch has expired");
        }
        
        // Step 2: Verify với blockchain
        try {
            CanExportResult blockchainResult = canExportBatchFromBlockchain(batchId, walletAddress);
            log.info("✅ Blockchain verification: {}", blockchainResult.isCanExport());
            return blockchainResult;
        } catch (Exception e) {
            log.warn("⚠️ Blockchain verification failed, using database result", e);
            return new CanExportResult(true, "Verified from database (blockchain unavailable)");
        }
    }

    // ============================================================
    // PRIVATE HELPER METHODS - BLOCKCHAIN CALLS
    // ============================================================

    /**
     * Gọi smart contract: getBatchesByOwner(address)
     */
    private List<BigInteger> getBatchesByOwnerFromBlockchain(String ownerAddress) throws Exception {
        // Function signature: getBatchesByOwner(address)
        Function function = new Function(
            "getBatchesByOwner",
            Arrays.asList(new Address(ownerAddress)),
            Arrays.asList(
                new TypeReference<DynamicArray<Uint256>>() {}, // batchIds
                new TypeReference<DynamicArray<Uint256>>() {}, // quantities
                new TypeReference<DynamicArray<Uint8>>() {}    // statuses
            )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(
                credentials.getAddress(),
                contractAddress,
                encodedFunction
            ),
            DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            throw new RuntimeException("Blockchain call failed: " + response.getError().getMessage());
        }

        @SuppressWarnings("unchecked")
        List<Type> rawResults = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        
        if (rawResults.isEmpty()) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Uint256> batchIds = ((DynamicArray<Uint256>) rawResults.get(0)).getValue();
        
        return batchIds.stream()
            .map(Uint256::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Gọi smart contract: canExportBatch(uint256, address)
     */
    private CanExportResult canExportBatchFromBlockchain(BigInteger batchId, String ownerAddress) throws Exception {
        Function function = new Function(
            "canExportBatch",
            Arrays.asList(
                new Uint256(batchId),
                new Address(ownerAddress)
            ),
            Arrays.asList(
                new TypeReference<Bool>() {},          // canExport
                new TypeReference<Utf8String>() {}     // reason
            )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(
                credentials.getAddress(),
                contractAddress,
                encodedFunction
            ),
            DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            throw new RuntimeException("Blockchain call failed: " + response.getError().getMessage());
        }

        @SuppressWarnings("unchecked")
        List<Type> rawResults = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        
        boolean canExport = ((Bool) rawResults.get(0)).getValue();
        String reason = ((Utf8String) rawResults.get(1)).getValue();
        
        return new CanExportResult(canExport, reason);
    }

    // ============================================================
    // INNER CLASS - RESULT OBJECT
    // ============================================================

    /**
     * Result object cho canExportBatch
     */
    public static class CanExportResult {
        private final boolean canExport;
        private final String reason;

        public CanExportResult(boolean canExport, String reason) {
            this.canExport = canExport;
            this.reason = reason;
        }

        public boolean isCanExport() {
            return canExport;
        }

        public String getReason() {
            return reason;
        }
    }
}

