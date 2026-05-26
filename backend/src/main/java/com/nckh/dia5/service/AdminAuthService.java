package com.nckh.dia5.service;

import com.nckh.dia5.dto.auth.AuthResponse;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.model.UserRole;
import com.nckh.dia5.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(LoginRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());
        
        String jwt;
        String name = "System Administrator";
        String id = "SYSTEM_ADMIN";

        // SIÊU TỐI ƯU: Tài khoản Admin hệ thống mặc định cho demo/development
        if ("admin@pharmachain.com".equals(request.getEmail()) && "admin123".equals(request.getPassword())) {
            log.info("🚀 System Admin login detected");
            jwt = tokenProvider.generateTokenFromUsername(request.getEmail());
        } else {
            // Đăng nhập bình thường qua Database
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Check if user has ADMIN role
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isAdmin) {
                log.warn("User {} tried to login to admin portal but is not an admin", request.getEmail());
                throw new RuntimeException("Tài khoản không có quyền quản trị");
            }

            jwt = tokenProvider.generateToken(authentication);
            
            // Extract info from UserDetails
            try {
                if (userDetails instanceof com.nckh.dia5.model.ManufacturerUser) {
                    name = ((com.nckh.dia5.model.ManufacturerUser) userDetails).getName();
                    id = ((com.nckh.dia5.model.ManufacturerUser) userDetails).getId();
                } else if (userDetails instanceof com.nckh.dia5.model.User) {
                    name = ((com.nckh.dia5.model.User) userDetails).getName();
                    id = ((com.nckh.dia5.model.User) userDetails).getId();
                }
            } catch (Exception e) {
                log.warn("Could not extract detailed user info: {}", e.getMessage());
            }
        }

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(id)
                        .email(request.getEmail())
                        .name(name)
                        .isActive(true)
                        .isProfileComplete(true)
                        .build())
                .build();
    }

    public UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }
}
