package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "drug_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DrugBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "batch_id", nullable = false, unique = true)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private BigInteger batchId;

    @NotNull
    @Size(max = 255)
    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @NotNull
    @Size(max = 255)
    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;

    @NotNull
    @Size(max = 100)
    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @NotNull
    @Size(max = 42)
    @Column(name = "manufacturer_address", nullable = false, length = 42)
    private String manufacturerAddress;

    @NotNull
    @Size(max = 42)
    @Column(name = "current_owner", nullable = false, length = 42)
    private String currentOwner;

    @NotNull
    @Column(name = "manufacture_timestamp", nullable = false)
    private LocalDateTime manufactureTimestamp;

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Size(max = 500)
    @Column(name = "storage_conditions")
    private String storageConditions;

    @Column(name = "raw_material_batch_number")
    private String rawMaterialBatchNumber;

    @Column(name = "raw_material_amount_used")
    private Double rawMaterialAmountUsed;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status = BatchStatus.MANUFACTURED;

    @Size(max = 1000)
    @Column(name = "qr_code")
    private String qrCode;

    @Size(max = 66)
    @Column(name = "transaction_hash", length = 66)
    private String transactionHash;

    @Column(name = "block_number")
    private BigInteger blockNumber;

    @Size(max = 66)
    @Column(name = "items_merkle_root", length = 66)
    private String itemsMerkleRoot;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-many relationship with shipments
    @OneToMany(mappedBy = "drugBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "drugBatch"})
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Shipment> shipments;

    // One-to-many relationship with blockchain transactions
    @OneToMany(mappedBy = "drugBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<BlockchainTransaction> transactions;

    public enum BatchStatus {
        MANUFACTURED,
        IN_TRANSIT,
        DELIVERED,
        SOLD,
        RECALLED
    }
}
