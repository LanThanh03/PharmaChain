package com.nckh.dia5.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "medications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 200)
    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "brand_names", columnDefinition = "TEXT")
    private String brandNames; // JSON array

    @Size(max = 100)
    @Column(name = "drug_class")
    private String drugClass;

    @Column(name = "dosage_forms", columnDefinition = "TEXT")
    private String dosageForms; // JSON array

    @Column(name = "common_dosages", columnDefinition = "TEXT")
    private String commonDosages; // JSON array

    @Column(name = "contraindications", columnDefinition = "TEXT")
    private String contraindications; // JSON array

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects; // JSON array

    @Column(name = "interactions", columnDefinition = "TEXT")
    private String interactions; // JSON array

    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_category")
    private PregnancyCategory pregnancyCategory;

    @Column(name = "requires_prescription", nullable = false)
    private Boolean requiresPrescription = true;

    @Column(name = "is_controlled_substance", nullable = false)
    private Boolean isControlledSubstance = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PregnancyCategory {
        A, B, C, D, X, unknown
    }
}
