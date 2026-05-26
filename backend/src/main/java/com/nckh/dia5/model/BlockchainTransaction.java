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

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "blockchain_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BlockchainTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 66)
    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @NotNull
    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;

    @NotNull
    @Size(max = 42)
    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @NotNull
    @Size(max = 42)
    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;

    @NotNull
    @Size(max = 100)
    @Column(name = "function_name", nullable = false)
    private String functionName;

    @Column(name = "gas_used")
    private BigInteger gasUsed;

    @Column(name = "gas_price")
    private BigInteger gasPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Lob
    @Column(name = "input_data")
    private String inputData;

    @Lob
    @Column(name = "event_logs")
    private String eventLogs;

    @Size(max = 1000)
    @Column(name = "error_message")
    private String errorMessage;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Many-to-one relationship with drug batch (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DrugBatch drugBatch;

    // Many-to-one relationship with shipment (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Shipment shipment;

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED,
        REVERTED
    }
}
