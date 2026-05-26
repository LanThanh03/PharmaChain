package com.nckh.dia5.controller;

import com.nckh.dia5.dto.auth.ManufacturerAuthResponse;
import com.nckh.dia5.dto.auth.ManufacturerRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.service.ManufacturerAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/manufacturer/auth")
@RequiredArgsConstructor
@Tag(name = "Manufacturer Authentication", description = "Authentication APIs for manufacturers")
public class ManufacturerAuthController {

    private final ManufacturerAuthService manufacturerAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register a new manufacturer", description = "Register a new manufacturer user account")
    public ResponseEntity<ApiResponse<ManufacturerAuthResponse>> register(@Valid @RequestBody ManufacturerRegisterRequest request) {
        try {
            log.info("Registering new manufacturer user: {}", request.getEmail());
            ManufacturerAuthResponse response = manufacturerAuthService.register(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký nhà sản xuất thành công"));
        } catch (Exception e) {
            log.error("Manufacturer registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng ký thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login manufacturer", description = "Authenticate manufacturer user")
    public ResponseEntity<ApiResponse<ManufacturerAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Manufacturer user login attempt: {}", request.getEmail());
            ManufacturerAuthResponse response = manufacturerAuthService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập nhà sản xuất thành công"));
        } catch (Exception e) {
            log.error("Manufacturer login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng nhập thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout manufacturer", description = "Logout manufacturer user (client-side token removal)")
    public ResponseEntity<ApiResponse<String>> logout() {
        // Since we're using stateless JWT, logout is handled on client side
        // by removing the token from storage
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current manufacturer user", description = "Get current authenticated manufacturer user information")
    public ResponseEntity<ApiResponse<ManufacturerAuthResponse.UserInfo>> getCurrentUser() {
        try {
            ManufacturerUser currentUser = manufacturerAuthService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Người dùng chưa được xác thực", 401));
            }

            ManufacturerAuthResponse.UserInfo userInfo = ManufacturerAuthResponse.UserInfo.builder()
                    .id(currentUser.getId())
                    .email(currentUser.getEmail())
                    .name(currentUser.getName())
                    .role(currentUser.getRole())
                    .companyName(currentUser.getCompanyName())
                    .companyAddress(currentUser.getCompanyAddress())
                    .walletAddress(currentUser.getWalletAddress())
                    .licenseNumber(currentUser.getLicenseNumber())
                    .isVerified(currentUser.getIsVerified())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(userInfo, "Lấy thông tin người dùng thành công"));
        } catch (Exception e) {
            log.error("Failed to get current manufacturer user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy thông tin người dùng: " + e.getMessage(), 400));
        }
    }
}
