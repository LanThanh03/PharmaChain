package com.nckh.dia5.dto.user;

import com.nckh.dia5.model.UserDemographic;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 255, message = "Tên không được quá 255 ký tự")
    private String name;

    @Min(value = 1900, message = "Năm sinh không hợp lệ")
    @Max(value = 2024, message = "Năm sinh không hợp lệ")
    private Integer birthYear;

    @Min(value = 1, message = "Tháng sinh phải từ 1-12")
    @Max(value = 12, message = "Tháng sinh phải từ 1-12")
    private Integer birthMonth;

    private UserDemographic.Gender gender;

    @Min(value = 50, message = "Chiều cao phải lớn hơn 50cm")
    @Max(value = 250, message = "Chiều cao phải nhỏ hơn 250cm")
    private Integer heightCm;

    @Min(value = 10, message = "Cân nặng phải lớn hơn 10kg")
    @Max(value = 500, message = "Cân nặng phải nhỏ hơn 500kg")
    private BigDecimal weightKg;

    private UserDemographic.BloodType bloodType;

    private Integer provinceId;

    @Size(max = 100, message = "Nghề nghiệp không được quá 100 ký tự")
    private String occupation;

    private UserDemographic.EducationLevel educationLevel;

    // Lifestyle and health information
    @Size(max = 1000, message = "Tiền sử bệnh không được quá 1000 ký tự")
    private String medicalHistory;

    @Size(max = 500, message = "Thông tin dị ứng không được quá 500 ký tự")
    private String allergies;

    @Size(max = 500, message = "Thông tin thuốc không được quá 500 ký tự")
    private String currentMedications;

    @Size(max = 20, message = "Trạng thái hút thuốc không hợp lệ")
    private String smokingStatus;

    @Size(max = 20, message = "Trạng thái uống rượu không hợp lệ")
    private String drinkingStatus;
}
