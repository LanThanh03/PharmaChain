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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for user's medication records in the mobile app
 * Tracks medications that users are taking, including schedule and adherence
 */
@Entity
@Table(name = "user_medication_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserMedicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to app user
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Link to purchased drug
    @Column(name = "product_item_id")
    private Long productItemId;

    @Column(name = "dispense_instruction_id")
    private Long dispenseInstructionId;

    // Drug info (cached for quick display)
    @NotNull
    @Size(max = 255)
    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Size(max = 50)
    @Column(name = "batch_number")
    private String batchNumber;

    @Size(max = 100)
    @Column(name = "item_code")
    private String itemCode;

    @Size(max = 255)
    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Schedule (copied from dispense_instructions or user-entered)
    @Size(max = 50)
    @Column(name = "dosage")
    private String dosage = "1 viên";

    @Column(name = "frequency")
    private Integer frequency = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_relation")
    private DispenseInstruction.MealRelation mealRelation = DispenseInstruction.MealRelation.AFTER;

    @Size(max = 100)
    @Column(name = "reminder_times")
    private String reminderTimes = "08:00,12:00,20:00";

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Progress tracking
    @Column(name = "total_doses")
    private Integer totalDoses;

    @Column(name = "taken_doses")
    private Integer takenDoses = 0;

    @Column(name = "missed_doses")
    private Integer missedDoses = 0;

    // Note: adherence_rate is a generated column in DB, so we make it read-only
    @Column(name = "adherence_rate", insertable = false, updatable = false)
    private Double adherenceRate;

    // Status flags
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "is_paused")
    private Boolean isPaused = false;

    @Size(max = 255)
    @Column(name = "pause_reason")
    private String pauseReason;

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    // Source info
    @Size(max = 100)
    @Column(name = "pharmacy_name")
    private String pharmacyName;

    @Column(name = "pharmacy_id")
    private Long pharmacyId;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    // Note: Removed @ManyToOne to AppUser to avoid FK constraint issues.
    // userId is stored as a plain column. User lookup is done at application level.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispense_instruction_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DispenseInstruction dispenseInstruction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_item_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductItem productItem;

    @OneToMany(mappedBy = "medicationRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "medicationRecord"})
    private List<MedicationReminder> reminders;

    // Helper methods
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        return expiryDate != null && LocalDate.now().plusDays(daysThreshold).isAfter(expiryDate);
    }

    public int getRemainingDoses() {
        if (totalDoses == null || takenDoses == null) return 0;
        return Math.max(0, totalDoses - takenDoses);
    }

    public int getDaysRemaining() {
        if (endDate == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return (int) Math.max(0, days);
    }

    public String[] getReminderTimesArray() {
        if (reminderTimes == null || reminderTimes.isEmpty()) {
            return new String[0];
        }
        return reminderTimes.split(",");
    }
}
