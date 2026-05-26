package com.nckh.dia5.controller;

import com.nckh.dia5.dto.auth.PharmacyAuthResponse;
import com.nckh.dia5.dto.auth.PharmacyRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.service.PharmacyAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pharmacy/auth")
@RequiredArgsConstructor
@Tag(name = "Pharmacy Authentication", description = "Authentication APIs for pharmacies")
public class PharmacyAuthController {

    private final PharmacyAuthService pharmacyAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register a new pharmacy", description = "Register a new pharmacy user account")
    public ResponseEntity<ApiResponse<PharmacyAuthResponse>> register(
            @Valid @RequestBody PharmacyRegisterRequest request) {
        try {
            log.info("Pharmacy registration request: {} - {}", request.getEmail(), request.getPharmacyName());
            PharmacyAuthResponse response = pharmacyAuthService.register(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký hiệu thuốc thành công"));
        } catch (Exception e) {
            log.error("Pharmacy registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng ký thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login pharmacy", description = "Authenticate pharmacy user")
    public ResponseEntity<ApiResponse<PharmacyAuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            log.info("Pharmacy login request: {}", request.getEmail());
            PharmacyAuthResponse response = pharmacyAuthService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập hiệu thuốc thành công"));
        } catch (Exception e) {
            log.error("Pharmacy login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Đăng nhập thất bại: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout pharmacy", description = "Logout pharmacy user")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current pharmacy", description = "Get current authenticated pharmacy user")
    public ResponseEntity<ApiResponse<PharmacyAuthResponse.PharmacyUserInfo>> getCurrentPharmacy() {
        try {
            PharmacyAuthResponse.PharmacyUserInfo pharmacy = pharmacyAuthService.getCurrentPharmacyInfo();
            return ResponseEntity.ok(ApiResponse.success(pharmacy, "Lấy thông tin hiệu thuốc thành công"));
        } catch (Exception e) {
            log.error("Get current pharmacy failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể lấy thông tin: " + e.getMessage(), 400));
        }
    }
}
