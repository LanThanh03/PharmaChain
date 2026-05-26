package com.nckh.dia5.dto.auth;

import com.nckh.dia5.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturerRegisterRequest {

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    private String confirmPassword;

    @NotBlank(message = "Tên là bắt buộc")
    @Size(max = 255, message = "Tên không được quá 255 ký tự")
    private String name;

    @NotNull(message = "Vai trò là bắt buộc")
    private UserRole role;

    @NotBlank(message = "Tên công ty là bắt buộc")
    @Size(max = 255, message = "Tên công ty không được quá 255 ký tự")
    private String companyName;

    @Size(max = 500, message = "Địa chỉ công ty không được quá 500 ký tự")
    private String companyAddress;

    @Size(max = 42, message = "Địa chỉ ví không được quá 42 ký tự")
    private String walletAddress;

    @Size(max = 50, message = "Số giấy phép không được quá 50 ký tự")
    private String licenseNumber;
}
