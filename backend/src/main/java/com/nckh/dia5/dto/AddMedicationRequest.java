package com.nckh.dia5.dto;

import com.nckh.dia5.model.DispenseInstruction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for adding a medication to user's profile (from mobile app)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMedicationRequest {

    // Required
    private Long userId;

    // Either dispense instruction ID (from QR scan) or manual entry
    private Long dispenseInstructionId;

    // Or item code (from QR scan)
    private String itemCode;

    // Manual entry fields (used if no dispenseInstructionId)
    private String drugName;
    private String manufacturer;
    private LocalDate expiryDate;

    // Schedule (can override dispense instruction)
    private String dosage;
    private Integer frequency;
    private String mealRelation;
    private String reminderTimes;      // "08:00,12:00,20:00"
    private LocalDate startDate;
    private LocalDate endDate;

    // Notes
    private String notes;

    public boolean isFromDispenseInstruction() {
        return dispenseInstructionId != null || (itemCode != null && !itemCode.isBlank());
    }

    public boolean isManualEntry() {
        return drugName != null && !drugName.isBlank();
    }

    public DispenseInstruction.MealRelation getMealRelationEnum() {
        if (mealRelation == null || mealRelation.isEmpty()) {
            return DispenseInstruction.MealRelation.AFTER;
        }
        try {
            return DispenseInstruction.MealRelation.valueOf(mealRelation.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DispenseInstruction.MealRelation.AFTER;
        }
    }
}

/**
 * DTO for updating reminder status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UpdateReminderRequest {
    private Long reminderId;
    private String status;      // "TAKEN", "SKIPPED"
    private String notes;
    private String skipReason;
}

/**
 * DTO for today's summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TodaySummaryDTO {
    private int totalReminders;
    private int takenCount;
    private int pendingCount;
    private int missedCount;
    private double adherencePercentage;
    private List<ReminderDTO> upcomingReminders;
    private List<MedicationRecordDTO> activeMedications;
}
