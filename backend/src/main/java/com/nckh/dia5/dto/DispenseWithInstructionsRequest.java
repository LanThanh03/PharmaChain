package com.nckh.dia5.dto;

import com.nckh.dia5.model.DispenseInstruction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for dispense request with instructions (when pharmacy sells an item)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispenseWithInstructionsRequest {

    // Item being sold
    private String itemCode;

    // Customer info
    private String customerName;
    private String customerPhone;

    // Usage instructions
    private String dosage;           // "1 viên", "5ml", etc.
    private Integer frequency;       // times per day (1, 2, 3, 4)
    private String mealRelation;     // "BEFORE", "AFTER", "WITH", "ANY"
    private String specificTimes;    // "08:00,12:00,20:00"
    private Integer durationDays;    // number of days
    private String specialNotes;     // special notes from pharmacist

    // Optional warnings
    private String warnings;         // JSON array string
    private String contraindications; // JSON array string

    // Pharmacist info
    private String pharmacistName;
    private String pharmacistLicense;

    // Sale info
    private BigDecimal salePrice;

    // Convert meal relation string to enum
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

    // Validate the request
    public boolean isValid() {
        return itemCode != null && !itemCode.isBlank();
    }

    // Check if customer info is provided
    public boolean hasCustomerInfo() {
        return (customerPhone != null && !customerPhone.isBlank()) ||
               (customerName != null && !customerName.isBlank());
    }

    // Check if instructions are provided
    public boolean hasInstructions() {
        return dosage != null || frequency != null || durationDays != null;
    }
}
