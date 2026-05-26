package com.nckh.dia5.controller;

import com.nckh.dia5.dto.auth.AuthResponse;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Authentication APIs for Admin Portal")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "Login admin", description = "Authenticate admin user")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = adminAuthService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập admin thành công"));
        } catch (Exception e) {
            log.error("Admin login failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Đăng nhập thất bại: " + e.getMessage(), 401));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current admin", description = "Get current authenticated admin information")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        try {
            UserDetails currentUser = adminAuthService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Chưa đăng nhập", 401));
            }

            // Map to UserInfo DTO
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .email(currentUser.getUsername())
                    .name("Admin System")
                    .isActive(true)
                    .build();
            
            // Try to extract name if possible
            if (currentUser instanceof com.nckh.dia5.model.ManufacturerUser) {
                userInfo.setName(((com.nckh.dia5.model.ManufacturerUser) currentUser).getName());
                userInfo.setId(((com.nckh.dia5.model.ManufacturerUser) currentUser).getId());
            }

            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }
}
