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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_material_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RawMaterialBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "batch_number", unique = true, nullable = false)
    private String batchNumber;

    // Kept for DB schema compatibility (legacy column from previous entity version)
    @Column(name = "manufacturer_id")
    private Long manufacturerId;


    @NotNull
    @Size(max = 255)
    @Column(name = "material_name", nullable = false)
    private String materialName;

    @NotNull
    @Size(max = 255)
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @NotNull
    @Size(max = 50)
    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Size(max = 500)
    @Column(name = "coa_document_url")
    private String coaDocumentUrl;

    // Status: PENDING, APPROVED, REJECTED
    @NotNull
    @Size(max = 50)
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "quality_control_notes", columnDefinition = "TEXT")
    private String qualityControlNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
