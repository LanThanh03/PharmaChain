package com.nckh.dia5.controller;

import com.nckh.dia5.dto.auth.DistributorAuthResponse;
import com.nckh.dia5.dto.auth.DistributorRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.service.DistributorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/distributor/auth")
@RequiredArgsConstructor
@Tag(name = "Distributor Authentication", description = "Authentication APIs for distributors")
public class DistributorAuthController {

    private final DistributorAuthService distributorAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register a new distributor", description = "Register a new distributor user account")
    public ResponseEntity<ApiResponse<DistributorAuthResponse>> register(
            @Valid @RequestBody DistributorRegisterRequest request) {
        try {
            log.info("Distributor registration request: {} - {}", request.getEmail(), request.getCompanyName());
            DistributorAuthResponse response = distributorAuthService.register(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký nhà phân phối thành công"));
        } catch (Exception e) {
            log.error("Distributor registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng ký thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login distributor", description = "Authenticate distributor user")
    public ResponseEntity<ApiResponse<DistributorAuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            log.info("Distributor login request: {}", request.getEmail());
            DistributorAuthResponse response = distributorAuthService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập nhà phân phối thành công"));
        } catch (Exception e) {
            log.error("Distributor login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng nhập thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout distributor", description = "Logout distributor user (client-side token removal)")
    public ResponseEntity<ApiResponse<String>> logout() {
        // Since we're using stateless JWT, logout is handled on client side
        // by removing the token from storage
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current distributor user", description = "Get current authenticated distributor user information")
    public ResponseEntity<ApiResponse<DistributorAuthResponse.DistributorUserInfo>> getCurrentUser() {
        try {
            DistributorUser currentUser = distributorAuthService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Người dùng chưa đăng nhập", 401));
            }

            DistributorAuthResponse.DistributorUserInfo userInfo = 
                    DistributorAuthResponse.DistributorUserInfo.builder()
                            .id(currentUser.getId())
                            .email(currentUser.getEmail())
                            .name(currentUser.getName())
                            .role(currentUser.getRole())
                            .walletAddress(currentUser.getWalletAddress())
                            .companyName(currentUser.getCompanyName())
                            .companyAddress(currentUser.getCompanyAddress())
                            .phoneNumber(currentUser.getPhoneNumber())
                            .licenseNumber(currentUser.getLicenseNumber())
                            .isActive(currentUser.getIsActive())
                            .isVerified(currentUser.getIsVerified())
                            .isProfileComplete(currentUser.getIsProfileComplete())
                            .build();

            return ResponseEntity.ok(ApiResponse.success(userInfo, "Lấy thông tin người dùng thành công"));
        } catch (Exception e) {
            log.error("Failed to get current user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy thông tin người dùng: " + e.getMessage(), 500));
        }
    }

    @PutMapping("/verify/{userId}")
    @Operation(summary = "Verify distributor user", description = "Verify distributor user account (Admin only)")
    public ResponseEntity<ApiResponse<String>> verifyUser(@PathVariable String userId) {
        // This endpoint would be used by admin to verify distributor accounts
        // Implementation would depend on admin authentication
        return ResponseEntity.ok(ApiResponse.success(null, "Tính năng đang phát triển"));
    }
}

