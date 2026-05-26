package com.nckh.dia5.service;

import com.nckh.dia5.model.*;
import com.nckh.dia5.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service để xử lý shipment với item-level tracking
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductItemShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductItemMovementRepository movementRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final MerkleTreeService merkleTreeService;

    /**
     * Tạo shipment với danh sách items cụ thể
     */
    @Transactional
    public ShipmentItemResponse createShipmentWithItems(CreateShipmentWithItemsRequest request) {
        log.info("Creating shipment with {} items", request.getItemIds().size());

        // Validate companies
        PharmaCompany fromCompany = pharmaCompanyRepository.findById(request.getFromCompanyId())
                .orElseThrow(() -> new RuntimeException("From company not found"));

        PharmaCompany toCompany = pharmaCompanyRepository.findById(request.getToCompanyId())
                .orElseThrow(() -> new RuntimeException("To company not found"));

        // Validate items
        List<ProductItem> items = new ArrayList<>();
        for (Long itemId : request.getItemIds()) {
            ProductItem item = productItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

            // Verify ownership
            if (!item.getCurrentOwnerId().equals(request.getFromCompanyId())) {
                throw new RuntimeException("Item " + item.getItemCode() + " does not belong to sender");
            }

            // Verify status
            if (item.getCurrentStatus() == ProductItem.ItemStatus.SOLD ||
                item.getCurrentStatus() == ProductItem.ItemStatus.RECALLED) {
                throw new RuntimeException("Item " + item.getItemCode() + " cannot be shipped (status: " + item.getCurrentStatus() + ")");
            }

            items.add(item);
        }

        // Get batch from first item (all items should be from same batch ideally)
        DrugBatch batch = items.get(0).getDrugBatch();

        // Create shipment
        Shipment shipment = new Shipment();
        shipment.setShipmentCode(generateShipmentCode());
        shipment.setFromCompany(fromCompany);
        shipment.setToCompany(toCompany);
        shipment.setDrugBatch(batch);
        shipment.setQuantity(items.size());
        shipment.setShipmentDate(LocalDateTime.now());
        shipment.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);

        // Store item IDs in notes as JSON
        StringBuilder itemIdsJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            itemIdsJson.append(items.get(i).getId());
            if (i < items.size() - 1) itemIdsJson.append(",");
        }
        itemIdsJson.append("]");
        
        shipment.setNotes(String.format(
            "{\"shipping_method\":\"ITEM_LEVEL\",\"item_ids\":%s,\"transport_method\":\"%s\",\"tracking_number\":\"%s\"}",
            itemIdsJson,
            request.getTransportMethod(),
            request.getTrackingNumber()
        ));

        Shipment savedShipment = shipmentRepository.save(shipment);
        log.info("Created shipment: {}", savedShipment.getShipmentCode());

        // Create movements for each item
        List<ProductItemMovement> movements = new ArrayList<>();
        for (ProductItem item : items) {
            // Create SHIP movement
            ProductItemMovement movement = new ProductItemMovement();
            movement.setProductItem(item);
            movement.setDrugBatch(batch);
            movement.setMovementType(ProductItemMovement.MovementType.SHIP);
            
            movement.setFromCompanyId(request.getFromCompanyId());
            movement.setFromCompanyType(fromCompany.getCompanyType() == PharmaCompany.CompanyType.MANUFACTURER 
                ? ProductItem.OwnerType.MANUFACTURER 
                : (fromCompany.getCompanyType() == PharmaCompany.CompanyType.DISTRIBUTOR 
                    ? ProductItem.OwnerType.DISTRIBUTOR 
                    : ProductItem.OwnerType.PHARMACY));
            movement.setFromCompanyName(fromCompany.getName());
            movement.setFromAddressDetail(fromCompany.getAddress());
            
            movement.setToCompanyId(request.getToCompanyId());
            movement.setToCompanyType(toCompany.getCompanyType() == PharmaCompany.CompanyType.MANUFACTURER 
                ? ProductItem.OwnerType.MANUFACTURER 
                : (toCompany.getCompanyType() == PharmaCompany.CompanyType.DISTRIBUTOR 
                    ? ProductItem.OwnerType.DISTRIBUTOR 
                    : ProductItem.OwnerType.PHARMACY));
            movement.setToCompanyName(toCompany.getName());
            movement.setToAddressDetail(toCompany.getAddress());
            
            movement.setShipment(savedShipment);
            movement.setRelatedTransactionId(savedShipment.getShipmentCode());
            movement.setMovementTimestamp(LocalDateTime.now());
            movement.setVerificationMethod(ProductItemMovement.VerificationMethod.QR_SCAN);
            movement.setNotes(request.getNotes());

            movements.add(movement);

            // Update item status
            item.setCurrentStatus(ProductItem.ItemStatus.IN_TRANSIT);
            productItemRepository.save(item);
        }

        movementRepository.saveAll(movements);
        log.info("Created {} movements for shipment", movements.size());

        // Generate Merkle Tree for blockchain sync
        List<String> itemCodes = items.stream()
                .map(ProductItem::getItemCode)
                .toList();
        MerkleTreeService.MerkleTree merkleTree = merkleTreeService.createMerkleTree(itemCodes);

        return ShipmentItemResponse.builder()
                .shipmentId(savedShipment.getId())
                .shipmentCode(savedShipment.getShipmentCode())
                .itemCount(items.size())
                .fromCompany(fromCompany.getName())
                .toCompany(toCompany.getName())
                .status(savedShipment.getStatus().toString())
                .merkleRoot(merkleTree.getRoot())
                .items(items)
                .build();
    }

    /**
     * Nhận hàng - quét từng sản phẩm
     */
    @Transactional
    public ShipmentReceiveResponse receiveShipmentItems(Long shipmentId, List<Long> receivedItemIds) {
        log.info("Receiving {} items for shipment {}", receivedItemIds.size(), shipmentId);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getStatus() != Shipment.ShipmentStatus.IN_TRANSIT) {
            throw new RuntimeException("Shipment is not in IN_TRANSIT status");
        }

        PharmaCompany toCompany = shipment.getToCompany();
        ProductItem.OwnerType newOwnerType = convertCompanyTypeToOwnerType(toCompany.getCompanyType());

        List<ProductItem> receivedItems = new ArrayList<>();
        List<ProductItemMovement> movements = new ArrayList<>();

        for (Long itemId : receivedItemIds) {
            ProductItem item = productItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

            // Verify item belongs to this shipment
            if (item.getCurrentStatus() != ProductItem.ItemStatus.IN_TRANSIT) {
                log.warn("Item {} is not in IN_TRANSIT status, skipping", item.getItemCode());
                continue;
            }

            // Create RECEIVE movement
            ProductItemMovement movement = new ProductItemMovement();
            movement.setProductItem(item);
            movement.setDrugBatch(item.getDrugBatch());
            movement.setMovementType(ProductItemMovement.MovementType.RECEIVE);
            
            movement.setFromCompanyId(shipment.getFromCompany().getId());
            movement.setFromCompanyType(convertCompanyTypeToOwnerType(shipment.getFromCompany().getCompanyType()));
            movement.setFromCompanyName(shipment.getFromCompany().getName());
            
            movement.setToCompanyId(toCompany.getId());
            movement.setToCompanyType(newOwnerType);
            movement.setToCompanyName(toCompany.getName());
            movement.setToAddressDetail(toCompany.getAddress());
            
            movement.setShipment(shipment);
            movement.setMovementTimestamp(LocalDateTime.now());
            movement.setVerificationMethod(ProductItemMovement.VerificationMethod.QR_SCAN);
            movement.setNotes("Received and verified by QR scan");

            movements.add(movement);

            // Update item
            item.setCurrentStatus(ProductItem.ItemStatus.DELIVERED);
            item.setCurrentOwnerId(toCompany.getId());
            item.setCurrentOwnerType(newOwnerType);
            
            receivedItems.add(item);
        }

        productItemRepository.saveAll(receivedItems);
        movementRepository.saveAll(movements);

        // Update shipment status if all items received
        if (receivedItems.size() == shipment.getQuantity()) {
            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
            shipment.setActualDeliveryDate(LocalDateTime.now());
            shipmentRepository.save(shipment);
        }

        log.info("Received {} items successfully", receivedItems.size());

        return ShipmentReceiveResponse.builder()
                .shipmentId(shipmentId)
                .shipmentCode(shipment.getShipmentCode())
                .receivedCount(receivedItems.size())
                .totalCount(shipment.getQuantity())
                .isComplete(receivedItems.size() == shipment.getQuantity())
                .receivedItems(receivedItems)
                .build();
    }

    /**
     * Quét hàng loạt items trong shipment
     */
    @Transactional
    public BulkScanResponse bulkScanItems(Long shipmentId, List<String> scannedItemCodes) {
        log.info("Bulk scanning {} items for shipment {}", scannedItemCodes.size(), shipmentId);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        List<ProductItem> validItems = new ArrayList<>();
        List<String> invalidCodes = new ArrayList<>();
        List<String> alreadyReceivedCodes = new ArrayList<>();

        for (String itemCode : scannedItemCodes) {
            ProductItem item = productItemRepository.findByItemCode(itemCode).orElse(null);
            
            if (item == null) {
                invalidCodes.add(itemCode);
                continue;
            }

            if (item.getCurrentStatus() == ProductItem.ItemStatus.DELIVERED) {
                alreadyReceivedCodes.add(itemCode);
                continue;
            }

            if (item.getCurrentStatus() != ProductItem.ItemStatus.IN_TRANSIT) {
                invalidCodes.add(itemCode + " (invalid status: " + item.getCurrentStatus() + ")");
                continue;
            }

            validItems.add(item);
        }

        // Auto-receive valid items
        if (!validItems.isEmpty()) {
            List<Long> itemIds = validItems.stream().map(ProductItem::getId).toList();
            receiveShipmentItems(shipmentId, itemIds);
        }

        return BulkScanResponse.builder()
                .totalScanned(scannedItemCodes.size())
                .validCount(validItems.size())
                .invalidCount(invalidCodes.size())
                .alreadyReceivedCount(alreadyReceivedCodes.size())
                .invalidCodes(invalidCodes)
                .alreadyReceivedCodes(alreadyReceivedCodes)
                .build();
    }

    /**
     * Get items trong shipment
     */
    public List<ProductItem> getShipmentItems(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        return movementRepository.findByShipmentId(shipmentId).stream()
                .map(ProductItemMovement::getProductItem)
                .distinct()
                .toList();
    }

    // Helper methods
    private String generateShipmentCode() {
        return "SHIP-" + System.currentTimeMillis();
    }

    private ProductItem.OwnerType convertCompanyTypeToOwnerType(PharmaCompany.CompanyType companyType) {
        return switch (companyType) {
            case MANUFACTURER -> ProductItem.OwnerType.MANUFACTURER;
            case DISTRIBUTOR -> ProductItem.OwnerType.DISTRIBUTOR;
            case PHARMACY -> ProductItem.OwnerType.PHARMACY;
        };
    }

    // DTOs
    @lombok.Builder
    @lombok.Data
    public static class CreateShipmentWithItemsRequest {
        private Long fromCompanyId;
        private Long toCompanyId;
        private List<Long> itemIds;
        private LocalDateTime expectedDeliveryDate;
        private String transportMethod;
        private String trackingNumber;
        private String notes;
    }

    @lombok.Builder
    @lombok.Data
    public static class ShipmentItemResponse {
        private Long shipmentId;
        private String shipmentCode;
        private Integer itemCount;
        private String fromCompany;
        private String toCompany;
        private String status;
        private String merkleRoot;
        private List<ProductItem> items;
    }

    @lombok.Builder
    @lombok.Data
    public static class ShipmentReceiveResponse {
        private Long shipmentId;
        private String shipmentCode;
        private Integer receivedCount;
        private Integer totalCount;
        private Boolean isComplete;
        private List<ProductItem> receivedItems;
    }

    @lombok.Builder
    @lombok.Data
    public static class BulkScanResponse {
        private Integer totalScanned;
        private Integer validCount;
        private Integer invalidCount;
        private Integer alreadyReceivedCount;
        private List<String> invalidCodes;
        private List<String> alreadyReceivedCodes;
    }
}

