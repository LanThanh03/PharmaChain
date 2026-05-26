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

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "item_code", nullable = false, unique = true)
    private String itemCode;

    // Relationships
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DrugBatch drugBatch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DrugProduct drugProduct;

    // QR Code information
    @Size(max = 500)
    @Column(name = "qr_code_data")
    private String qrCodeData;

    @Size(max = 500)
    @Column(name = "qr_image_path")
    private String qrImagePath;

    @Column(name = "qr_generated_at")
    private LocalDateTime qrGeneratedAt;

    // Status tracking
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private ItemStatus currentStatus = ItemStatus.MANUFACTURED;

    // Ownership tracking
    @Column(name = "current_owner_id")
    private Long currentOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_owner_type")
    private OwnerType currentOwnerType;

    // Product dates (denormalized for quick query)
    @NotNull
    @Column(name = "manufacture_date", nullable = false)
    private LocalDateTime manufactureDate;

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    // Blockchain tracking
    @Column(name = "blockchain_token_id")
    private Long blockchainTokenId;

    @Column(name = "blockchain_merkle_proof", columnDefinition = "TEXT")
    private String blockchainMerkleProof;

    @Column(name = "is_blockchain_synced", nullable = false)
    private Boolean isBlockchainSynced = false;

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-many relationships
    @OneToMany(mappedBy = "productItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<ProductItemMovement> movements;

    @OneToMany(mappedBy = "productItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<ProductItemVerification> verifications;

    // Enums
    public enum ItemStatus {
        MANUFACTURED,
        IN_WAREHOUSE,
        IN_TRANSIT,
        DELIVERED,
        SOLD,
        EXPIRED,
        RECALLED,
        DAMAGED
    }

    public enum OwnerType {
        MANUFACTURER,
        DISTRIBUTOR,
        PHARMACY,
        CONSUMER
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        return LocalDateTime.now().plusDays(daysThreshold).isAfter(expiryDate);
    }

    public long getDaysUntilExpiry() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }
}

