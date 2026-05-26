package com.nckh.dia5.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "drug_shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "shipment_code", nullable = false, unique = true, length = 100)
    private String shipmentCode;
    
    // Keep shipment_id for blockchain compatibility (can be stored in notes or tracking_info)
    @Transient
    private BigInteger shipmentId;

    // Blockchain addresses (stored in tracking_info or notes)
    @Transient
    private String fromAddress;
    
    @Transient
    private String toAddress;
    
    // Company relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_company_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany fromCompany;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_company_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PharmaCompany toCompany;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "shipment_date")
    private LocalDateTime shipmentDate;
    
    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status")
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Size(max = 66)
    @Column(name = "create_tx_hash", length = 66)
    private String createTxHash;
    
    @Size(max = 66)
    @Column(name = "receive_tx_hash", length = 66)
    private String receiveTxHash;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Store blockchain data in notes field as JSON
    @Transient
    private String trackingInfo;
    
    @Transient
    private BigInteger blockNumber;
    
    @Transient
    private Boolean isSynced = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-one relationship with drug batch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "shipments", "transactions"})
    private DrugBatch drugBatch;

    // One-to-many relationship with blockchain transactions
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private List<BlockchainTransaction> transactions;

    public enum ShipmentStatus {
        PENDING,
        IN_TRANSIT,
        DELIVERED,
        CANCELLED
    }
}
