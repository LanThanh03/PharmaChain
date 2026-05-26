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
@Table(name = "distributor_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DistributorInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Company relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany distributor;

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

    // available_quantity is GENERATED ALWAYS in DB, so we use insertable/updatable = false
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private Integer availableQuantity;

    // Batch info
    @Column(name = "manufacture_date")
    private LocalDateTime manufactureDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "qr_code", length = 1000)
    private String qrCode;

    // Storage location
    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation = "Kho chính";

    @Column(name = "storage_conditions", length = 500)
    private String storageConditions;

    @Column(name = "storage_temperature", length = 50)
    private String storageTemperature;

    // Pricing
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    // total_value is GENERATED in DB
    @Column(name = "total_value", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InventoryStatus status = InventoryStatus.GOOD;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 100;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel = 10000;

    // Blockchain tracking
    @Column(name = "blockchain_batch_id", precision = 38, scale = 0)
    private BigInteger blockchainBatchId;

    @Column(name = "receive_tx_hash", length = 66)
    private String receiveTxHash;

    @Column(name = "current_owner_address", length = 42)
    private String currentOwnerAddress;

    // Receipt info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_from_company_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany receivedFromCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_shipment_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Shipment receivedShipment;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "received_quantity")
    private Integer receivedQuantity = 0;

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

    public enum InventoryStatus {
        GOOD,
        LOW_STOCK,
        EXPIRING_SOON,
        EXPIRED,
        QUARANTINE
    }
}
