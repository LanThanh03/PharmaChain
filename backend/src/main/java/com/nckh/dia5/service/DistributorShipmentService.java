package com.nckh.dia5.service;

import com.nckh.dia5.dto.blockchain.CreateDistributorShipmentRequest;
import com.nckh.dia5.dto.blockchain.ShipmentDto;
import com.nckh.dia5.dto.blockchain.DrugBatchDto;
import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.model.DistributorInventory;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.PharmaCompany;
import com.nckh.dia5.model.Shipment;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.nckh.dia5.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributorShipmentService {

    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final ShipmentRepository shipmentRepository;
    private final BlockchainService blockchainService;
    private final ShipmentAdapter shipmentAdapter;
    private final DistributorInventoryService distributorInventoryService;

    @Transactional
    public ShipmentDto createShipmentToPharmacy(CreateDistributorShipmentRequest request) {
        try {
            log.info("Starting distributor shipment creation...");
            
            // 1. Find pharmacy by ID and get wallet address
            PharmaCompany pharmacy = pharmaCompanyRepository.findById(request.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId().toString()));
            
            if (pharmacy.getCompanyType() != PharmaCompany.CompanyType.PHARMACY) {
                throw new IllegalArgumentException("Company với ID " + request.getPharmacyId() + " không phải là hiệu thuốc");
            }
            
            if (pharmacy.getWalletAddress() == null || pharmacy.getWalletAddress().trim().isEmpty()) {
                throw new IllegalStateException("Hiệu thuốc chưa có địa chỉ ví blockchain");
            }
            
            log.info("Found pharmacy: {} with wallet address: {}", pharmacy.getName(), pharmacy.getWalletAddress());
            
            // 2. Find batch - CRITICAL: Use blockchain batch ID
            log.info("🔍 Looking for batch with blockchain batch_id: {}", request.getBatchId());
            DrugBatch batch = drugBatchRepository.findByBatchId(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", request.getBatchId().toString()));
            
            // ⭐ LOG COMPLETE BATCH INFO
            log.info("✅ Found batch - Database ID: {}, Blockchain Batch ID: {}, Batch Number: {}, Quantity: {}", 
                    batch.getId(), 
                    batch.getBatchId(), 
                    batch.getBatchNumber(), 
                    batch.getQuantity());
            
            // ⚠️ VERIFY: Blockchain batch ID matches request
            if (!batch.getBatchId().equals(request.getBatchId())) {
                log.error("❌ CRITICAL: Batch ID mismatch! Request: {}, Found: {}", 
                        request.getBatchId(), batch.getBatchId());
                throw new IllegalStateException(
                    String.format("Batch ID không khớp! Yêu cầu: %s, Tìm thấy: %s", 
                        request.getBatchId(), batch.getBatchId()));
            }
            
            // 3. Verify quantity - check distributor inventory instead of batch
            // Note: We'll verify actual available quantity when calling distributorInventoryService.shipOut()
            
            // 4. Get distributor address (current owner)
            String distributorAddress = batch.getCurrentOwner();
            log.info("Distributor address (current owner): {}", distributorAddress);
            
            // 5. Get distributor info
            PharmaCompany distributor = pharmaCompanyRepository.findByWalletAddress(distributorAddress)
                    .orElseThrow(() -> new ResourceNotFoundException("Distributor", "walletAddress", distributorAddress));
            
            // 6. Generate meaningful tracking number if not provided
            String trackingNumber = request.getTrackingNumber();
            if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                // Format: NPP[DistributorName] gui [PharmacyName] - [DrugName] SL:[quantity] - Lo:[batchNumber]
                // Example: "NPP ABC gui Hieu Thuoc XYZ - Paracetamol SL:100 - Lo:BATCH123"
                String distName = distributor.getName();
                if (distName.contains("XYZ") || distName.contains("Distributor")) distName = "CPC1 Hà Nội";
                
                String pharmName = pharmacy.getName();
                if (pharmName.contains("An Khang") || pharmName.contains("Pharmacy")) pharmName = "Long Châu";

                String cleanDistName = removeVietnameseDiacritics(distName).replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "");
                String cleanPharmName = removeVietnameseDiacritics(pharmName).replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "");
                String drugName = removeVietnameseDiacritics(batch.getDrugName()).replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "");
                String batchNum = batch.getBatchNumber().replaceAll("[^a-zA-Z0-9]", "");
                
                trackingNumber = String.format("NPP %s gui %s - %s SL:%d - Lo:%s",
                        cleanDistName.length() > 15 ? cleanDistName.substring(0, 15) : cleanDistName,
                        cleanPharmName.length() > 15 ? cleanPharmName.substring(0, 15) : cleanPharmName,
                        drugName.length() > 20 ? drugName.substring(0, 20) : drugName,
                        request.getQuantity(),
                        batchNum.length() > 15 ? batchNum.substring(0, 15) : batchNum
                );
                
                log.info("Generated tracking number: {}", trackingNumber);
            }
            
            // 7. Try to create shipment on blockchain with tracking number
            boolean blockchainSuccess = false;
            TransactionReceipt receipt = null;
            BigInteger shipmentId = BigInteger.valueOf(System.currentTimeMillis());
            
            try {
                // Normalize names for blockchain storage
                String fromLocation = distributor.getName();
                if (fromLocation.contains("XYZ") || fromLocation.contains("Distributor")) fromLocation = "CPC1 Hà Nội";
                
                String toLocation = pharmacy.getName();
                if (toLocation.contains("An Khang") || toLocation.contains("Pharmacy")) toLocation = "Long Châu";

                receipt = blockchainService.createAndDispatchShipment(
                    request.getBatchId(),
                    pharmacy.getWalletAddress(),
                    fromLocation, // Normalized fromLocation
                    toLocation,    // Normalized toLocation
                    BigInteger.valueOf(request.getQuantity()),
                    trackingNumber,         // Pass the meaningful tracking number
                    String.format("Lo: %s - %s", batch.getBatchNumber(), request.getNotes() != null ? request.getNotes() : "Phan phoi den Hieu thuoc") // Optimized notes with Batch Number
                ).get();
                blockchainSuccess = true;
                log.info("Shipment created on blockchain successfully. TX: {}, Tracking: {}", 
                        receipt.getTransactionHash(), trackingNumber);
            } catch (Exception e) {
                log.warn("Failed to create shipment on blockchain, proceeding with local save: {}", e.getMessage());
            }
            
            // 7. Create shipment entity
            Shipment shipment;
            if (blockchainSuccess && receipt != null) {
                shipment = shipmentAdapter.createShipmentFromBlockchain(
                    shipmentId,
                    distributorAddress,
                    pharmacy.getWalletAddress(),
                    request.getQuantity(),
                    trackingNumber,
                    receipt.getTransactionHash(),
                    receipt.getBlockNumber()
                );
            } else {
                shipment = shipmentAdapter.createShipmentFromBlockchain(
                    shipmentId,
                    distributorAddress,
                    pharmacy.getWalletAddress(),
                    request.getQuantity(),
                    trackingNumber,
                    "PENDING_" + System.currentTimeMillis(),
                    BigInteger.ZERO
                );
            }
            
            // 8. Set additional information
            shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
            shipment.setDrugBatch(batch);
            shipment.setShipmentDate(LocalDateTime.now());
            shipment.setExpectedDeliveryDate(LocalDateTime.now().plusDays(2));
            
            // ⭐ VERIFY: Shipment has correct batch with correct blockchain ID
            log.info("✅ Shipment linked to batch - Database ID: {}, Blockchain Batch ID: {}", 
                    batch.getId(), batch.getBatchId());
            
            // 9. Build notes with additional info
            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put("tracking_number", trackingNumber);
            additionalInfo.put("driver_name", request.getDriverName());
            additionalInfo.put("driver_phone", request.getDriverPhone());
            additionalInfo.put("transport_method", request.getTransportMethod());
            additionalInfo.put("user_notes", request.getNotes());
            additionalInfo.put("pharmacy_name", pharmacy.getName());
            additionalInfo.put("pharmacy_address", pharmacy.getAddress());
            
            String currentNotes = shipment.getNotes() != null ? shipment.getNotes() : "{}";
            // Merge with existing notes (which contains blockchain data)
            shipment.setNotes(currentNotes.replace("}", ", \"shipment_info\": " + toJson(additionalInfo) + "}"));
            
            // 10. ✅ Update batch quantity - giảm số lượng khi xuất kho
            // Khi xuất kho thì phải giảm quantity trong drug_batches
            // drug_batches.quantity là source of truth từ blockchain
            if (batch.getQuantity() < request.getQuantity()) {
                throw new IllegalStateException("Không đủ số lượng trong kho. Có sẵn: " + batch.getQuantity());
            }
            
            Long oldQuantity = batch.getQuantity();
            batch.setQuantity(batch.getQuantity() - request.getQuantity());
            drugBatchRepository.save(batch);
            log.info("✅ Reduced drug_batches quantity from {} to {}", oldQuantity, batch.getQuantity());
            
            // 11. Update distributor inventory (KHÔNG giảm quantity, chỉ ghi log)
            // Vì drug_batches đã giảm rồi, distributor_inventory chỉ là bản sao
            // Nếu giảm cả 2 sẽ bị trừ 2 lần
            Long distributorId = pharmaCompanyRepository.findByWalletAddress(distributorAddress)
                    .map(PharmaCompany::getId)
                    .orElse(null);
            
            if (distributorId != null) {
                // Chỉ check xem có inventory record không, KHÔNG giảm quantity
                DistributorInventory inventory = distributorInventoryService
                    .getInventoryByDistributorAndBatch(distributorId, batch.getId());
                
                if (inventory != null) {
                    log.info("✅ Distributor inventory exists (id={}), drug_batches already updated", inventory.getId());
                } else {
                    log.info("ℹ️ No distributor inventory record - batch owned directly from blockchain");
                }
            } else {
                log.warn("⚠️ Could not find distributor with wallet address: {}", distributorAddress);
            }
            
            // 12. Save shipment
            Shipment savedShipment = shipmentRepository.save(shipment);
            log.info("Shipment saved successfully with ID: {}", savedShipment.getId());
            
            // 13. Convert to DTO and return
            return convertToDto(savedShipment, batch, pharmacy);
            
        } catch (Exception e) {
            log.error("Failed to create distributor shipment", e);
            throw new RuntimeException("Không thể tạo phiếu xuất kho: " + e.getMessage(), e);
        }
    }

    public List<ShipmentDto> getAllShipments() {
        List<Shipment> shipments = shipmentRepository.findAll();
        return shipments.stream()
                .map(s -> convertToDto(s, s.getDrugBatch(), s.getToCompany()))
                .collect(Collectors.toList());
    }

    public ShipmentDto getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id.toString()));
        return convertToDto(shipment, shipment.getDrugBatch(), shipment.getToCompany());
    }

    private ShipmentDto convertToDto(Shipment shipment, DrugBatch batch, PharmaCompany pharmacy) {
        // ⭐ Convert batch to DTO to include blockchain batch ID
        DrugBatchDto batchDto = null;
        if (batch != null) {
            batchDto = DrugBatchDto.builder()
                    .id(batch.getId())
                    .batchId(batch.getBatchId()) // ⭐ CRITICAL: Blockchain batch ID
                    .batchNumber(batch.getBatchNumber())
                    .drugName(batch.getDrugName())
                    .manufacturer(batch.getManufacturer())
                    .quantity(batch.getQuantity())
                    .manufactureTimestamp(batch.getManufactureTimestamp())
                    .expiryDate(batch.getExpiryDate())
                    .currentOwner(batch.getCurrentOwner())
                    .status(batch.getStatus() != null ? batch.getStatus().name() : null)
                    .qrCode(batch.getQrCode())
                    .storageConditions(batch.getStorageConditions())
                    .transactionHash(batch.getTransactionHash())
                    .build();
            
            log.info("📦 Including batch in DTO - Blockchain Batch ID: {}", batch.getBatchId());
        }
        
        return ShipmentDto.builder()
                .id(shipment.getId())
                .shipmentCode(shipment.getShipmentCode())
                .shipmentId(shipment.getShipmentId() != null ? shipment.getShipmentId() : BigInteger.valueOf(shipment.getId()))
                .fromAddress(shipment.getFromCompany() != null ? shipment.getFromCompany().getWalletAddress() : null)
                .toAddress(pharmacy != null ? pharmacy.getWalletAddress() : null)
                .quantity(shipment.getQuantity() != null ? shipment.getQuantity().longValue() : 0L)
                .shipmentTimestamp(shipment.getShipmentDate() != null ? shipment.getShipmentDate() : shipment.getCreatedAt())
                .status(shipment.getStatus().name())
                .trackingInfo(shipment.getNotes())
                .transactionHash(shipment.getCreateTxHash())
                .blockNumber(shipment.getBlockNumber())
                .isSynced(shipment.getIsSynced())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .drugBatch(batchDto) // ⭐ CRITICAL: Include full batch info with blockchain ID
                .build();
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            if (value != null) {
                json.append("\"").append(key).append("\": \"").append(value.toString()).append("\", ");
            }
        });
        if (json.length() > 1) {
            json.setLength(json.length() - 2); // Remove trailing comma
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Remove Vietnamese diacritics for blockchain storage
     */
    private String removeVietnameseDiacritics(String str) {
        if (str == null) return "";
        
        String result = str;
        // Lowercase
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("đ", "d");
        
        // Uppercase
        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("Đ", "D");
        
        return result;
    }
}
