package com.nckh.dia5.service;

import com.nckh.dia5.dto.pharmacy.PharmacyInventoryDto;
import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.model.*;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.nckh.dia5.repository.PharmacyInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyInventoryService {

    private final PharmacyInventoryRepository inventoryRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;

    /**
     * Get all inventory for a pharmacy
     */
    public List<PharmacyInventory> getInventoryByPharmacyId(Long pharmacyId) {
        return inventoryRepository.findByPharmacyId(pharmacyId);
    }

    /**
     * Get inventory by pharmacy wallet address
     */
    public List<PharmacyInventory> getInventoryByWalletAddress(String walletAddress) {
        return inventoryRepository.findByPharmacyWalletAddress(walletAddress);
    }

    /**
     * Get inventory by pharmacy wallet address (DTO version to avoid lazy loading issues)
     */
    public List<PharmacyInventoryDto> getInventoryDtoByWalletAddress(String walletAddress) {
        List<PharmacyInventory> inventoryList = inventoryRepository.findByPharmacyWalletAddress(walletAddress);
        return inventoryList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert PharmacyInventory entity to DTO
     */
    private PharmacyInventoryDto convertToDto(PharmacyInventory inventory) {
        return PharmacyInventoryDto.builder()
                .id(inventory.getId())
                .drugName(inventory.getDrugName())
                .manufacturer(inventory.getManufacturer())
                .batchNumber(inventory.getBatchNumber())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .soldQuantity(inventory.getSoldQuantity())
                .manufactureDate(inventory.getManufactureDate())
                .expiryDate(inventory.getExpiryDate())
                .qrCode(inventory.getQrCode())
                .shelfLocation(inventory.getShelfLocation())
                .storageConditions(inventory.getStorageConditions())
                .storageTemperature(inventory.getStorageTemperature())
                .costPrice(inventory.getCostPrice())
                .retailPrice(inventory.getRetailPrice())
                .discountPrice(inventory.getDiscountPrice())
                .totalValue(inventory.getTotalValue())
                .profitMargin(inventory.getProfitMargin())
                .status(inventory.getStatus() != null ? inventory.getStatus().name() : null)
                .minStockLevel(inventory.getMinStockLevel())
                .maxStockLevel(inventory.getMaxStockLevel())
                .reorderPoint(inventory.getReorderPoint())
                .blockchainBatchId(inventory.getBlockchainBatchId())
                .receiveTxHash(inventory.getReceiveTxHash())
                .currentOwnerAddress(inventory.getCurrentOwnerAddress())
                .isVerified(inventory.getIsVerified())
                .receivedFromDistributorId(inventory.getReceivedFromDistributor() != null ? inventory.getReceivedFromDistributor().getId() : null)
                .receivedFromDistributorName(inventory.getReceivedFromDistributor() != null ? inventory.getReceivedFromDistributor().getName() : null)
                .receivedShipmentId(inventory.getReceivedShipment() != null ? inventory.getReceivedShipment().getId() : null)
                .receivedDate(inventory.getReceivedDate())
                .receivedQuantity(inventory.getReceivedQuantity())
                .firstSaleDate(inventory.getFirstSaleDate())
                .lastSaleDate(inventory.getLastSaleDate())
                .averageDailySales(inventory.getAverageDailySales())
                .daysOfSupply(inventory.getDaysOfSupply())
                .requiresPrescription(inventory.getRequiresPrescription())
                .controlledSubstance(inventory.getControlledSubstance())
                .isFeatured(inventory.getIsFeatured())
                .isOnSale(inventory.getIsOnSale())
                .displayOrder(inventory.getDisplayOrder())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .createdBy(inventory.getCreatedBy())
                .updatedBy(inventory.getUpdatedBy())
                .notes(inventory.getNotes())
                .build();
    }

    /**
     * Get low stock items
     */
    public List<PharmacyInventory> getLowStockItems(Long pharmacyId) {
        return inventoryRepository.findLowStockItems(pharmacyId);
    }

    /**
     * Get items that need reorder
     */
    public List<PharmacyInventory> getItemsNeedReorder(Long pharmacyId) {
        return inventoryRepository.findItemsNeedReorder(pharmacyId);
    }

    /**
     * Get expiring soon items
     */
    public List<PharmacyInventory> getExpiringSoonItems(Long pharmacyId) {
        return inventoryRepository.findExpiringSoonItems(pharmacyId);
    }

    /**
     * Search inventory by drug name
     */
    public List<PharmacyInventory> searchByDrugName(Long pharmacyId, String searchTerm) {
        return inventoryRepository.searchByDrugName(pharmacyId, searchTerm);
    }

    /**
     * Get featured products
     */
    public List<PharmacyInventory> getFeaturedProducts(Long pharmacyId) {
        return inventoryRepository.findByPharmacyIdAndIsFeaturedTrue(pharmacyId);
    }

    /**
     * Get products on sale
     */
    public List<PharmacyInventory> getProductsOnSale(Long pharmacyId) {
        return inventoryRepository.findByPharmacyIdAndIsOnSaleTrue(pharmacyId);
    }

    /**
     * Add or update inventory when receiving shipment
     */
    @Transactional
    public PharmacyInventory receiveShipment(Long pharmacyId, Long batchId, Integer quantity, Shipment shipment) {
        log.info("📦 Receiving shipment to pharmacy inventory: pharmacyId={}, batchId={}, quantity={}", 
                pharmacyId, batchId, quantity);

        // Get pharmacy
        PharmaCompany pharmacy = pharmaCompanyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", pharmacyId.toString()));

        // Get batch
        DrugBatch batch = drugBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId.toString()));

        // ✅ LOG BATCH INFO TO VERIFY CONSISTENCY
        log.info("✅ Batch Info - Database ID: {}, Blockchain Batch ID: {}, Batch Number: {}", 
                batch.getId(), 
                batch.getBatchId(), 
                batch.getBatchNumber());

        // Check if inventory record exists
        PharmacyInventory inventory = inventoryRepository
                .findByPharmacyIdAndDrugBatchId(pharmacyId, batchId)
                .orElse(null);

        if (inventory != null) {
            // Update existing inventory
            log.info("📝 Updating existing inventory record: id={}, blockchain_batch_id={}", 
                    inventory.getId(), inventory.getBlockchainBatchId());
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventory.setReceivedShipment(shipment);
            inventory.setReceivedDate(LocalDateTime.now());
            inventory.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new inventory record
            log.info("✨ Creating new inventory record for pharmacy");
            inventory = new PharmacyInventory();
            inventory.setPharmacy(pharmacy);
            inventory.setDrugBatch(batch);
            inventory.setDrugName(batch.getDrugName());
            inventory.setManufacturer(batch.getManufacturer());
            inventory.setBatchNumber(batch.getBatchNumber());
            inventory.setQuantity(quantity);
            inventory.setManufactureDate(batch.getManufactureTimestamp());
            inventory.setExpiryDate(batch.getExpiryDate());
            inventory.setQrCode(batch.getQrCode());
            
            // ⭐ CRITICAL: Use blockchain batch ID for traceability
            inventory.setBlockchainBatchId(batch.getBatchId());
            
            inventory.setCurrentOwnerAddress(pharmacy.getWalletAddress());
            inventory.setReceivedFromDistributor(shipment.getFromCompany());
            inventory.setReceivedShipment(shipment);
            inventory.setReceivedDate(LocalDateTime.now());
            inventory.setReceivedQuantity(quantity);
            inventory.setIsVerified(true);
            
            log.info("✅ Inventory created with Blockchain Batch ID: {}", batch.getBatchId());
        }

        return inventoryRepository.save(inventory);
    }

    /**
     * Record a sale
     */
    @Transactional
    public PharmacyInventory recordSale(Long pharmacyId, Long batchId, Integer quantity) {
        log.info("Recording sale from pharmacy inventory: pharmacyId={}, batchId={}, quantity={}", 
                pharmacyId, batchId, quantity);

        PharmacyInventory inventory = inventoryRepository
                .findByPharmacyIdAndDrugBatchId(pharmacyId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "batchId", batchId.toString()));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Không đủ số lượng trong kho. Có sẵn: " + inventory.getAvailableQuantity());
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setSoldQuantity(inventory.getSoldQuantity() + quantity);
        inventory.setLastSaleDate(LocalDateTime.now());

        if (inventory.getFirstSaleDate() == null) {
            inventory.setFirstSaleDate(LocalDateTime.now());
        }

        return inventoryRepository.save(inventory);
    }

    /**
     * Reserve quantity for online order
     */
    @Transactional
    public PharmacyInventory reserveQuantity(Long pharmacyId, Long batchId, Integer quantity) {
        PharmacyInventory inventory = inventoryRepository
                .findByPharmacyIdAndDrugBatchId(pharmacyId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "batchId", batchId.toString()));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Không đủ số lượng có thể đặt trước. Có sẵn: " + inventory.getAvailableQuantity());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        return inventoryRepository.save(inventory);
    }

    /**
     * Release reserved quantity
     */
    @Transactional
    public PharmacyInventory releaseReservedQuantity(Long pharmacyId, Long batchId, Integer quantity) {
        PharmacyInventory inventory = inventoryRepository
                .findByPharmacyIdAndDrugBatchId(pharmacyId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "batchId", batchId.toString()));

        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        return inventoryRepository.save(inventory);
    }

    /**
     * Get total inventory value (cost)
     */
    public Double getTotalInventoryValue(Long pharmacyId) {
        return inventoryRepository.getTotalInventoryValue(pharmacyId);
    }

    /**
     * Get total retail value
     */
    public Double getTotalRetailValue(Long pharmacyId) {
        return inventoryRepository.getTotalRetailValue(pharmacyId);
    }
}
