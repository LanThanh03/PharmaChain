package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.DistributorInventory;
import com.nckh.dia5.model.PharmacyInventory;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.DistributorInventoryRepository;
import com.nckh.dia5.repository.PharmacyInventoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/batch/verify")
@RequiredArgsConstructor
@Tag(name = "Batch Verification", description = "APIs xác minh tính nhất quán của Batch ID")
public class BatchVerificationController {

    private final DrugBatchRepository drugBatchRepository;
    private final DistributorInventoryRepository distributorInventoryRepository;
    private final PharmacyInventoryRepository pharmacyInventoryRepository;

    @GetMapping("/consistency")
    @Operation(summary = "Kiểm tra tính nhất quán của Batch ID", 
               description = "Kiểm tra xem blockchain batch ID có nhất quán xuyên suốt chuỗi cung ứng không")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkBatchConsistency() {
        log.info("🔍 Starting batch ID consistency check...");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> inconsistencies = new ArrayList<>();
        int totalBatches = 0;
        int inconsistentCount = 0;

        // Get all batches
        List<DrugBatch> allBatches = drugBatchRepository.findAll();
        totalBatches = allBatches.size();

        for (DrugBatch batch : allBatches) {
            BigInteger batchId = batch.getBatchId();
            
            // Check distributor inventory
            List<DistributorInventory> distInventories = distributorInventoryRepository
                    .findByDrugBatchId(batch.getId());
            
            for (DistributorInventory inv : distInventories) {
                if (inv.getBlockchainBatchId() == null || 
                    !inv.getBlockchainBatchId().equals(batchId)) {
                    
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "DISTRIBUTOR_INVENTORY");
                    issue.put("inventory_id", inv.getId());
                    issue.put("expected_batch_id", batchId);
                    issue.put("actual_batch_id", inv.getBlockchainBatchId());
                    issue.put("batch_number", batch.getBatchNumber());
                    issue.put("distributor", inv.getDistributor().getName());
                    inconsistencies.add(issue);
                    inconsistentCount++;
                    
                    log.warn("❌ Inconsistency found in distributor_inventory: expected={}, actual={}", 
                            batchId, inv.getBlockchainBatchId());
                }
            }
            
            // Check pharmacy inventory
            List<PharmacyInventory> pharmInventories = pharmacyInventoryRepository
                    .findByDrugBatchId(batch.getId());
            
            for (PharmacyInventory inv : pharmInventories) {
                if (inv.getBlockchainBatchId() == null || 
                    !inv.getBlockchainBatchId().equals(batchId)) {
                    
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", "PHARMACY_INVENTORY");
                    issue.put("inventory_id", inv.getId());
                    issue.put("expected_batch_id", batchId);
                    issue.put("actual_batch_id", inv.getBlockchainBatchId());
                    issue.put("batch_number", batch.getBatchNumber());
                    issue.put("pharmacy", inv.getPharmacy().getName());
                    inconsistencies.add(issue);
                    inconsistentCount++;
                    
                    log.warn("❌ Inconsistency found in pharmacy_inventory: expected={}, actual={}", 
                            batchId, inv.getBlockchainBatchId());
                }
            }
        }

        result.put("total_batches", totalBatches);
        result.put("inconsistent_records", inconsistentCount);
        result.put("is_consistent", inconsistentCount == 0);
        result.put("inconsistencies", inconsistencies);

        if (inconsistentCount == 0) {
            log.info("✅ All batch IDs are consistent!");
            return ResponseEntity.ok(ApiResponse.success(result, 
                    "Tất cả Batch ID đều nhất quán trong hệ thống"));
        } else {
            log.warn("⚠️ Found {} inconsistent records", inconsistentCount);
            return ResponseEntity.ok(ApiResponse.success(result, 
                    String.format("Phát hiện %d bản ghi không nhất quán", inconsistentCount)));
        }
    }

    @PostMapping("/fix-inconsistencies")
    @Operation(summary = "Tự động sửa các batch ID không nhất quán", 
               description = "Cập nhật blockchain_batch_id trong inventory về đúng giá trị từ drug_batches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fixInconsistencies() {
        log.info("🔧 Starting batch ID inconsistency fix...");

        Map<String, Object> result = new HashMap<>();
        int fixedDistributor = 0;
        int fixedPharmacy = 0;

        List<DrugBatch> allBatches = drugBatchRepository.findAll();

        for (DrugBatch batch : allBatches) {
            BigInteger correctBatchId = batch.getBatchId();
            
            // Fix distributor inventory
            List<DistributorInventory> distInventories = distributorInventoryRepository
                    .findByDrugBatchId(batch.getId());
            
            for (DistributorInventory inv : distInventories) {
                if (inv.getBlockchainBatchId() == null || 
                    !inv.getBlockchainBatchId().equals(correctBatchId)) {
                    
                    log.info("🔧 Fixing distributor_inventory id={}: {} -> {}", 
                            inv.getId(), inv.getBlockchainBatchId(), correctBatchId);
                    
                    inv.setBlockchainBatchId(correctBatchId);
                    distributorInventoryRepository.save(inv);
                    fixedDistributor++;
                }
            }
            
            // Fix pharmacy inventory
            List<PharmacyInventory> pharmInventories = pharmacyInventoryRepository
                    .findByDrugBatchId(batch.getId());
            
            for (PharmacyInventory inv : pharmInventories) {
                if (inv.getBlockchainBatchId() == null || 
                    !inv.getBlockchainBatchId().equals(correctBatchId)) {
                    
                    log.info("🔧 Fixing pharmacy_inventory id={}: {} -> {}", 
                            inv.getId(), inv.getBlockchainBatchId(), correctBatchId);
                    
                    inv.setBlockchainBatchId(correctBatchId);
                    pharmacyInventoryRepository.save(inv);
                    fixedPharmacy++;
                }
            }
        }

        result.put("fixed_distributor_records", fixedDistributor);
        result.put("fixed_pharmacy_records", fixedPharmacy);
        result.put("total_fixed", fixedDistributor + fixedPharmacy);

        log.info("✅ Fixed {} distributor and {} pharmacy inventory records", 
                fixedDistributor, fixedPharmacy);

        return ResponseEntity.ok(ApiResponse.success(result, 
                String.format("Đã sửa %d bản ghi không nhất quán", 
                        fixedDistributor + fixedPharmacy)));
    }
}

