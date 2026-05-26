package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PharmacyInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Company relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany pharmacy;

    // Batch relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DrugBatch drugBatch;

    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    // Quantity fields
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    // available_quantity is GENERATED ALWAYS in DB
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private Integer availableQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    // Batch info
    @Column(name = "manufacture_date")
    private LocalDateTime manufactureDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "qr_code", length = 1000)
    private String qrCode;

    // Storage location
    @Column(name = "shelf_location", length = 100)
    private String shelfLocation = "Kệ chính";

    @Column(name = "storage_conditions", length = 500)
    private String storageConditions;

    @Column(name = "storage_temperature", length = 50)
    private String storageTemperature;

    // Pricing
    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "retail_price", precision = 15, scale = 2)
    private BigDecimal retailPrice = BigDecimal.ZERO;

    @Column(name = "discount_price", precision = 15, scale = 2)
    private BigDecimal discountPrice;

    // total_value and profit_margin are GENERATED in DB
    @Column(name = "total_value", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "profit_margin", insertable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal profitMargin;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PharmacyInventoryStatus status = PharmacyInventoryStatus.IN_STOCK;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 20;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel = 500;

    @Column(name = "reorder_point")
    private Integer reorderPoint = 30;

    // Blockchain tracking
    @Column(name = "blockchain_batch_id", precision = 38, scale = 0)
    private BigInteger blockchainBatchId;

    @Column(name = "receive_tx_hash", length = 66)
    private String receiveTxHash;

    @Column(name = "current_owner_address", length = 42)
    private String currentOwnerAddress;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    // Receipt info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_from_distributor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany receivedFromDistributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_shipment_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Shipment receivedShipment;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "received_quantity")
    private Integer receivedQuantity = 0;

    // Sales info
    @Column(name = "first_sale_date")
    private LocalDateTime firstSaleDate;

    @Column(name = "last_sale_date")
    private LocalDateTime lastSaleDate;

    @Column(name = "average_daily_sales", precision = 10, scale = 2)
    private BigDecimal averageDailySales = BigDecimal.ZERO;

    // days_of_supply is GENERATED in DB
    @Column(name = "days_of_supply", insertable = false, updatable = false)
    private Integer daysOfSupply;

    // Prescription flags
    @Column(name = "requires_prescription")
    private Boolean requiresPrescription = false;

    @Column(name = "controlled_substance")
    private Boolean controlledSubstance = false;

    // Display & Marketing
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "is_on_sale")
    private Boolean isOnSale = false;

    @Column(name = "display_order")
    private Integer displayOrder = 999;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum PharmacyInventoryStatus {
        IN_STOCK,
        LOW_STOCK,
        OUT_OF_STOCK,
        EXPIRING_SOON,
        EXPIRED,
        RECALL
    }
}
