package com.nckh.dia5.controller;

import com.nckh.dia5.dto.blockchain.*;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.service.DrugTraceabilityService;
import com.nckh.dia5.service.BlockchainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/blockchain/drugs")
@RequiredArgsConstructor
public class DrugTraceabilityController {

    private final DrugTraceabilityService drugTraceabilityService;
    private final DrugBatchRepository drugBatchRepository;
    private final BlockchainService blockchainService;

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<DrugBatchDto>> createBatch(
            @Valid @RequestBody CreateBatchRequest request) {
        log.info("Creating new drug batch: {}", request.getDrugName());
        DrugBatchDto batch = drugTraceabilityService.createBatch(request);
        return ResponseEntity.ok(ApiResponse.success(batch, "Tạo lô thuốc thành công"));
    }

    @PostMapping("/shipments")
    public ResponseEntity<ApiResponse<ShipmentDto>> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        log.info("Creating new shipment for batch: {}, toAddress: {}, quantity: {}", 
                 request.getBatchId(), request.getToAddress(), request.getQuantity());
        log.info("Request details: {}", request);
        ShipmentDto shipment = drugTraceabilityService.createShipment(request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Tạo shipment thành công"));
    }

    /**
     * ✅ MỚI: GỬI HÀNG - Nhà sản xuất/phân phối xác nhận đã gửi hàng
     * POST /api/blockchain/drugs/shipments/{shipmentId}/dispatch
     */
    @PostMapping("/shipments/{shipmentId}/dispatch")
    public ResponseEntity<ApiResponse<ShipmentDto>> dispatchShipment(
            @PathVariable BigInteger shipmentId,
            @RequestParam(required = false, defaultValue = "Manufacturer Warehouse") String dispatchLocation,
            @RequestParam(required = false, defaultValue = "") String notes) {
        
        log.info("Dispatching shipment: shipmentId={}, location={}, notes={}", 
                 shipmentId, dispatchLocation, notes);
        
        try {
            // Call blockchain service to dispatch shipment
            blockchainService.dispatchShipment(shipmentId, dispatchLocation, notes).get();
            
            // Get updated shipment info
            ShipmentDto shipment = drugTraceabilityService.getShipment(shipmentId);
            
            return ResponseEntity.ok(ApiResponse.success(shipment, "Gửi hàng thành công"));
            
        } catch (Exception e) {
            log.error("Failed to dispatch shipment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi gửi hàng: " + e.getMessage(), 400)
            );
        }
    }

    @PostMapping("/shipments/{shipmentId}/receive")
    public ResponseEntity<ApiResponse<ShipmentDto>> receiveShipment(
            @PathVariable BigInteger shipmentId) {
        log.info("Receiving shipment: {}", shipmentId);
        try {
            ShipmentDto shipment = drugTraceabilityService.receiveShipment(shipmentId);
            return ResponseEntity.ok(ApiResponse.success(shipment, "Nhận shipment thành công"));
        } catch (Exception e) {
            // Try to find by database ID instead
            log.info("Shipment not found by shipmentId: {}, trying by database ID", shipmentId);
            try {
                ShipmentDto shipment = drugTraceabilityService.receiveShipmentByDatabaseId(shipmentId);
                return ResponseEntity.ok(ApiResponse.success(shipment, "Nhận shipment thành công"));
            } catch (Exception ex) {
                log.error("Failed to receive shipment by both methods: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy shipment với ID: " + shipmentId, 400));
            }
        }
    }

    @PutMapping("/shipments/status")
    public ResponseEntity<ApiResponse<ShipmentDto>> updateShipmentStatus(
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        log.info("Updating shipment status: {} -> {}", request.getShipmentId(), request.getNewStatus());
        ShipmentDto shipment = drugTraceabilityService.updateShipmentStatus(request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Cập nhật trạng thái shipment thành công"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyDrug(
            @Valid @RequestBody VerifyDrugRequest request) {
        log.info("Verifying drug with QR code: {}", request.getQrCode());
        
        try {
            DrugBatchDto batch = drugTraceabilityService.verifyDrug(request);
            
            // Build verification response for Flutter app
            Map<String, Object> response = new HashMap<>();
            response.put("isValid", true);
            
            // Drug info
            Map<String, Object> drugInfo = new HashMap<>();
            drugInfo.put("name", batch.getDrugName());
            drugInfo.put("manufacturer", batch.getManufacturer());
            drugInfo.put("batchNumber", batch.getBatchNumber());
            drugInfo.put("expiryDate", batch.getExpiryDate());
            response.put("drugInfo", drugInfo);
            
            // Get ownership history from shipments
            List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByBatch(batch.getBatchId());
            List<Map<String, Object>> ownershipHistory = new ArrayList<>();
            
            // Add mint record
            Map<String, Object> mintRecord = new HashMap<>();
            mintRecord.put("action", "MINT");
            mintRecord.put("fromAddress", "SYSTEM");
            mintRecord.put("toAddress", batch.getManufacturerAddress());
            mintRecord.put("timestamp", batch.getManufactureTimestamp().toString());
            ownershipHistory.add(mintRecord);
            
            // Add transfer records
            for (ShipmentDto shipment : shipments) {
                if ("DELIVERED".equals(shipment.getStatus())) {
                    Map<String, Object> transferRecord = new HashMap<>();
                    transferRecord.put("action", "TRANSFER");
                    transferRecord.put("fromAddress", shipment.getFromAddress());
                    transferRecord.put("toAddress", shipment.getToAddress());
                    transferRecord.put("timestamp", shipment.getShipmentTimestamp().toString());
                    ownershipHistory.add(transferRecord);
                }
            }
            
            response.put("ownershipHistory", ownershipHistory);
            response.put("transactionHash", batch.getMintTransactionHash());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Xác minh thuốc thành công"));
            
        } catch (Exception e) {
            log.error("Drug verification failed: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("isValid", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Xác minh thuốc thất bại"));
        }
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<DrugBatchDto>>> getAllBatches() {
        log.info("Getting all drug batches");
        List<DrugBatchDto> batches = drugTraceabilityService.getAllBatches();
        log.info("Found {} batches in database", batches.size());
        for (DrugBatchDto batch : batches) {
            log.info("Batch: id={}, drugName={}, batchNumber={}", batch.getId(), batch.getDrugName(), batch.getBatchNumber());
        }
        return ResponseEntity.ok(ApiResponse.success(batches, "Lấy danh sách tất cả lô thuốc thành công"));
    }

    @GetMapping("/batches/debug")
    public ResponseEntity<ApiResponse<String>> debugBatches() {
        log.info("Debug: Getting raw batch count from repository");
        List<com.nckh.dia5.model.DrugBatch> rawBatches = drugBatchRepository.findAll();
        log.info("Raw batches count: {}", rawBatches.size());
        
        StringBuilder debug = new StringBuilder();
        debug.append("Raw batches in database: ").append(rawBatches.size()).append("\n");
        
        for (com.nckh.dia5.model.DrugBatch batch : rawBatches) {
            debug.append("Raw Batch: id=").append(batch.getId())
                 .append(", batchId=").append(batch.getBatchId())
                 .append(", drugName=").append(batch.getDrugName())
                 .append(", batchNumber=").append(batch.getBatchNumber())
                 .append("\n");
        }
        
        return ResponseEntity.ok(ApiResponse.success(debug.toString(), "Debug batch information"));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<DrugBatchDto>> getBatch(@PathVariable BigInteger batchId) {
        log.info("Getting batch: {}", batchId);
        DrugBatchDto batch = drugTraceabilityService.getBatch(batchId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Lấy thông tin lô thuốc thành công"));
    }

    @GetMapping("/batches/manufacturer/{manufacturerAddress}")
    public ResponseEntity<ApiResponse<List<DrugBatchDto>>> getBatchesByManufacturer(
            @PathVariable String manufacturerAddress) {
        log.info("Getting batches by manufacturer: {}", manufacturerAddress);
        List<DrugBatchDto> batches = drugTraceabilityService.getBatchesByManufacturer(manufacturerAddress);
        return ResponseEntity.ok(ApiResponse.success(batches, "Lấy danh sách lô thuốc theo nhà sản xuất thành công"));
    }

    @GetMapping("/batches/owner/{ownerAddress}")
    public ResponseEntity<ApiResponse<List<DrugBatchDto>>> getBatchesByOwner(
            @PathVariable String ownerAddress) {
        log.info("Getting batches by owner: {}", ownerAddress);
        List<DrugBatchDto> batches = drugTraceabilityService.getBatchesByOwner(ownerAddress);
        return ResponseEntity.ok(ApiResponse.success(batches, "Lấy danh sách lô thuốc theo chủ sở hữu thành công"));
    }

    @GetMapping("/batches/{batchId}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getShipmentsByBatch(
            @PathVariable BigInteger batchId) {
        log.info("Getting shipments for batch: {}", batchId);
        List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByBatch(batchId);
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách shipment theo lô thuốc thành công"));
    }

    /**
     * ✅ MỚI: Smart search - Tìm shipments theo Số lô (BT202512121857) hoặc Batch ID
     * API này ưu tiên tìm theo Số lô (batchNumber) trước, nếu không tìm thấy sẽ thử Batch ID
     */
    @GetMapping("/batches/search/{identifier}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> searchShipmentsByBatchIdentifier(
            @PathVariable String identifier) {
        log.info("Smart search shipments for identifier: {}", identifier);
        try {
            List<ShipmentDto> shipments = drugTraceabilityService.searchShipmentsByBatchIdentifier(identifier);
            return ResponseEntity.ok(ApiResponse.success(shipments, 
                "Tìm thấy " + shipments.size() + " shipment cho mã: " + identifier));
        } catch (ResourceNotFoundException e) {
            log.warn("No shipments found for identifier: {}", identifier);
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), 404));
        } catch (Exception e) {
            log.error("Error searching shipments: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi tìm kiếm: " + e.getMessage(), 400));
        }
    }

    /**
     * ✅ MỚI: Tìm shipments theo Số lô (batchNumber) - VD: BT202512121857
     */
    @GetMapping("/batches/by-number/{batchNumber}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getShipmentsByBatchNumber(
            @PathVariable String batchNumber) {
        log.info("Getting shipments for batch number (Số lô): {}", batchNumber);
        try {
            List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByBatchNumber(batchNumber);
            return ResponseEntity.ok(ApiResponse.success(shipments, 
                "Lấy danh sách shipment theo Số lô thành công"));
        } catch (ResourceNotFoundException e) {
            log.warn("Batch not found with number: {}", batchNumber);
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy lô hàng với Số lô: " + batchNumber, 404));
        } catch (Exception e) {
            log.error("Error getting shipments by batch number: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/batches/{batchId}/transactions")
    public ResponseEntity<ApiResponse<List<BlockchainTransactionDto>>> getBatchTransactionHistory(
            @PathVariable BigInteger batchId) {
        log.info("Getting transaction history for batch: {}", batchId);
        List<BlockchainTransactionDto> transactions = drugTraceabilityService.getBatchTransactionHistory(batchId);
        return ResponseEntity.ok(ApiResponse.success(transactions, "Lấy lịch sử giao dịch blockchain thành công"));
    }

    @PutMapping("/batches/{id}/status")
    public ResponseEntity<ApiResponse<DrugBatchDto>> updateBatchStatus(
            @PathVariable Long id,
            @RequestParam com.nckh.dia5.model.DrugBatch.BatchStatus status,
            @RequestParam(required = false) String reason) {
        log.info("Updating status for batch with DB ID {}: status={}, reason={}", id, status, reason);
        DrugBatchDto updatedBatch = drugTraceabilityService.updateBatchStatus(id, status, reason);
        return ResponseEntity.ok(ApiResponse.success(updatedBatch, "Cập nhật trạng thái lô thuốc thành công"));
    }

    @GetMapping("/batches/ready-for-shipment")
    public ResponseEntity<ApiResponse<List<DrugBatchDto>>> getBatchesReadyForShipment() {
        log.info("Getting batches ready for shipment");
        List<DrugBatchDto> batches = drugTraceabilityService.getBatchesReadyForShipment();
        return ResponseEntity.ok(ApiResponse.success(batches, "Lấy danh sách lô thuốc sẵn sàng gửi thành công"));
    }

    @GetMapping("/distributors")
    public ResponseEntity<ApiResponse<List<DistributorDto>>> getDistributors() {
        log.info("Getting available distributors");
        List<DistributorDto> distributors = drugTraceabilityService.getDistributors();
        return ResponseEntity.ok(ApiResponse.success(distributors, "Lấy danh sách nhà phân phối thành công"));
    }

    @GetMapping("/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getAllShipments() {
        log.info("Getting all shipments");
        List<ShipmentDto> shipments = drugTraceabilityService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách lô hàng thành công"));
    }

    @GetMapping("/shipments/manufacturer/{manufacturerAddress}")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getShipmentsByManufacturer(
            @PathVariable String manufacturerAddress) {
        log.info("Getting shipments by manufacturer: {}", manufacturerAddress);
        List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByManufacturer(manufacturerAddress);
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách lô hàng theo nhà sản xuất thành công"));
    }

    @GetMapping("/shipments/pending")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getPendingShipments() {
        log.info("Getting pending shipments for pharmacy");
        List<ShipmentDto> shipments = drugTraceabilityService.getPendingShipments();
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách lô hàng chờ xử lý thành công"));
    }

    @GetMapping("/shipments/recipient/{recipientAddress}")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getShipmentsByRecipient(
            @PathVariable String recipientAddress) {
        log.info("Getting shipments by recipient: {}", recipientAddress);
        List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByRecipient(recipientAddress);
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách lô hàng theo người nhận thành công"));
    }

    @GetMapping("/shipments/sender/{senderAddress}")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getShipmentsBySender(
            @PathVariable String senderAddress) {
        log.info("Getting shipments by sender: {}", senderAddress);
        List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsBySender(senderAddress);
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách lô hàng theo người gửi thành công"));
    }

    @GetMapping("/debug/batches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDebugBatches() {
        log.info("DEBUG: Getting all batches");
        List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findAll();
        log.info("DEBUG: Found {} batches in database", batches.size());
        
        List<Map<String, Object>> debugInfo = batches.stream().map(batch -> {
            Map<String, Object> info = new HashMap<>();
            info.put("batchId", batch.getBatchId());
            info.put("drugName", batch.getDrugName());
            info.put("status", batch.getStatus());
            info.put("currentOwner", batch.getCurrentOwner());
            info.put("quantity", batch.getQuantity());
            log.info("DEBUG: Batch {} - {} - Status: {} - Owner: {}", 
                     batch.getBatchId(), batch.getDrugName(), batch.getStatus(), batch.getCurrentOwner());
            return info;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(debugInfo, "Debug: Lấy danh sách tất cả lô thuốc"));
    }

    @PostMapping("/debug/validate-shipment")
    public ResponseEntity<ApiResponse<String>> validateShipmentRequest(
            @RequestBody CreateShipmentRequest request) {
        log.info("DEBUG: Validating shipment request: {}", request);
        log.info("DEBUG: BatchId type: {}, value: {}", request.getBatchId().getClass(), request.getBatchId());
        log.info("DEBUG: ToAddress: '{}', length: {}", request.getToAddress(), request.getToAddress().length());
        log.info("DEBUG: Quantity: {}", request.getQuantity());
        
        return ResponseEntity.ok(ApiResponse.success("Validation passed", "Request is valid"));
    }

    @PostMapping("/debug/create-sample-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSampleBatch() {
        log.info("DEBUG: Creating sample batch for testing");
        try {
            // Create a simple batch manually
            BigInteger sampleBatchId = BigInteger.valueOf(System.currentTimeMillis());
            String manufacturerAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
            
            com.nckh.dia5.model.DrugBatch batch = new com.nckh.dia5.model.DrugBatch();
            batch.setBatchId(sampleBatchId);
            batch.setDrugName("Paracetamol 500mg Test");
            batch.setManufacturer("Test Manufacturer");
            batch.setBatchNumber("TEST-" + sampleBatchId);
            batch.setQuantity(1000L);
            batch.setManufacturerAddress(manufacturerAddress);
            batch.setCurrentOwner(manufacturerAddress);
            batch.setManufactureTimestamp(LocalDateTime.now());
            batch.setExpiryDate(LocalDateTime.now().plusYears(2));
            batch.setStorageConditions("Store in cool, dry place");
            batch.setStatus(com.nckh.dia5.model.DrugBatch.BatchStatus.MANUFACTURED);
            batch.setQrCode("QR-TEST-" + sampleBatchId);
            batch.setIsSynced(false);

            com.nckh.dia5.model.DrugBatch savedBatch = drugBatchRepository.save(batch);
            
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", savedBatch.getBatchId().toString());
            result.put("drugName", savedBatch.getDrugName());
            result.put("status", savedBatch.getStatus());
            result.put("currentOwner", savedBatch.getCurrentOwner());
            result.put("quantity", savedBatch.getQuantity());
            
            return ResponseEntity.ok(ApiResponse.success(result, "Sample batch created successfully"));
            
        } catch (Exception e) {
            log.error("Error creating sample batch: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create sample distributors: " + e.getMessage()));
        }
    }

    // Removed outdated debug endpoint for creating distributors (moved to pharma_companies)

    @GetMapping("/shipments/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentDto>> getShipmentById(@PathVariable BigInteger shipmentId) {
        log.info("Getting shipment by ID: {}", shipmentId);
        try {
            ShipmentDto shipment = drugTraceabilityService.getShipment(shipmentId);
            return ResponseEntity.ok(ApiResponse.success(shipment, "Lấy thông tin shipment thành công"));
        } catch (Exception e) {
            log.error("Failed to get shipment by ID: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy shipment với ID: " + shipmentId, 404));
        }
    }

    /**
     * ✅ MỚI: Lấy lịch sử đầy đủ của shipment (tất cả checkpoints)
     */
    @GetMapping("/shipments/{shipmentId}/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getShipmentHistory(
            @PathVariable BigInteger shipmentId) {
        try {
            log.info("Getting shipment history for shipmentId: {}", shipmentId);
            List<Map<String, Object>> history = blockchainService.getShipmentHistory(shipmentId);
            return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử shipment thành công"));
        } catch (Exception e) {
            log.error("Failed to get shipment history: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Không thể lấy lịch sử shipment: " + e.getMessage(), 500));
        }
    }
    
    /**
     * ✅ MỚI: Lấy chi tiết shipment kèm số lượng checkpoints
     */
    @GetMapping("/shipments/{shipmentId}/details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShipmentDetails(
            @PathVariable BigInteger shipmentId) {
        try {
            log.info("Getting shipment details for shipmentId: {}", shipmentId);
            Map<String, Object> details = blockchainService.getShipmentDetails(shipmentId);
            return ResponseEntity.ok(ApiResponse.success(details, "Lấy chi tiết shipment thành công"));
        } catch (Exception e) {
            log.error("Failed to get shipment details: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Không thể lấy chi tiết shipment: " + e.getMessage(), 500));
        }
    }
    
    /**
     * ✅ MỚI: Cập nhật trạng thái shipment + thêm checkpoint
     */
    @PostMapping("/shipments/{shipmentId}/checkpoint")
    public ResponseEntity<ApiResponse<String>> addShipmentCheckpoint(
            @PathVariable BigInteger shipmentId,
            @RequestParam Integer status,
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "") String notes) {
        try {
            log.info("Adding checkpoint for shipmentId: {}, status: {}, location: {}", 
                     shipmentId, status, location);
            
            blockchainService.updateShipmentStatus(
                shipmentId,
                BigInteger.valueOf(status),
                location,
                notes
            ).get();
            
            return ResponseEntity.ok(ApiResponse.success(
                "Checkpoint added successfully",
                "Đã thêm checkpoint thành công"
            ));
        } catch (Exception e) {
            log.error("Failed to add checkpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Không thể thêm checkpoint: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/shipments/{shipmentId}/verify-ownership")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyShipmentOwnership(
            @PathVariable BigInteger shipmentId,
            @RequestParam String expectedOwner) {
        try {
            log.info("Verifying shipment ownership: shipmentId={}, expectedOwner={}", shipmentId, expectedOwner);
            
            // Get shipment details
            ShipmentDto shipment = drugTraceabilityService.getShipment(shipmentId);
            
            // Check if the expectedOwner matches the recipient
            boolean isOwner = expectedOwner.equalsIgnoreCase(shipment.getToAddress());
            
            // Also verify on blockchain if needed
            if (isOwner && shipment.getBatchId() != null) {
                try {
                    isOwner = blockchainService.verifyOwnership(shipment.getBatchId(), expectedOwner).get();
                } catch (Exception e) {
                    log.warn("Could not verify on blockchain, using database check: {}", e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("shipmentId", shipmentId);
            result.put("expectedOwner", expectedOwner);
            result.put("actualRecipient", shipment.getToAddress());
            result.put("isOwner", isOwner);
            result.put("status", shipment.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success(result, "Xác minh quyền sở hữu shipment thành công"));
        } catch (Exception e) {
            log.error("Failed to verify shipment ownership", e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi xác minh quyền sở hữu shipment: " + e.getMessage()));
        }
    }

    @GetMapping("/debug/test-scan/{scanCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testScanCode(@PathVariable String scanCode) {
        log.info("DEBUG: Testing scan code: {}", scanCode);
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("scanCode", scanCode);
            
            // Test 1: Try as shipment ID
            try {
                BigInteger shipmentId = new BigInteger(scanCode);
                ShipmentDto shipment = drugTraceabilityService.getShipment(shipmentId);
                result.put("foundAsShipment", true);
                result.put("shipmentData", shipment);
                return ResponseEntity.ok(ApiResponse.success(result, "Found as shipment ID"));
            } catch (Exception e) {
                result.put("foundAsShipment", false);
                result.put("shipmentError", e.getMessage());
            }
            
            // Test 2: Try as batch ID
            try {
                BigInteger batchId = new BigInteger(scanCode);
                List<ShipmentDto> shipments = drugTraceabilityService.getShipmentsByBatch(batchId);
                result.put("foundAsBatch", true);
                result.put("shipmentsCount", shipments.size());
                result.put("shipmentsData", shipments);
                return ResponseEntity.ok(ApiResponse.success(result, "Found as batch ID"));
            } catch (Exception e) {
                result.put("foundAsBatch", false);
                result.put("batchError", e.getMessage());
            }
            
            result.put("conclusion", "Scan code not found as shipment ID or batch ID");
            return ResponseEntity.ok(ApiResponse.success(result, "Scan test completed"));
            
        } catch (Exception e) {
            log.error("Error testing scan code: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to test scan code: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        log.info("Getting dashboard statistics");
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get basic statistics
            List<DrugBatchDto> allBatches = drugTraceabilityService.getAllBatches();
            List<ShipmentDto> allShipments = drugTraceabilityService.getAllShipments();
            
            // Calculate statistics
            long totalBatches = allBatches.size();
            long totalShipments = allShipments.size();
            
            // Count by status
            long activeBatches = allBatches.stream()
                .filter(batch -> !"EXPIRED".equals(batch.getStatus()) && !"CONSUMED".equals(batch.getStatus()))
                .count();
            
            long pendingShipments = allShipments.stream()
                .filter(shipment -> "IN_TRANSIT".equals(shipment.getStatus()) || "PENDING".equals(shipment.getStatus()))
                .count();
            
            long deliveredShipments = allShipments.stream()
                .filter(shipment -> "DELIVERED".equals(shipment.getStatus()))
                .count();
            
            // Calculate total quantity
            long totalQuantity = allBatches.stream()
                .mapToLong(batch -> batch.getQuantity() != null ? batch.getQuantity() : 0L)
                .sum();
            
            // Build stats response
            stats.put("totalBatches", totalBatches);
            stats.put("activeBatches", activeBatches);
            stats.put("totalShipments", totalShipments);
            stats.put("pendingShipments", pendingShipments);
            stats.put("deliveredShipments", deliveredShipments);
            stats.put("totalQuantity", totalQuantity);
            
            // Recent activity (last 10 shipments)
            List<ShipmentDto> recentShipments = allShipments.stream()
                .sorted((s1, s2) -> s2.getShipmentTimestamp().compareTo(s1.getShipmentTimestamp()))
                .limit(10)
                .collect(Collectors.toList());
            stats.put("recentShipments", recentShipments);
            
            // Monthly statistics (simplified)
            Map<String, Long> monthlyStats = new HashMap<>();
            monthlyStats.put("currentMonth", (long) allShipments.stream()
                .filter(s -> s.getShipmentTimestamp().getMonthValue() == LocalDateTime.now().getMonthValue())
                .collect(Collectors.toList()).size());
            stats.put("monthlyStats", monthlyStats);
            
            return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê dashboard thành công"));
            
        } catch (Exception e) {
            log.error("Error getting dashboard stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get dashboard stats: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/debug/blockchain-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBlockchainInfo() {
        log.info("DEBUG: Getting blockchain information");
        try {
            Map<String, Object> info = new HashMap<>();
            
            // Check blockchain connectivity
            info.put("blockchainConnected", "Checking...");
            info.put("latestBlock", "Fetching...");
            info.put("contractAddress", "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512");
            info.put("networkId", "31337"); // Hardhat local network
            
            return ResponseEntity.ok(ApiResponse.success(info, "Blockchain info retrieved"));
            
        } catch (Exception e) {
            log.error("Error getting blockchain info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get blockchain info: " + e.getMessage(), 400));
        }
    }

    /**
     * API dành riêng cho nhà phân phối để xem shipments được gửi tới họ
     * Bao gồm cả xác thực để đảm bảo chỉ nhà phân phối đó mới có thể xem
     */
    @GetMapping("/distributor/my-shipments/{distributorAddress}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyShipments(
            @PathVariable String distributorAddress,
            @RequestParam(required = false) String status) {
        
        log.info("Getting shipments for distributor: {}, status filter: {}", distributorAddress, status);
        
        try {
            // Validate address format
            if (!distributorAddress.matches("^0x[a-fA-F0-9]{40}$")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Địa chỉ ví không hợp lệ", 400)
                );
            }
            
            // Get shipments sent to this distributor
            List<ShipmentDto> allShipments = drugTraceabilityService.getShipmentsByRecipient(distributorAddress);
            
            // Filter by status if provided
            List<ShipmentDto> filteredShipments = allShipments;
            if (status != null && !status.isEmpty()) {
                filteredShipments = allShipments.stream()
                    .filter(shipment -> status.equalsIgnoreCase(shipment.getStatus()))
                    .collect(Collectors.toList());
            }
            
            // Prepare response with summary statistics
            Map<String, Object> response = new HashMap<>();
            response.put("distributorAddress", distributorAddress);
            response.put("shipments", filteredShipments);
            response.put("totalCount", filteredShipments.size());
            
            // Count by status
            Map<String, Long> statusCounts = allShipments.stream()
                .collect(Collectors.groupingBy(
                    ShipmentDto::getStatus, 
                    Collectors.counting()
                ));
            response.put("statusSummary", statusCounts);
            
            // Recent shipments (last 5)
            List<ShipmentDto> recentShipments = filteredShipments.stream()
                .sorted((s1, s2) -> s2.getShipmentTimestamp().compareTo(s1.getShipmentTimestamp()))
                .limit(5)
                .collect(Collectors.toList());
            response.put("recentShipments", recentShipments);
            
            String message = String.format("Lấy danh sách %d shipments cho nhà phân phối %s thành công", 
                                         filteredShipments.size(), distributorAddress);
            
            return ResponseEntity.ok(ApiResponse.success(response, message));
            
        } catch (Exception e) {
            log.error("Error getting shipments for distributor {}: {}", distributorAddress, e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi lấy danh sách shipments: " + e.getMessage(), 400)
            );
        }
    }

    /**
     * API để nhà phân phối xem chi tiết một shipment cụ thể (với xác thực quyền sở hữu)
     */
    @GetMapping("/distributor/shipment/{shipmentId}/details/{distributorAddress}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShipmentDetailsForDistributor(
            @PathVariable BigInteger shipmentId,
            @PathVariable String distributorAddress) {
        
        log.info("Getting shipment details: shipmentId={}, distributorAddress={}", shipmentId, distributorAddress);
        
        try {
            // Get shipment details
            ShipmentDto shipment = drugTraceabilityService.getShipment(shipmentId);
            
            // Verify ownership - only the recipient can view details
            if (!distributorAddress.equalsIgnoreCase(shipment.getToAddress())) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("Bạn không có quyền xem shipment này", 403)
                );
            }
            
            // Get batch information
            DrugBatchDto batch = drugTraceabilityService.getBatch(shipment.getBatchId());
            
            // Get transaction history for this shipment
            List<BlockchainTransactionDto> transactions = 
                drugTraceabilityService.getBatchTransactionHistory(shipment.getBatchId());
            
            // Prepare detailed response
            Map<String, Object> response = new HashMap<>();
            response.put("shipment", shipment);
            response.put("batch", batch);
            response.put("transactions", transactions);
            response.put("canReceive", "IN_TRANSIT".equals(shipment.getStatus()));
            
            // Add tracking timeline
            List<Map<String, Object>> timeline = new ArrayList<>();
            
            // Add shipment created event
            Map<String, Object> createdEvent = new HashMap<>();
            createdEvent.put("event", "SHIPMENT_CREATED");
            createdEvent.put("timestamp", shipment.getShipmentTimestamp());
            createdEvent.put("description", "Lô hàng được tạo và bắt đầu vận chuyển");
            createdEvent.put("status", "COMPLETED");
            timeline.add(createdEvent);
            
            // Add in-transit event if status is IN_TRANSIT or DELIVERED
            if ("IN_TRANSIT".equals(shipment.getStatus()) || "DELIVERED".equals(shipment.getStatus())) {
                Map<String, Object> transitEvent = new HashMap<>();
                transitEvent.put("event", "IN_TRANSIT");
                transitEvent.put("timestamp", shipment.getShipmentTimestamp());
                transitEvent.put("description", "Lô hàng đang trong quá trình vận chuyển");
                transitEvent.put("status", "COMPLETED");
                timeline.add(transitEvent);
            }
            
            // Add delivered event if status is DELIVERED
            if ("DELIVERED".equals(shipment.getStatus())) {
                Map<String, Object> deliveredEvent = new HashMap<>();
                deliveredEvent.put("event", "DELIVERED");
                deliveredEvent.put("timestamp", shipment.getUpdatedAt());
                deliveredEvent.put("description", "Lô hàng đã được giao thành công");
                deliveredEvent.put("status", "COMPLETED");
                timeline.add(deliveredEvent);
            }
            
            response.put("timeline", timeline);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết shipment thành công"));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(
                ApiResponse.error("Không tìm thấy shipment", 404)
            );
        } catch (Exception e) {
            log.error("Error getting shipment details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi lấy chi tiết shipment: " + e.getMessage(), 400)
            );
        }
    }
}
