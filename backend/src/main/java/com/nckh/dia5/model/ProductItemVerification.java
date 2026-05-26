package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_item_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductItemVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Relationship
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductItem productItem;

    // Scanner information
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scanner_type", nullable = false)
    private ScannerType scannerType;

    @Size(max = 100)
    @Column(name = "scanner_id")
    private String scannerId;

    @Size(max = 255)
    @Column(name = "scanner_name")
    private String scannerName;

    // Device & Location
    @Column(name = "scanner_device_info", columnDefinition = "JSON")
    private String scannerDeviceInfo;

    @Size(max = 500)
    @Column(name = "scanner_location")
    private String scannerLocation;

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;

    // Verification result
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_result", nullable = false)
    private VerificationResult verificationResult = VerificationResult.AUTHENTIC;

    @Column(name = "verification_details", columnDefinition = "JSON")
    private String verificationDetails;

    // Security tracking
    @Size(max = 45)
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @NotNull
    @Column(name = "scan_timestamp", nullable = false)
    private LocalDateTime scanTimestamp = LocalDateTime.now();

    // Blockchain verification
    @Column(name = "blockchain_verified", nullable = false)
    private Boolean blockchainVerified = false;

    @Column(name = "blockchain_query_time_ms")
    private Integer blockchainQueryTimeMs;

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enums
    public enum ScannerType {
        MANUFACTURER,
        DISTRIBUTOR,
        PHARMACY,
        CONSUMER,
        INSPECTOR,
        ANONYMOUS
    }

    public enum VerificationResult {
        AUTHENTIC,
        SUSPICIOUS,
        COUNTERFEIT,
        EXPIRED,
        RECALLED
    }
}

