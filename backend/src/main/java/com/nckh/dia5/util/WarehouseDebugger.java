package com.nckh.dia5.util;

import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.repository.DrugBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

/**
 * Utility để debug warehouse inventory issue
 * 
 * Chạy khi application start để kiểm tra database
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseDebugger implements CommandLineRunner {

    private final DrugBatchRepository drugBatchRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("============================================================");
        log.info("🔍 WAREHOUSE INVENTORY DEBUGGER");
        log.info("============================================================");
        
        debugBatchById();
        debugAllBatches();
        debugCurrentOwners();
        debugWalletAddressSearch();
        
        log.info("============================================================");
        log.info("✅ DEBUGGER COMPLETED");
        log.info("============================================================");
    }

    /**
     * Debug batch cụ thể: 17616683956897301
     */
    private void debugBatchById() {
        log.info("\n📦 DEBUG BATCH 17616683956897301:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        BigInteger batchId = new BigInteger("17616683956897301");
        
        try {
            var batchOpt = drugBatchRepository.findByBatchId(batchId);
            
            if (batchOpt.isPresent()) {
                DrugBatch batch = batchOpt.get();
                log.info("✅ Batch found:");
                log.info("  - ID: {}", batch.getId());
                log.info("  - Batch ID: {}", batch.getBatchId());
                log.info("  - Drug Name: {}", batch.getDrugName());
                log.info("  - Batch Number: {}", batch.getBatchNumber());
                log.info("  - Quantity: {}", batch.getQuantity());
                log.info("  - Manufacturer: {}", batch.getManufacturer());
                log.info("  - Manufacturer Address: {}", batch.getManufacturerAddress());
                log.info("  - Current Owner: {}", batch.getCurrentOwner());
                log.info("  - Status: {}", batch.getStatus());
                log.info("  - Expiry Date: {}", batch.getExpiryDate());
                log.info("  - Is Synced: {}", batch.getIsSynced());
                
                // Check current owner
                String owner = batch.getCurrentOwner();
                if (owner == null) {
                    log.error("❌ CURRENT_OWNER IS NULL!");
                } else if (owner.isEmpty()) {
                    log.error("❌ CURRENT_OWNER IS EMPTY!");
                } else if (!owner.startsWith("0x")) {
                    log.error("❌ CURRENT_OWNER KHÔNG PHẢI WALLET ADDRESS: {}", owner);
                } else {
                    log.info("✅ Current owner valid: {}", owner);
                }
            } else {
                log.error("❌ Batch {} NOT FOUND in database!", batchId);
            }
        } catch (Exception e) {
            log.error("❌ Error querying batch", e);
        }
    }

    /**
     * Debug tất cả batches
     */
    private void debugAllBatches() {
        log.info("\n📊 ALL BATCHES IN DATABASE:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            List<DrugBatch> allBatches = drugBatchRepository.findAll();
            log.info("Total batches: {}", allBatches.size());
            
            allBatches.forEach(batch -> {
                log.info("  Batch {}: {} | Owner: {} | Qty: {} | Status: {}", 
                    batch.getBatchId(),
                    batch.getDrugName(),
                    batch.getCurrentOwner(),
                    batch.getQuantity(),
                    batch.getStatus()
                );
            });
        } catch (Exception e) {
            log.error("❌ Error querying all batches", e);
        }
    }

    /**
     * Debug distinct current owners
     */
    private void debugCurrentOwners() {
        log.info("\n👥 DISTINCT CURRENT OWNERS:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            List<DrugBatch> allBatches = drugBatchRepository.findAll();
            
            allBatches.stream()
                .map(DrugBatch::getCurrentOwner)
                .filter(owner -> owner != null && !owner.isEmpty())
                .distinct()
                .forEach(owner -> {
                    long count = allBatches.stream()
                        .filter(b -> owner.equals(b.getCurrentOwner()))
                        .count();
                    log.info("  - {}: {} batches", owner, count);
                });
        } catch (Exception e) {
            log.error("❌ Error querying owners", e);
        }
    }

    /**
     * Test search by wallet address
     */
    private void debugWalletAddressSearch() {
        log.info("\n🔍 TEST WALLET ADDRESS SEARCH:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        String[] testAddresses = {
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
            "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc",
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            "0x70997970c51812dc3a010c7d01b50e0d17dc79c8"
        };
        
        for (String address : testAddresses) {
            try {
                List<DrugBatch> batches = drugBatchRepository.findByCurrentOwner(address);
                log.info("  Address: {} → {} batches found", address, batches.size());
                
                batches.forEach(b -> log.info("    - Batch {}: {}", 
                    b.getBatchId(), b.getDrugName()));
            } catch (Exception e) {
                log.error("❌ Error searching address: {}", address, e);
            }
        }
    }
}

