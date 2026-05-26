package com.nckh.dia5.service;

import com.nckh.dia5.handler.ResourceNotFoundException;
import com.nckh.dia5.model.PharmaCompany;
import com.nckh.dia5.model.Shipment;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.nckh.dia5.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentReceiveService {

    private final ShipmentRepository shipmentRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final DistributorInventoryService distributorInventoryService;
    private final PharmacyInventoryService pharmacyInventoryService;

    /**
     * Receive shipment and automatically update inventory
     */
    @Transactional
    public void receiveShipment(Long shipmentId, String receiverWalletAddress) {
        log.info("Receiving shipment: shipmentId={}, receiverWalletAddress={}", shipmentId, receiverWalletAddress);

        // Get shipment
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId.toString()));

        // Get receiver company
        PharmaCompany receiver = pharmaCompanyRepository.findByWalletAddress(receiverWalletAddress)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "walletAddress", receiverWalletAddress));

        // Verify receiver is the intended recipient
        if (!shipment.getToCompany().getId().equals(receiver.getId())) {
            throw new IllegalStateException("Bạn không phải người nhận của lô hàng này");
        }

        // Update shipment status
        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        shipment.setActualDeliveryDate(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // Update inventory based on company type
        try {
            if (receiver.getCompanyType() == PharmaCompany.CompanyType.DISTRIBUTOR) {
                // Receive to distributor inventory
                distributorInventoryService.receiveShipment(
                    receiver.getId(),
                    shipment.getDrugBatch().getId(),
                    shipment.getQuantity(),
                    shipment
                );
                log.info("Updated distributor inventory for company: {}", receiver.getName());
                
            } else if (receiver.getCompanyType() == PharmaCompany.CompanyType.PHARMACY) {
                // Receive to pharmacy inventory
                pharmacyInventoryService.receiveShipment(
                    receiver.getId(),
                    shipment.getDrugBatch().getId(),
                    shipment.getQuantity(),
                    shipment
                );
                log.info("Updated pharmacy inventory for company: {}", receiver.getName());
            }
        } catch (Exception e) {
            log.error("Failed to update inventory after receiving shipment", e);
            throw new RuntimeException("Không thể cập nhật kho hàng: " + e.getMessage(), e);
        }
    }
}
