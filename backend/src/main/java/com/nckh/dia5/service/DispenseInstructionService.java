package com.nckh.dia5.service;

import com.nckh.dia5.dto.DispenseInstructionDTO;
import com.nckh.dia5.dto.DispenseWithInstructionsRequest;
import com.nckh.dia5.model.*;
import com.nckh.dia5.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing dispense instructions (medication usage guidance)
 * Extends the existing DispenseService to include usage instructions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispenseInstructionService {

    private final DispenseService dispenseService;
    private final DispenseInstructionRepository dispenseInstructionRepository;
    private final ProductItemRepository productItemRepository;
    private final AppUserRepository appUserRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;

    /**
     * Dispense an item with usage instructions
     * Called by pharmacy when selling a drug
     */
    @Transactional
    public DispenseResult dispenseWithInstructions(
            DispenseWithInstructionsRequest request,
            Long pharmacyId,
            String pharmacyName) {

        log.info("Dispensing item {} with instructions", request.getItemCode());

        // Step 1: Perform the standard dispense (update status to SOLD)
        DispenseService.DispenseResult dispenseResult =
                dispenseService.dispenseItem(request.getItemCode(), request.getCustomerPhone(), null);

        if (!dispenseResult.isSuccess()) {
            log.warn("Failed to dispense item: {}", dispenseResult.getStatusMessage());
            return DispenseResult.builder()
                    .success(false)
                    .message(dispenseResult.getStatusMessage())
                    .alreadyDispensed(dispenseResult.isAlreadyDispensed())
                    .build();
        }

        // Step 2: Find the product item
        Optional<ProductItem> itemOpt = productItemRepository.findByItemCode(request.getItemCode());
        if (itemOpt.isEmpty()) {
            log.error("Product item not found: {}", request.getItemCode());
            return DispenseResult.builder()
                    .success(false)
                    .message("Không tìm thấy sản phẩm")
                    .build();
        }

        ProductItem item = itemOpt.get();

        // Step 3: Check if instructions already exist for this item
        if (dispenseInstructionRepository.existsByProductItemId(item.getId())) {
            log.warn("Instructions already exist for item: {}", request.getItemCode());
            return DispenseResult.builder()
                    .success(true)
                    .message("Bán hàng thành công (hướng dẫn đã tồn tại)")
                    .itemCode(request.getItemCode())
                    .drugName(item.getDrugProduct().getName())
                    .build();
        }

        // Step 4: Create dispense instruction
        DispenseInstruction instruction = new DispenseInstruction();
        instruction.setProductItemId(item.getId());
        instruction.setPharmacyId(pharmacyId);
        instruction.setMovementId(dispenseResult.getMovementId());

        // Drug info
        instruction.setDrugName(item.getDrugProduct().getName());
        instruction.setBatchNumber(item.getDrugBatch().getBatchNumber());
        instruction.setItemCode(request.getItemCode());

        // Customer info
        instruction.setCustomerName(request.getCustomerName());
        instruction.setCustomerPhone(request.getCustomerPhone());

        // Try to link to existing app user
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
            appUserRepository.findByPhone(request.getCustomerPhone())
                    .ifPresent(user -> instruction.setCustomerAppUserId(user.getId()));
        }

        // Usage instructions
        if (request.getDosage() != null) instruction.setDosage(request.getDosage());
        if (request.getFrequency() != null) instruction.setFrequency(request.getFrequency());
        instruction.setMealRelation(request.getMealRelationEnum());
        if (request.getSpecificTimes() != null) instruction.setSpecificTimes(request.getSpecificTimes());
        if (request.getDurationDays() != null) instruction.setDurationDays(request.getDurationDays());
        if (request.getSpecialNotes() != null) instruction.setSpecialNotes(request.getSpecialNotes());

        // Warnings
        if (request.getWarnings() != null) instruction.setWarnings(request.getWarnings());
        if (request.getContraindications() != null) instruction.setContraindications(request.getContraindications());

        // Pharmacist info
        if (request.getPharmacistName() != null) instruction.setPharmacistName(request.getPharmacistName());
        if (request.getPharmacistLicense() != null) instruction.setPharmacistLicense(request.getPharmacistLicense());

        // Sale info
        instruction.setSalePrice(request.getSalePrice());
        instruction.setDispensedAt(LocalDateTime.now());

        // Save
        DispenseInstruction saved = dispenseInstructionRepository.save(instruction);

        log.info("Created dispense instruction {} for item {}", saved.getId(), request.getItemCode());

        return DispenseResult.builder()
                .success(true)
                .message("Bán hàng thành công với hướng dẫn sử dụng")
                .itemCode(request.getItemCode())
                .drugName(item.getDrugProduct().getName())
                .instructionId(saved.getId())
                .transactionHash(dispenseResult.getTransactionHash())
                .build();
    }

    /**
     * Get dispense instruction by item code
     * Called by mobile app when scanning QR of a purchased drug
     */
    public Optional<DispenseInstructionDTO> getInstructionByItemCode(String itemCode) {
        return dispenseInstructionRepository.findByItemCode(itemCode)
                .map(instruction -> {
                    DispenseInstructionDTO dto = DispenseInstructionDTO.fromEntity(instruction);

                    // Add pharmacy name
                    pharmaCompanyRepository.findById(instruction.getPharmacyId())
                            .ifPresent(pharmacy -> dto.setPharmacyName(pharmacy.getName()));

                    // Add manufacturer and expiry from product item
                    if (instruction.getProductItem() != null) {
                        ProductItem item = instruction.getProductItem();
                        dto.setManufacturer(item.getDrugBatch().getManufacturer());
                        dto.setExpiryDate(item.getExpiryDate().toLocalDate());
                    }

                    return dto;
                });
    }

    /**
     * Get dispense instruction by product item ID
     */
    public Optional<DispenseInstruction> getInstructionByProductItemId(Long productItemId) {
        return dispenseInstructionRepository.findByProductItemId(productItemId);
    }

    /**
     * Get instructions for a customer by phone
     */
    public List<DispenseInstruction> getInstructionsByCustomerPhone(String phone) {
        return dispenseInstructionRepository.findByCustomerPhoneOrderByDispensedAtDesc(phone);
    }

    /**
     * Get instructions for a pharmacy
     */
    public List<DispenseInstruction> getInstructionsByPharmacy(Long pharmacyId) {
        return dispenseInstructionRepository.findByPharmacyIdOrderByDispensedAtDesc(pharmacyId);
    }

    /**
     * Get instructions for an app user
     */
    public List<DispenseInstruction> getInstructionsByAppUser(Long appUserId) {
        return dispenseInstructionRepository.findByCustomerAppUserIdOrderByDispensedAtDesc(appUserId);
    }

    /**
     * Result of dispense with instructions operation
     */
    @Data
    @lombok.Builder
    public static class DispenseResult {
        private boolean success;
        private String message;
        private String itemCode;
        private String drugName;
        private Long instructionId;
        private boolean alreadyDispensed;
        private String transactionHash;
    }
}
