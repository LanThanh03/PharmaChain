package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_item_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductItemMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Relationships
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductItem productItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DrugBatch drugBatch;

    // Movement details
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    // From location
    @Column(name = "from_company_id")
    private Long fromCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_company_type")
    private ProductItem.OwnerType fromCompanyType;

    @Size(max = 255)
    @Column(name = "from_company_name")
    private String fromCompanyName;

    @Column(name = "from_address_detail", columnDefinition = "TEXT")
    private String fromAddressDetail;

    // To location
    @NotNull
    @Column(name = "to_company_id", nullable = false)
    private Long toCompanyId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_company_type", nullable = false)
    private ProductItem.OwnerType toCompanyType;

    @Size(max = 255)
    @Column(name = "to_company_name")
    private String toCompanyName;

    @Column(name = "to_address_detail", columnDefinition = "TEXT")
    private String toAddressDetail;

    // Related records
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Shipment shipment;

    @Size(max = 100)
    @Column(name = "related_transaction_id")
    private String relatedTransactionId;

    // Tracking
    @NotNull
    @Column(name = "movement_timestamp", nullable = false)
    private LocalDateTime movementTimestamp = LocalDateTime.now();

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;

    // Verification
    @Size(max = 100)
    @Column(name = "verified_by")
    private String verifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method")
    private VerificationMethod verificationMethod = VerificationMethod.AUTO;

    // Blockchain
    @Size(max = 66)
    @Column(name = "blockchain_tx_hash")
    private String blockchainTxHash;

    @Column(name = "blockchain_block_number")
    private Long blockchainBlockNumber;

    @Column(name = "is_blockchain_synced", nullable = false)
    private Boolean isBlockchainSynced = false;

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enums
    public enum MovementType {
        MANUFACTURE,
        TRANSFER,
        SHIP,
        RECEIVE,
        SALE,
        RETURN,
        RECALL,
        DAMAGE,
        EXPIRE
    }

    public enum VerificationMethod {
        QR_SCAN,
        MANUAL,
        AUTO
    }
}

