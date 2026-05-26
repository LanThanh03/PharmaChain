        package com.nckh.dia5.service;

import com.nckh.dia5.dto.blockchain.*;
import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.model.*;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.ShipmentRepository;
import com.nckh.dia5.repository.BlockchainTransactionRepository;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.nckh.dia5.util.VietnameseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugTraceabilityService {

    private final DrugBatchRepository drugBatchRepository;
    private final ShipmentRepository shipmentRepository;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final BlockchainService blockchainService;
    private final ShipmentAdapter shipmentAdapter;
    private final ProductItemService productItemService;
    private final PharmacyInventoryService pharmacyInventoryService;
    private final DistributorInventoryService distributorInventoryService;
    private final com.nckh.dia5.repository.RawMaterialBatchRepository rawMaterialBatchRepository;

    /**
     * Create a new drug batch
     * OPTIMIZED FLOW: Generate items FIRST, then create batch on blockchain with real Merkle root
     */
    @Transactional
    public DrugBatchDto createBatch(CreateBatchRequest request) {
        try {
            // Validate request
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }

            // Validate and deduct Raw Material (UC07 / AD08 Step 5 & 8)
            if (request.getRawMaterialBatchNumber() != null && !request.getRawMaterialBatchNumber().isEmpty()) {
                com.nckh.dia5.model.RawMaterialBatch rmBatch = rawMaterialBatchRepository.findByBatchNumber(request.getRawMaterialBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException("Lô nguyên liệu không tồn tại: " + request.getRawMaterialBatchNumber()));
                
                if (!"APPROVED".equals(rmBatch.getStatus())) {
                    throw new IllegalArgumentException("Lô nguyên liệu chưa được kiểm định đạt chuẩn!");
                }

                double amountUsed = request.getRawMaterialAmountUsed() != null ? request.getRawMaterialAmountUsed() : 0.0;
                if (amountUsed <= 0) {
                    throw new IllegalArgumentException("Định mức tiêu hao nguyên liệu phải lớn hơn 0");
                }
                if (amountUsed > rmBatch.getQuantity()) {
                    throw new IllegalArgumentException(String.format("Lượng nguyên liệu trong lô %s không đủ! Yêu cầu: %.2f %s, Hiện có: %.2f %s", 
                            rmBatch.getBatchNumber(), amountUsed, rmBatch.getUnit(), rmBatch.getQuantity(), rmBatch.getUnit()));
                }

                // Deduct inventory
                rmBatch.setQuantity(rmBatch.getQuantity() - amountUsed);
                rawMaterialBatchRepository.save(rmBatch);
            }
            
            // Use default manufacturer address for development (no auth required)
            String manufacturerAddress = getDefaultManufacturerAddress();

            // Generate unique batch ID
            BigInteger batchId = generateBatchId();
            log.info("=== GENERATED batchId: {} ===", batchId);
            
            // Generate QR code
            String qrCode = generateQrCode(batchId, request.getBatchNumber());

            log.info("Creating batch: batchId={}, drugName={}, manufacturer={}, quantity={}", 
                     batchId, request.getDrugName(), request.getManufacturer(), request.getQuantity());

            // Create local entity FIRST (needed for item generation)
            com.nckh.dia5.model.DrugBatch batch = new com.nckh.dia5.model.DrugBatch();
            batch.setBatchId(batchId);
            batch.setDrugName(request.getDrugName());
            batch.setManufacturer(request.getManufacturer());
            batch.setBatchNumber(request.getBatchNumber());
            batch.setQuantity(request.getQuantity());
            batch.setManufacturerAddress(manufacturerAddress);
            batch.setCurrentOwner(manufacturerAddress);
            batch.setManufactureTimestamp(LocalDateTime.now());
            batch.setExpiryDate(request.getExpiryDate());
            batch.setStorageConditions(request.getStorageConditions() != null ? request.getStorageConditions() : "Bảo quản ở nhiệt độ phòng");
            batch.setRawMaterialBatchNumber(request.getRawMaterialBatchNumber());
            batch.setRawMaterialAmountUsed(request.getRawMaterialAmountUsed());
            batch.setStatus(com.nckh.dia5.model.DrugBatch.BatchStatus.MANUFACTURED);
            batch.setQrCode(qrCode);
            batch.setTransactionHash("PENDING_" + System.currentTimeMillis());
            batch.setBlockNumber(BigInteger.ZERO);
            batch.setIsSynced(false);

            log.info("=== BEFORE SAVE: batchId={} ===", batch.getBatchId());
            batch = drugBatchRepository.save(batch);
            log.info("=== AFTER SAVE: id={}, batchId={} ===", batch.getId(), batch.getBatchId());
            drugBatchRepository.flush(); // Force immediate write to DB
            log.info("=== AFTER FLUSH: id={}, batchId={} ===", batch.getId(), batch.getBatchId());

            // STEP 1: Generate product items (this will also register to blockchain with Merkle root)
            // The autoGenerateItemsForNewBatch method now handles blockchain registration internally
            try {
                log.info("Auto-generating {} product items for batch {}", request.getQuantity(), batch.getBatchNumber());
                productItemService.autoGenerateItemsForNewBatch(batch, request.getQuantity());
                log.info("Successfully auto-generated product items and registered to blockchain");
                
                // If we reach here, blockchain registration was successful
                // Update batch with blockchain info (items service should have updated it)
                batch = drugBatchRepository.findById(batch.getId())
                        .orElseThrow(() -> new RuntimeException("Batch not found after item generation"));
                
            } catch (Exception e) {
                log.error("Failed to auto-generate product items: {}", e.getMessage(), e);
                // Don't fail the batch creation if item generation fails
                // Batch is saved in database, items can be generated later
            }

            log.info("Batch created successfully: id={}, batchId={}, blockchain={}", 
                     batch.getId(), batchId, batch.getIsSynced() ? "synced" : "pending");
            
            DrugBatchDto dto = mapToDrugBatchDto(batch);
            log.info("=== RETURNING DTO: batchId={} ===", dto.getBatchId());
            return dto;

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to create batch", e);
            throw new RuntimeException("Không thể tạo lô thuốc: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new shipment
     */
    // Removed @Transactional to avoid rollback issues with triggers
    public ShipmentDto createShipment(CreateShipmentRequest request) {
        String fromAddress = getDefaultManufacturerAddress();
        boolean blockchainSuccess = false;
        TransactionReceipt receipt = null;
        
        try {
            log.info("Starting shipment creation process...");
            
            // Find the batch
            log.info("Looking for batch with ID: {}", request.getBatchId());
            log.info("DEBUG: All batches in DB:");
            drugBatchRepository.flush(); // Ensure all pending changes are written
            List<com.nckh.dia5.model.DrugBatch> allBatches = drugBatchRepository.findAll();
            log.info("DEBUG: Total batches found: {}", allBatches.size());
            for (com.nckh.dia5.model.DrugBatch b : allBatches) {
                log.info("  - DB ID: {}, Batch ID: {}, Number: {}, Owner: {}", 
                    b.getId(), b.getBatchId(), b.getBatchNumber(), b.getCurrentOwner());
            }
            
            com.nckh.dia5.model.DrugBatch batch = drugBatchRepository.findByBatchId(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", request.getBatchId().toString()));
            log.info("Found batch: {} with owner: {}", batch.getBatchNumber(), batch.getCurrentOwner());

            // Verify ownership
            log.info("Verifying ownership: fromAddress={}, currentOwner={}", fromAddress, batch.getCurrentOwner());
            if (!batch.getCurrentOwner().equals(fromAddress)) {
                throw new IllegalStateException("Bạn không có quyền tạo shipment cho lô này");
            }

            // Verify quantity
            log.info("Verifying quantity: requested={}, available={}", request.getQuantity(), batch.getQuantity());
            if (request.getQuantity().compareTo(batch.getQuantity()) > 0) {
                throw new IllegalStateException("Số lượng shipment vượt quá số lượng trong lô");
            }

            // Generate shipment ID
            log.info("Generating shipment ID...");
            BigInteger shipmentId = generateShipmentId();
            log.info("Generated shipment ID: {}", shipmentId);

            log.info("Creating shipment: shipmentId={}, batchId={}, from={}, to={}, quantity={}", 
                     shipmentId, request.getBatchId(), fromAddress, request.getToAddress(), request.getQuantity());

            // Generate tracking info if not provided
            String trackingInfo = request.getTrackingInfo();
            if (trackingInfo == null || trackingInfo.trim().isEmpty()) {
                trackingInfo = "SHIPMENT-" + shipmentId;
            }
            
            // Try to find recipient name and normalize it
            String toLocationName = "Khach hang";
            Optional<PharmaCompany> toCompany = pharmaCompanyRepository.findByWalletAddress(request.getToAddress());
            if (toCompany.isPresent()) {
                String dbName = toCompany.get().getName();
                // Map legacy/placeholder names to correct names
                if (dbName.contains("XYZ") || dbName.contains("Distributor")) {
                    toLocationName = "CPC1 Hà Nội";
                } else if (dbName.contains("An Khang") || dbName.contains("Pharmacy")) {
                    toLocationName = "Long Châu";
                } else {
                    toLocationName = dbName;
                }
            }

            // Try to create shipment on blockchain
            try {
                receipt = blockchainService.createAndDispatchShipment(
                    request.getBatchId(),
                    request.getToAddress(),
                    "Dược Hậu Giang", // Correct manufacturer name
                    toLocationName,
                    BigInteger.valueOf(request.getQuantity()),
                    trackingInfo,  // Pass tracking info to blockchain
                    String.format("Lo: %s - %s", batch.getBatchNumber(), request.getNotes() != null ? request.getNotes() : "Xuat kho nha may") // Optimized notes with Batch Number
                ).get();
                blockchainSuccess = true;
                log.info("Shipment created on blockchain successfully with tracking: {}", trackingInfo);
                
                // ✅ CRITICAL FIX: Extract the REAL shipmentId assigned by the blockchain counter
                BigInteger realShipmentId = blockchainService.extractShipmentId(receipt).orElse(shipmentId);
                if (!realShipmentId.equals(shipmentId)) {
                    log.info("🎯 EXTRACTED REAL blockchain shipmentId: {}", realShipmentId);
                    shipmentId = realShipmentId;
                }
            } catch (Exception e) {
                log.error("Failed to create shipment on blockchain, proceeding with local save", e);
                blockchainSuccess = false;
            }

            // Create local entity using adapter
            log.info("Creating shipment entity using adapter...");
            Shipment shipment;
            if (blockchainSuccess && receipt != null) {
                log.info("Creating shipment with blockchain data");
                shipment = shipmentAdapter.createShipmentFromBlockchain(
                    shipmentId,
                    fromAddress,
                    request.getToAddress(),
                    request.getQuantity(),
                    request.getTrackingInfo(),
                    receipt.getTransactionHash(),
                    receipt.getBlockNumber()
                );
            } else {
                // Fallback: create shipment without blockchain data
                log.info("Creating shipment without blockchain data (fallback mode)");
                shipment = shipmentAdapter.createShipmentFromBlockchain(
                    shipmentId,
                    fromAddress,
                    request.getToAddress(),
                    request.getQuantity(),
                    request.getTrackingInfo(),
                    "PENDING_" + System.currentTimeMillis(),
                    BigInteger.ZERO
                );
            }
            log.info("Shipment entity created successfully");
            
            shipment.setStatus(Shipment.ShipmentStatus.PENDING);
            shipment.setDrugBatch(batch);

            shipment = shipmentRepository.save(shipment);

            // Update batch status
            batch.setStatus(com.nckh.dia5.model.DrugBatch.BatchStatus.IN_TRANSIT);
            drugBatchRepository.save(batch);

            // Record blockchain transaction if successful
            if (blockchainSuccess && receipt != null) {
                recordBlockchainTransaction(receipt, "createShipment", batch, shipment);
            }

            log.info("Shipment created successfully: id={}, shipmentId={}, blockchain={}", 
                     shipment.getId(), shipmentId, blockchainSuccess);
            return mapToShipmentDto(shipment);

        } catch (ResourceNotFoundException | IllegalStateException e) {
            log.error("Validation error creating shipment: {}", e.getMessage());
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating shipment, but returning local saved entity: {}", e.getMessage());
            // Try to persist shipment without triggering problematic triggers by deferring ownership_history
            // Already saved above; return last saved shipment
            List<Shipment> latest = shipmentRepository.findAll();
            Shipment shipment = latest.isEmpty() ? null : latest.get(latest.size() - 1);
            if (shipment == null) {
                throw new RuntimeException("Không thể tạo shipment: dữ liệu không hợp lệ", e);
            }
            return mapToShipmentDto(shipment);
        } catch (Exception e) {
            log.error("Failed to create shipment", e);
            throw new RuntimeException("Không thể tạo shipment: " + e.getMessage(), e);
        }
    }

    /**
     * Receive a shipment
     */
    // Removed @Transactional to avoid blocking DB transaction while waiting for blockchain
    // This also ensures shipment is committed before async blockchain task tries to link to it
    public ShipmentDto receiveShipment(BigInteger shipmentId) {
        try {
            log.info("Attempting to receive shipment with ID: {}", shipmentId);

            // Try multiple lookup strategies to find the shipment
            Shipment shipment = null;
            
            // Strategy 1: Look by shipment code (SHIP-{id})
            Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentId(shipmentId);
            if (shipmentOpt.isPresent()) {
                shipment = shipmentOpt.get();
                log.info("Found shipment by shipmentId: {}", shipment.getShipmentCode());
            } else {
                // Strategy 2: Look by blockchain_id in notes JSON field
                log.info("Shipment not found by shipmentId, trying blockchain_id lookup...");
                shipmentOpt = shipmentRepository.findByBlockchainId(shipmentId.toString());
                if (shipmentOpt.isPresent()) {
                    shipment = shipmentOpt.get();
                    log.info("Found shipment by blockchain_id in notes: {}", shipment.getShipmentCode());
                } else {
                    // Strategy 3: Look by database ID as fallback
                    try {
                        shipment = shipmentRepository.findById(shipmentId.longValue()).orElse(null);
                        if (shipment != null) {
                            log.info("Found shipment by database ID: {}", shipment.getShipmentCode());
                        }
                    } catch (Exception e) {
                        log.debug("Failed to find by database ID: {}", e.getMessage());
                    }
                }
            }

            if (shipment == null) {
                throw new ResourceNotFoundException("Shipment", "shipmentId", shipmentId.toString());
            }

            // Get receiver address from shipment (not default manufacturer)
            String receiverAddress = shipment.getToAddress();
            if (receiverAddress == null) {
                // Fallback: extract from notes if available
                if (shipment.getNotes() != null && shipment.getNotes().contains("to_address")) {
                    try {
                        // Simple JSON parsing to extract to_address
                        String notes = shipment.getNotes();
                        int toAddressIndex = notes.indexOf("\"to_address\":");
                        if (toAddressIndex != -1) {
                            int startQuote = notes.indexOf("\"", toAddressIndex + 13);
                            int endQuote = notes.indexOf("\"", startQuote + 1);
                            if (startQuote != -1 && endQuote != -1) {
                                receiverAddress = notes.substring(startQuote + 1, endQuote);
                                log.info("Extracted receiver address from notes: {}", receiverAddress);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract receiver address from notes: {}", e.getMessage());
                    }
                }
                
                if (receiverAddress == null) {
                    receiverAddress = shipment.getToCompany() != null ? shipment.getToCompany().getWalletAddress() : null;
                }
            }

            if (receiverAddress == null) {
                throw new IllegalStateException("Không thể xác định địa chỉ người nhận");
            }

            log.info("Receiving shipment: shipmentCode={}, receiver={}", shipment.getShipmentCode(), receiverAddress);

            // Determine receiver location name
            String receiverLocationName = "Unknown Location";
            if (shipment.getToCompany() != null) {
                receiverLocationName = shipment.getToCompany().getName();
            } else {
                // Try to find company by wallet address
                Optional<PharmaCompany> receiverCompany = pharmaCompanyRepository.findByWalletAddress(receiverAddress);
                if (receiverCompany.isPresent()) {
                    receiverLocationName = receiverCompany.get().getName();
                } else {
                    receiverLocationName = "Dia diem " + receiverAddress.substring(0, 8);
                }
            }

            // ✅ CRITICAL: Receive shipment on blockchain using receiveShipmentWithLink
            // This guarantees a UNIQUE new TX hash AND saves Admin Log with shipment linkage atomically.
            // Service credentials are used (not receiver wallet key lookup which fails on Sepolia).
            // First update shipment status so the link in Admin Log is meaningful
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
            shipment.setActualDeliveryDate(LocalDateTime.now());
            shipment = shipmentRepository.save(shipment); // save first to get a persisted shipment for FK

            final Shipment savedShipmentForLink = shipment;
            TransactionReceipt receipt = blockchainService.receiveShipmentWithLink(
                shipmentId,
                "Da nhan tai: " + receiverLocationName,
                savedShipmentForLink,
                receiverAddress
            ).get();
            
            // ✅ Persist the NEW receive TX hash (distinct from createTxHash)
            shipment.setReceiveTxHash(receipt.getTransactionHash());
            shipment = shipmentRepository.save(shipment);
            
            log.info("✅ receiveShipment: new TX hash={} (Block={}) saved to shipment.receiveTxHash", 
                     receipt.getTransactionHash(), receipt.getBlockNumber());
            // recordBlockchainTransaction() is intentionally NOT called here -
            // BlockchainService.receiveShipmentWithLink already saved to Admin Log with shipment linkage.

            // Update batch owner and status
            com.nckh.dia5.model.DrugBatch batch = shipment.getDrugBatch();
            if (batch != null) {
                batch.setCurrentOwner(receiverAddress);
                batch.setStatus(com.nckh.dia5.model.DrugBatch.BatchStatus.DELIVERED);
                drugBatchRepository.save(batch);
            }

            // ✅ CRITICAL: Update pharmacy/distributor inventory
            try {
                PharmaCompany receiver = shipment.getToCompany();
                if (receiver != null && batch != null) {
                    if (receiver.getCompanyType() == PharmaCompany.CompanyType.PHARMACY) {
                        // Add to pharmacy_inventory table
                        pharmacyInventoryService.receiveShipment(
                            receiver.getId(),
                            batch.getId(),
                            shipment.getQuantity(),
                            shipment
                        );
                        log.info("✅ Added to pharmacy_inventory: pharmacy={}, batch={}, quantity={}", 
                                receiver.getName(), batch.getBatchNumber(), shipment.getQuantity());
                    } else if (receiver.getCompanyType() == PharmaCompany.CompanyType.DISTRIBUTOR) {
                        // Add to distributor_inventory table
                        distributorInventoryService.receiveShipment(
                            receiver.getId(),
                            batch.getId(),
                            shipment.getQuantity(),
                            shipment
                        );
                        log.info("✅ Added to distributor_inventory: distributor={}, batch={}, quantity={}", 
                                receiver.getName(), batch.getBatchNumber(), shipment.getQuantity());
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to update inventory after receiving shipment", e);
                // Don't throw - shipment is already marked as DELIVERED
            }

            log.info("Shipment received successfully: shipmentCode={}", shipment.getShipmentCode());
            return mapToShipmentDto(shipment);

        } catch (Exception e) {
            log.error("Failed to receive shipment", e);
            throw new RuntimeException("Không thể nhận shipment: " + e.getMessage(), e);
        }
    }

    /**
     * Receive a shipment by database ID (fallback method)
     */
    // Removed @Transactional to avoid blocking DB transaction
    public ShipmentDto receiveShipmentByDatabaseId(BigInteger databaseId) {
        try {
            log.info("Attempting to receive shipment by database ID: {}", databaseId);

            // Find the shipment by database ID first
            Shipment shipment = shipmentRepository.findById(databaseId.longValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", databaseId.toString()));

            // Get the receiver address from the shipment (distributor address)
            String receiverAddress = shipment.getToAddress();
            if (receiverAddress == null) {
                // Fallback: extract from notes if available
                if (shipment.getNotes() != null && shipment.getNotes().contains("to_address")) {
                    try {
                        // Simple JSON parsing to extract to_address
                        String notes = shipment.getNotes();
                        int toAddressIndex = notes.indexOf("\"to_address\":");
                        if (toAddressIndex != -1) {
                            int startQuote = notes.indexOf("\"", toAddressIndex + 13);
                            int endQuote = notes.indexOf("\"", startQuote + 1);
                            if (startQuote != -1 && endQuote != -1) {
                                receiverAddress = notes.substring(startQuote + 1, endQuote);
                                log.info("Extracted receiver address from notes: {}", receiverAddress);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract receiver address from notes: {}", e.getMessage());
                    }
                }
                
                if (receiverAddress == null) {
                    receiverAddress = shipment.getToCompany() != null ? shipment.getToCompany().getWalletAddress() : null;
                }
            }
            
            log.info("Receiving shipment by database ID: databaseId={}, shipmentCode={}, receiver={}", 
                     databaseId, shipment.getShipmentCode(), receiverAddress);

            // ✅ FIX: Also call blockchain receiveShipment for fallback path (generates unique TX hash)
            String receiverLocationName = "Unknown Location";
            if (shipment.getToCompany() != null) {
                receiverLocationName = shipment.getToCompany().getName();
            } else if (receiverAddress != null && receiverAddress.length() >= 8) {
                receiverLocationName = "Dia diem " + receiverAddress.substring(0, 8);
            }

            // Get blockchain shipment ID from notes/code
            BigInteger blockchainShipmentId = databaseId; // fallback
            try {
                String code = shipment.getShipmentCode();
                if (code != null && code.startsWith("SHIP-")) {
                    blockchainShipmentId = new BigInteger(code.substring(5));
                }
            } catch (Exception ex) {
                log.warn("Could not extract blockchain shipmentId from code, using databaseId: {}", databaseId);
            }

            // Save shipment status first so the Admin Log FK is valid
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
            shipment.setActualDeliveryDate(LocalDateTime.now());
            shipment = shipmentRepository.save(shipment);

            // Call blockchain and link Admin Log
            try {
                final Shipment savedShipmentForLink = shipment;
                TransactionReceipt receipt = blockchainService.receiveShipmentWithLink(
                    blockchainShipmentId,
                    "Da nhan tai: " + receiverLocationName,
                    savedShipmentForLink,
                    receiverAddress
                ).get();
                shipment.setReceiveTxHash(receipt.getTransactionHash());
                shipment = shipmentRepository.save(shipment);
                log.info("✅ receiveShipmentByDatabaseId: new TX hash={} (Block={})", 
                         receipt.getTransactionHash(), receipt.getBlockNumber());
            } catch (Exception blockchainEx) {
                log.warn("⚠️ Blockchain call failed in receiveShipmentByDatabaseId, status already set to DELIVERED: {}", 
                         blockchainEx.getMessage());
                // Do not rethrow — shipment is already DELIVERED in DB
            }

            // Update batch owner and status
            com.nckh.dia5.model.DrugBatch batch = shipment.getDrugBatch();
            if (batch != null && receiverAddress != null) {
                batch.setCurrentOwner(receiverAddress);
                batch.setStatus(com.nckh.dia5.model.DrugBatch.BatchStatus.DELIVERED);
                drugBatchRepository.save(batch);
            }

            // ✅ CRITICAL: Update pharmacy/distributor inventory (same as receiveShipment)
            try {
                PharmaCompany receiver = shipment.getToCompany();
                if (receiver != null && batch != null) {
                    if (receiver.getCompanyType() == PharmaCompany.CompanyType.PHARMACY) {
                        // Add to pharmacy_inventory table
                        pharmacyInventoryService.receiveShipment(
                            receiver.getId(),
                            batch.getId(),
                            shipment.getQuantity(),
                            shipment
                        );
                        log.info("✅ Added to pharmacy_inventory: pharmacy={}, batch={}, quantity={}", 
                                receiver.getName(), batch.getBatchNumber(), shipment.getQuantity());
                    } else if (receiver.getCompanyType() == PharmaCompany.CompanyType.DISTRIBUTOR) {
                        // Add to distributor_inventory table
                        distributorInventoryService.receiveShipment(
                            receiver.getId(),
                            batch.getId(),
                            shipment.getQuantity(),
                            shipment
                        );
                        log.info("✅ Added to distributor_inventory: distributor={}, batch={}, quantity={}", 
                                receiver.getName(), batch.getBatchNumber(), shipment.getQuantity());
                    }
                } else {
                    log.warn("⚠️ Cannot update inventory: receiver={}, batch={}", receiver, batch);
                }
            } catch (Exception e) {
                log.error("❌ Failed to update inventory after receiving shipment by database ID", e);
                // Don't throw - shipment is already marked as DELIVERED
            }

            log.info("Shipment received successfully by database ID: {}", shipment.getShipmentCode());
            return mapToShipmentDto(shipment);

        } catch (Exception e) {
            log.error("Failed to receive shipment by database ID", e);
            throw new RuntimeException("Không thể nhận shipment: " + e.getMessage(), e);
        }
    }

    /**
     * Update shipment status
     */
    @Transactional
    public ShipmentDto updateShipmentStatus(UpdateShipmentStatusRequest request) {
        try {
            Shipment shipment = shipmentRepository.findByShipmentId(request.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", request.getShipmentId().toString()));

            // Update status
            Shipment.ShipmentStatus newStatus = Shipment.ShipmentStatus.valueOf(request.getNewStatus().toUpperCase());
            shipment.setStatus(newStatus);
            
            if (request.getTrackingInfo() != null) {
                shipment.setTrackingInfo(request.getTrackingInfo());
            }

            shipment = shipmentRepository.save(shipment);

            log.info("Shipment status updated: shipmentId={}, newStatus={}", request.getShipmentId(), newStatus);
            return mapToShipmentDto(shipment);

        } catch (Exception e) {
            log.error("Failed to update shipment status", e);
            throw new RuntimeException("Không thể cập nhật trạng thái shipment: " + e.getMessage(), e);
        }
    }

    /**
     * Verify drug authenticity by QR code
     */
    public DrugBatchDto verifyDrug(VerifyDrugRequest request) {
        try {
            com.nckh.dia5.model.DrugBatch batch = drugBatchRepository.findByQrCode(request.getQrCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Drug batch", "qrCode", request.getQrCode()));

            // Verify on blockchain
            boolean isValid = blockchainService.verifyOwnership(batch.getBatchId(), batch.getCurrentOwner()).get();
            
            if (!isValid) {
                throw new IllegalStateException("Thuốc không hợp lệ hoặc đã bị giả mạo");
            }

            log.info("Drug verified successfully: batchId={}, qrCode={}", batch.getBatchId(), request.getQrCode());
            return mapToDrugBatchDto(batch);

        } catch (Exception e) {
            log.error("Failed to verify drug", e);
            throw new RuntimeException("Không thể xác minh thuốc: " + e.getMessage(), e);
        }
    }

    /**
     * Get all batches
     */
    public List<DrugBatchDto> getAllBatches() {
        List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findAll();
        return batches.stream().map(this::mapToDrugBatchDto).collect(Collectors.toList());
    }

    /**
     * Get batch by ID
     */
    public DrugBatchDto getBatch(BigInteger batchId) {
        com.nckh.dia5.model.DrugBatch batch = drugBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", batchId.toString()));
        return mapToDrugBatchDto(batch);
    }

    /**
     * Get batches by manufacturer
     */
    public List<DrugBatchDto> getBatchesByManufacturer(String manufacturerAddress) {
        List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findByManufacturerAddress(manufacturerAddress);
        return batches.stream().map(this::mapToDrugBatchDto).collect(Collectors.toList());
    }

    /**
     * Get batches by current owner
     */
    public List<DrugBatchDto> getBatchesByOwner(String ownerAddress) {
        List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findByCurrentOwner(ownerAddress);
        return batches.stream().map(this::mapToDrugBatchDto).collect(Collectors.toList());
    }

    /**
     * Get shipment by ID
     */
    public ShipmentDto getShipment(BigInteger shipmentId) {
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId.toString()));
        return mapToShipmentDto(shipment);
    }

    /**
     * Get shipments by batch (supports both batchId and batchNumber)
     */
    public List<ShipmentDto> getShipmentsByBatch(BigInteger batchId) {
        try {
            log.info("Getting shipments for batch: {}", batchId);
            
            com.nckh.dia5.model.DrugBatch batch = drugBatchRepository.findByBatchId(batchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found with batchId: '" + batchId + "'"));
            
            List<Shipment> shipments = shipmentRepository.findByDrugBatch(batch);
            log.info("Found {} shipments for batch {}", shipments.size(), batchId);
            
            return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            log.error("Batch not found: {}", batchId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting shipments for batch {}: {}", batchId, e.getMessage(), e);
            throw new RuntimeException("Failed to get shipments for batch: " + e.getMessage(), e);
        }
    }

    /**
     * Get shipments by batch number (Số lô) - VD: BT202512121857
     * This method searches by the human-readable batch number that stays consistent from manufacturer to pharmacy
     */
    public List<ShipmentDto> getShipmentsByBatchNumber(String batchNumber) {
        try {
            log.info("Getting shipments for batch number (Số lô): {}", batchNumber);
            
            // First try exact match
            Optional<com.nckh.dia5.model.DrugBatch> batchOpt = drugBatchRepository.findByBatchNumber(batchNumber);
            
            if (batchOpt.isEmpty()) {
                // Try containing match (for partial batch numbers)
                List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findByBatchNumberContaining(batchNumber);
                if (!batches.isEmpty()) {
                    batchOpt = Optional.of(batches.get(0));
                    log.info("Found batch by partial match: {}", batches.get(0).getBatchNumber());
                }
            }
            
            if (batchOpt.isEmpty()) {
                throw new ResourceNotFoundException("Batch not found with batchNumber (Số lô): '" + batchNumber + "'");
            }
            
            com.nckh.dia5.model.DrugBatch batch = batchOpt.get();
            List<Shipment> shipments = shipmentRepository.findByDrugBatch(batch);
            log.info("Found {} shipments for batch number {}", shipments.size(), batchNumber);
            
            return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            log.error("Batch not found by number: {}", batchNumber);
            throw e;
        } catch (Exception e) {
            log.error("Error getting shipments for batch number {}: {}", batchNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to get shipments for batch number: " + e.getMessage(), e);
        }
    }

    /**
     * Smart search for shipments - tries batchNumber first, then batchId
     * This is the preferred method for scanning/receiving goods
     */
    public List<ShipmentDto> searchShipmentsByBatchIdentifier(String identifier) {
        try {
            log.info("Smart search for shipments with identifier: {}", identifier);
            
            // Strategy 1: Try to find by batch number (Số lô) - e.g., BT202512121857
            if (identifier.startsWith("BT") || !identifier.matches("\\d+")) {
                try {
                    return getShipmentsByBatchNumber(identifier);
                } catch (ResourceNotFoundException e) {
                    log.info("Batch number not found, trying batchId...");
                }
            }
            
            // Strategy 2: Try to find by batchId (blockchain ID) - e.g., 17655406385509934
            try {
                BigInteger batchId = new BigInteger(identifier);
                return getShipmentsByBatch(batchId);
            } catch (NumberFormatException e) {
                log.warn("Identifier is not a valid number, cannot search by batchId");
            }
            
            throw new ResourceNotFoundException("Không tìm thấy lô hàng với mã: " + identifier + ". Vui lòng kiểm tra lại Số lô (VD: BT202512121857) hoặc Batch ID.");
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in smart search for identifier {}: {}", identifier, e.getMessage(), e);
            throw new RuntimeException("Lỗi tìm kiếm lô hàng: " + e.getMessage(), e);
        }
    }

    /**
     * Get transaction history for a batch
     */
    public List<BlockchainTransactionDto> getBatchTransactionHistory(BigInteger batchId) {
        List<BlockchainTransaction> transactions = blockchainTransactionRepository.findByBatchIdOrderByTimestamp(batchId);
        return transactions.stream().map(this::mapToBlockchainTransactionDto).collect(Collectors.toList());
    }

    @Transactional
    public DrugBatchDto updateBatchStatus(Long id, com.nckh.dia5.model.DrugBatch.BatchStatus status, String reason) {
        com.nckh.dia5.model.DrugBatch batch = drugBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô thuốc với Database ID: " + id));
        
        log.info("Updating batch {} status to {}", batch.getBatchNumber(), status);
        batch.setStatus(status);
        
        // Cập nhật thông tin bổ sung nếu cần (ví dụ: ghi vào notes)
        if (reason != null && !reason.isEmpty()) {
            batch.setStorageConditions(batch.getStorageConditions() + " | THU HỒI: " + reason);
        }
        
        batch = drugBatchRepository.save(batch);

        // ✅ MỚI: Lan tỏa trạng thái thu hồi xuống từng sản phẩm con (Items) thông qua Service chính thống
        if (status == com.nckh.dia5.model.DrugBatch.BatchStatus.RECALLED) {
            try {
                productItemService.recallBatchItems(batch);
                
                // ✅ MỚI: Tìm và hủy các vận đơn (Shipments) liên quan đang chờ hoặc đang giao
                List<com.nckh.dia5.model.Shipment> activeShipments = shipmentRepository.findByDrugBatch(batch);
                for (com.nckh.dia5.model.Shipment shipment : activeShipments) {
                    if (shipment.getStatus() == com.nckh.dia5.model.Shipment.ShipmentStatus.PENDING || 
                        shipment.getStatus() == com.nckh.dia5.model.Shipment.ShipmentStatus.IN_TRANSIT) {
                        
                        shipment.setStatus(com.nckh.dia5.model.Shipment.ShipmentStatus.CANCELLED);
                        shipment.setNotes(shipment.getNotes() + "\n[AUTO-CANCEL] Lô hàng bị thu hồi bởi NSX.");
                        shipmentRepository.save(shipment);
                        log.info("Automatically cancelled shipment {} due to batch recall", shipment.getShipmentCode());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to cascade recall status to items/shipments for batch {}", batch.getBatchNumber(), e);
            }
        }

        // Ghi lên Blockchain nếu là lệnh thu hồi (Chờ kết quả để lấy TxHash)
        if (status == com.nckh.dia5.model.DrugBatch.BatchStatus.RECALLED) {
            final String batchNum = batch.getBatchNumber();
            final BigInteger bId = batch.getBatchId();
            final com.nckh.dia5.model.DrugBatch finalBatch = batch;
            try {
                // Đợi giao dịch hoàn tất để lấy TxHash trả về cho UI
                org.web3j.protocol.core.methods.response.TransactionReceipt receipt = blockchainService.recallBatch(bId, finalBatch).get(); 
                log.info("Successfully recalled batch {} on blockchain. TxHash: {}", 
                         batchNum, receipt.getTransactionHash());
                
                // Cập nhật TxHash vào DB cho lô thuốc
                batch.setTransactionHash(receipt.getTransactionHash());
                batch = drugBatchRepository.save(batch);
            } catch (Exception e) {
                log.error("Failed to trigger blockchain recall for batch {}", batchNum, e);
            }
        }
        
        return mapToDrugBatchDto(batch);
    }

    /**
     * Get batches ready for shipment (MANUFACTURED status and owned by manufacturer)
     */
    public List<DrugBatchDto> getBatchesReadyForShipment() {
        String manufacturerAddress = getDefaultManufacturerAddress();
        List<com.nckh.dia5.model.DrugBatch> batches = drugBatchRepository.findByCurrentOwnerAndStatus(
            manufacturerAddress, com.nckh.dia5.model.DrugBatch.BatchStatus.MANUFACTURED);
        return batches.stream().map(this::mapToDrugBatchDto).collect(Collectors.toList());
    }

    /**
     * Get available distributors from pharma_companies table
     */
    public List<DistributorDto> getDistributors() {
        List<PharmaCompany> distributors = pharmaCompanyRepository.findByCompanyTypeAndIsActive(
            PharmaCompany.CompanyType.DISTRIBUTOR, true);
        return distributors.stream().map(this::mapPharmaCompanyToDistributorDto).collect(Collectors.toList());
    }

    /**
     * Get all shipments
     */
    public List<ShipmentDto> getAllShipments() {
        List<Shipment> shipments = shipmentRepository.findAll();
        return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
    }

    /**
     * Get shipments by manufacturer address
     */
    public List<ShipmentDto> getShipmentsByManufacturer(String manufacturerAddress) {
        List<Shipment> shipments = shipmentRepository.findByFromAddress(manufacturerAddress);
        return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
    }

    /**
     * Get pending shipments (IN_TRANSIT status)
     */
    public List<ShipmentDto> getPendingShipments() {
        List<Shipment> shipments = shipmentRepository.findByStatus(Shipment.ShipmentStatus.IN_TRANSIT);
        return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
    }

    /**
     * Get shipments by recipient address
     */
    public List<ShipmentDto> getShipmentsByRecipient(String recipientAddress) {
        List<Shipment> shipments = shipmentRepository.findByToAddress(recipientAddress);
        return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
    }

    /**
     * Get shipments by sender address (for distributor export management)
     */
    public List<ShipmentDto> getShipmentsBySender(String senderAddress) {
        List<Shipment> shipments = shipmentRepository.findByFromAddress(senderAddress);
        return shipments.stream().map(this::mapToShipmentDto).collect(Collectors.toList());
    }

    // Helper methods
    private BigInteger generateBatchId() {
        // Generate a shorter batch ID that fits in NUMERIC(38,0)
        // Use current timestamp + random number
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return BigInteger.valueOf(timestamp * 10000L + random);
    }

    private BigInteger generateShipmentId() {
        // Generate a smaller shipment ID that fits in database
        // Use current timestamp + random number (similar to batchId)
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return BigInteger.valueOf(timestamp * 10000L + random);
    }

    private String generateQrCode(BigInteger batchId, String batchNumber) {
        return String.format("NCKH-PHARMA-%s-%s", batchId.toString(16).toUpperCase(), batchNumber);
    }

    private String getManufacturerAddress(User user) {
        // In a real implementation, this would map user to their blockchain address
        // For now, we'll use a placeholder based on user ID
        return "0x" + user.getId().replace("-", "").substring(0, 40);
    }
    
    private String getDefaultManufacturerAddress() {
        // Default manufacturer address for testing
        // This should be replaced with proper user authentication in production
        return "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // First Hardhat account
    }


    private void recordBlockchainTransaction(TransactionReceipt receipt, String functionName, 
                                           com.nckh.dia5.model.DrugBatch batch, Shipment shipment) {
        try {
            String txHash = receipt.getTransactionHash();
            
            // ✅ IDEMPOTENT: Check if record already exists (BlockchainService.executeTransactionInternal may have saved it)
            if (blockchainTransactionRepository.existsByTransactionHash(txHash)) {
                Optional<BlockchainTransaction> existing = blockchainTransactionRepository.findByTransactionHash(txHash);
                // Record exists - update it with Shipment/Batch linkage if missing
                BlockchainTransaction tx = existing.get();
                boolean updated = false;
                if (tx.getShipment() == null && shipment != null) {
                    tx.setShipment(shipment);
                    updated = true;
                }
                if (tx.getDrugBatch() == null && batch != null) {
                    tx.setDrugBatch(batch);
                    updated = true;
                }
                // Also update functionName if it was saved generically
                if (!functionName.equals(tx.getFunctionName())) {
                    tx.setFunctionName(functionName);
                    updated = true;
                }
                if (updated) {
                    blockchainTransactionRepository.save(tx);
                    log.info("✅ Updated existing blockchain_transaction record for txHash: {} (function={})", txHash, functionName);
                } else {
                    log.debug("ℹ️ Blockchain transaction already recorded: {} ({})", txHash, functionName);
                }
                return;
            }
            
            // New record - save
            BlockchainTransaction transaction = new BlockchainTransaction();
            transaction.setTransactionHash(txHash);
            // Set block number directly from receipt (BigInteger)
            BigInteger txBlock = receipt.getBlockNumber();
            transaction.setBlockNumber(txBlock != null ? txBlock : BigInteger.ONE);
            transaction.setFromAddress(receipt.getFrom());
            transaction.setToAddress(receipt.getTo());
            transaction.setFunctionName(functionName);
            // Gas used is already BigInteger
            transaction.setGasUsed(receipt.getGasUsed());
            transaction.setStatus("0x1".equals(receipt.getStatus()) ? 
                                 BlockchainTransaction.TransactionStatus.SUCCESS : 
                                 BlockchainTransaction.TransactionStatus.FAILED);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setDrugBatch(batch);
            transaction.setShipment(shipment);

            blockchainTransactionRepository.save(transaction);
            log.info("💾 Saved new blockchain_transaction: {} ({})", txHash, functionName);
        } catch (Exception e) {
            log.error("Failed to record blockchain transaction", e);
        }
    }

    // Mapping methods
    private DrugBatchDto mapToDrugBatchDto(com.nckh.dia5.model.DrugBatch batch) {
        return DrugBatchDto.builder()
                .id(batch.getId())
                .batchId(batch.getBatchId())
                .drugName(batch.getDrugName())
                .manufacturer(batch.getManufacturer())
                .batchNumber(batch.getBatchNumber())
                .quantity(batch.getQuantity())
                .manufacturerAddress(batch.getManufacturerAddress())
                .currentOwner(batch.getCurrentOwner())
                .manufactureTimestamp(batch.getManufactureTimestamp())
                .expiryDate(batch.getExpiryDate())
                .storageConditions(batch.getStorageConditions())
                .status(batch.getStatus().name())
                .qrCode(batch.getQrCode())
                .transactionHash(batch.getTransactionHash())
                .blockNumber(batch.getBlockNumber())
                .isSynced(batch.getIsSynced())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    private ShipmentDto mapToShipmentDto(Shipment shipment) {
        // Extract blockchain data from notes
        Map<String, Object> blockchainData = shipmentAdapter.extractBlockchainData(shipment);
        
        return ShipmentDto.builder()
                .id(shipment.getId())
                .shipmentCode(shipment.getShipmentCode())
                .shipmentId((BigInteger) blockchainData.getOrDefault("shipmentId", BigInteger.ZERO))
                .fromAddress(shipment.getFromCompany() != null ? 
                    shipment.getFromCompany().getWalletAddress() : 
                    (String) blockchainData.get("fromAddress"))
                .toAddress(shipment.getToCompany() != null ? 
                    shipment.getToCompany().getWalletAddress() : 
                    (String) blockchainData.get("toAddress"))
                .quantity(shipment.getQuantity() != null ? shipment.getQuantity().longValue() : 0L)
                .shipmentTimestamp(shipment.getShipmentDate())
                .status(shipment.getStatus().name())
                .trackingInfo((String) blockchainData.getOrDefault("trackingInfo", ""))
                .transactionHash(shipment.getCreateTxHash())
                .receiveTransactionHash(shipment.getReceiveTxHash())
                .blockNumber((BigInteger) blockchainData.get("blockNumber"))
                .isSynced((Boolean) blockchainData.getOrDefault("isSynced", false))
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .drugBatch(shipment.getDrugBatch() != null ? mapToDrugBatchDto(shipment.getDrugBatch()) : null)
                .build();
    }

    private BlockchainTransactionDto mapToBlockchainTransactionDto(BlockchainTransaction transaction) {
        return BlockchainTransactionDto.builder()
                .id(transaction.getId())
                .transactionHash(transaction.getTransactionHash())
                .blockNumber(transaction.getBlockNumber())
                .fromAddress(transaction.getFromAddress())
                .toAddress(transaction.getToAddress())
                .functionName(transaction.getFunctionName())
                .gasUsed(transaction.getGasUsed())
                .gasPrice(transaction.getGasPrice())
                .status(transaction.getStatus().name())
                .inputData(transaction.getInputData())
                .eventLogs(transaction.getEventLogs())
                .errorMessage(transaction.getErrorMessage())
                .timestamp(transaction.getTimestamp())
                .createdAt(transaction.getCreatedAt())
                .drugBatchId(transaction.getDrugBatch() != null ? transaction.getDrugBatch().getId() : null)
                .shipmentId(transaction.getShipment() != null ? transaction.getShipment().getId() : null)
                .build();
    }

    private DistributorDto mapPharmaCompanyToDistributorDto(PharmaCompany pharmaCompany) {
        return DistributorDto.builder()
                .id(pharmaCompany.getId())
                .name(pharmaCompany.getName())
                .address(pharmaCompany.getAddress())
                .phone(pharmaCompany.getPhone())
                .email(pharmaCompany.getEmail())
                .walletAddress(pharmaCompany.getWalletAddress())
                .licenseNumber(pharmaCompany.getLicenseNumber())
                .website(null) // PharmaCompany không có website field
                .contactPerson(pharmaCompany.getContactPerson())
                .status(pharmaCompany.getIsActive() ? "ACTIVE" : "INACTIVE")
                .build();
    }
}
