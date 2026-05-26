package com.nckh.dia5.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PharmacyRegisterRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Tên hiệu thuốc không được để trống")
    private String pharmacyName;

    @NotBlank(message = "Địa chỉ ví không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ ví phải có đúng 42 ký tự")
    private String walletAddress;

    private String address;
    private String phone;
}
