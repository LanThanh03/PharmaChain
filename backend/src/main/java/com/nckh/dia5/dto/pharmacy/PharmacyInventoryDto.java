package com.nckh.dia5.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyInventoryDto {
    
    private Long id;
    
    // Basic info
    private String drugName;
    private String manufacturer;
    private String batchNumber;
    
    // Quantity fields
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer soldQuantity;
    
    // Batch info
    private LocalDateTime manufactureDate;
    private LocalDateTime expiryDate;
    private String qrCode;
    
    // Storage
    private String shelfLocation;
    private String storageConditions;
    private String storageTemperature;
    
    // Pricing
    private BigDecimal costPrice;
    private BigDecimal retailPrice;
    private BigDecimal discountPrice;
    private BigDecimal totalValue;
    private BigDecimal profitMargin;
    
    // Status
    private String status;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private Integer reorderPoint;
    
    // Blockchain tracking
    private BigInteger blockchainBatchId;
    private String receiveTxHash;
    private String currentOwnerAddress;
    private Boolean isVerified;
    
    // Receipt info
    private Long receivedFromDistributorId;
    private String receivedFromDistributorName;
    private Long receivedShipmentId;
    private LocalDateTime receivedDate;
    private Integer receivedQuantity;
    
    // Sales info
    private LocalDateTime firstSaleDate;
    private LocalDateTime lastSaleDate;
    private BigDecimal averageDailySales;
    private Integer daysOfSupply;
    
    // Prescription flags
    private Boolean requiresPrescription;
    private Boolean controlledSubstance;
    
    // Display & Marketing
    private Boolean isFeatured;
    private Boolean isOnSale;
    private Integer displayOrder;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private String notes;
}
