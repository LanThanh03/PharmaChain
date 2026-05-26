package com.nckh.dia5.dto.user;

import com.nckh.dia5.model.UserDemographic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String email;
    private String name;
    private boolean isProfileComplete;

    // Demographics
    private Integer birthYear;
    private Integer birthMonth;
    private UserDemographic.Gender gender;
    private Integer heightCm;
    private BigDecimal weightKg;
    private UserDemographic.BloodType bloodType;
    private String provinceName;
    private String occupation;
    private UserDemographic.EducationLevel educationLevel;

    // Health summary
    private Integer chronicDiseaseCount;
    private Integer activeMedicationCount;
    private Integer allergyCount;
}
