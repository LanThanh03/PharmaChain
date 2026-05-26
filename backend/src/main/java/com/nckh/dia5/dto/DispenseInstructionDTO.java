package com.nckh.dia5.dto;

import com.nckh.dia5.model.DispenseInstruction;
import com.nckh.dia5.model.MedicationReminder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for dispense instruction response (returned to mobile app when scanning QR)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseInstructionDTO {

    private Long id;

    // Drug info
    private String drugName;
    private String batchNumber;
    private String itemCode;
    private String manufacturer;
    private LocalDate expiryDate;

    // Customer info (masked for privacy)
    private String customerName;
    private String customerPhoneMasked; // Show only last 4 digits

    // Usage instructions
    private String dosage;
    private Integer frequency;
    private String mealRelation;
    private String mealRelationDisplay;
    private List<String> specificTimes;
    private Integer durationDays;
    private String specialNotes;

    // Warnings
    private List<String> warnings;
    private List<String> contraindications;

    // Pharmacy info
    private String pharmacyName;
    private String pharmacistName;

    // Sale info
    private BigDecimal salePrice;
    private LocalDateTime dispensedAt;

    // Status flags
    private boolean canAddToProfile;
    private boolean alreadyInProfile;

    // Static factory method from entity
    public static DispenseInstructionDTO fromEntity(DispenseInstruction entity) {
        if (entity == null) return null;

        return DispenseInstructionDTO.builder()
                .id(entity.getId())
                .drugName(entity.getDrugName())
                .batchNumber(entity.getBatchNumber())
                .itemCode(entity.getItemCode())
                .dosage(entity.getDosage())
                .frequency(entity.getFrequency())
                .mealRelation(entity.getMealRelation() != null ? entity.getMealRelation().name() : null)
                .mealRelationDisplay(entity.getMealRelationDisplay())
                .specificTimes(entity.getSpecificTimes() != null ?
                        List.of(entity.getSpecificTimes().split(",")) : List.of())
                .durationDays(entity.getDurationDays())
                .specialNotes(entity.getSpecialNotes())
                .pharmacistName(entity.getPharmacistName())
                .salePrice(entity.getSalePrice())
                .dispensedAt(entity.getDispensedAt())
                .customerName(entity.getCustomerName())
                .customerPhoneMasked(maskPhone(entity.getCustomerPhone()))
                .canAddToProfile(true)
                .build();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}

/**
 * DTO for medication record in mobile app
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MedicationRecordDTO {

    private Long id;

    // Drug info
    private String drugName;
    private String batchNumber;
    private String itemCode;
    private String manufacturer;
    private LocalDate expiryDate;

    // Schedule
    private String dosage;
    private Integer frequency;
    private String mealRelation;
    private List<String> reminderTimes;
    private LocalDate startDate;
    private LocalDate endDate;

    // Progress
    private Integer totalDoses;
    private Integer takenDoses;
    private Integer missedDoses;
    private Integer remainingDoses;
    private Double adherenceRate;
    private Integer daysRemaining;

    // Status
    private boolean isActive;
    private boolean isCompleted;
    private boolean isPaused;
    private boolean isExpiringSoon;

    // Source
    private String pharmacyName;
    private LocalDateTime purchasedAt;

    // Today's reminders
    private List<ReminderDTO> todayReminders;
}

/**
 * DTO for a single reminder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ReminderDTO {

    private Long id;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String status;
    private String statusDisplay;
    private LocalDateTime takenAt;
    private String notes;

    // From parent medication
    private String drugName;
    private String dosage;

    public static ReminderDTO fromEntity(MedicationReminder entity, String drugName, String dosage) {
        if (entity == null) return null;

        return ReminderDTO.builder()
                .id(entity.getId())
                .scheduledDate(entity.getScheduledDate())
                .scheduledTime(entity.getScheduledTime())
                .status(entity.getStatus().name())
                .statusDisplay(entity.getStatusDisplay())
                .takenAt(entity.getTakenAt())
                .notes(entity.getNotes())
                .drugName(drugName)
                .dosage(dosage)
                .build();
    }
}
