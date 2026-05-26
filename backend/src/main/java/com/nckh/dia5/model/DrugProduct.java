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

import java.time.LocalDateTime;

@Entity
@Table(name = "drug_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DrugProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "active_ingredient")
    private String activeIngredient;

    @Size(max = 100)
    @Column(name = "dosage")
    private String dosage;

    @Size(max = 50)
    @Column(name = "unit")
    private String unit;

    @Size(max = 100)
    @Column(name = "category")
    private String category;

    @Size(max = 500)
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 500)
    @Column(name = "article_url")
    private String articleUrl;

    // === Chi tiết y khoa cho AI Chatbot ===
    @Column(name = "indications", columnDefinition = "TEXT")
    private String indications;

    @Column(name = "contraindications", columnDefinition = "TEXT")
    private String contraindications;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions;

    @Column(name = "drug_interactions", columnDefinition = "TEXT")
    private String drugInteractions;

    @Column(name = "usage_instructions", columnDefinition = "TEXT")
    private String usageInstructions;

    @Size(max = 500)
    @Column(name = "storage_conditions")
    private String storageConditions;

    @Size(max = 100)
    @Column(name = "shelf_life")
    private String shelfLife;

    @Size(max = 50)
    @Column(name = "status")
    private String status = "active";

    // Ownership: which manufacturer owns this product
    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Long totalBatches = 0L;

    @Transient
    private Long totalProduced = 0L;
}


