package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Entity để lưu trữ blockchain events
 */
@Entity
@Table(name = "blockchain_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BlockchainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "contract_address", nullable = false)
    private String contractAddress;

    @Column(name = "transaction_hash", nullable = false, unique = true)
    private String transactionHash;

    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;

    @Column(name = "log_index", nullable = false)
    private BigInteger logIndex;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "batch_id")
    private BigInteger batchId;

    @Column(name = "shipment_id")
    private BigInteger shipmentId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "indexed_at", nullable = false)
    private LocalDateTime indexedAt;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @PrePersist
    protected void onCreate() {
        this.indexedAt = LocalDateTime.now();
    }
}
