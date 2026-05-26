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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for storing medication usage instructions when pharmacy dispenses (sells) a drug
 * This is created when the pharmacist sells a product and provides usage guidance to the customer
 */
@Entity
@Table(name = "dispense_instructions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DispenseInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the sale movement
    @Column(name = "movement_id")
    private Long movementId;

    @NotNull
    @Column(name = "product_item_id", nullable = false)
    private Long productItemId;

    @NotNull
    @Column(name = "pharmacy_id", nullable = false)
    private Long pharmacyId;

    // Customer identification
    @Size(max = 100)
    @Column(name = "customer_name")
    private String customerName;

    @Size(max = 20)
    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_app_user_id")
    private Long customerAppUserId;

    // Drug info (cached for quick access)
    @NotNull
    @Size(max = 255)
    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Size(max = 100)
    @Column(name = "batch_number")
    private String batchNumber;

    @Size(max = 100)
    @Column(name = "item_code")
    private String itemCode;

    // Usage instructions
    @Size(max = 50)
    @Column(name = "dosage")
    private String dosage = "1 viên";

    @Column(name = "frequency")
    private Integer frequency = 3; // times per day

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_relation")
    private MealRelation mealRelation = MealRelation.AFTER;

    @Size(max = 100)
    @Column(name = "specific_times")
    private String specificTimes = "08:00,12:00,20:00";

    @Column(name = "duration_days")
    private Integer durationDays = 7;

    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    // Warnings
    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings; // JSON array

    @Column(name = "contraindications", columnDefinition = "TEXT")
    private String contraindications; // JSON array

    // Pharmacist info
    @Size(max = 100)
    @Column(name = "pharmacist_name")
    private String pharmacistName;

    @Size(max = 50)
    @Column(name = "pharmacist_license")
    private String pharmacistLicense;

    // Sale info
    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships (optional - for JPA navigation)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_item_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "movements", "verifications"})
    private ProductItem productItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_app_user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "medicationRecords"})
    private AppUser customerAppUser;

    // Enums
    public enum MealRelation {
        BEFORE,  // Trước bữa ăn
        AFTER,   // Sau bữa ăn
        WITH,    // Trong bữa ăn
        ANY      // Bất kỳ lúc nào
    }

    // Helper methods
    public String getMealRelationDisplay() {
        return switch (mealRelation) {
            case BEFORE -> "Trước bữa ăn";
            case AFTER -> "Sau bữa ăn";
            case WITH -> "Trong bữa ăn";
            case ANY -> "Bất kỳ lúc nào";
        };
    }

    public String[] getSpecificTimesArray() {
        if (specificTimes == null || specificTimes.isEmpty()) {
            return new String[0];
        }
        return specificTimes.split(",");
    }
}
