package com.nckh.dia5.service;

import com.nckh.dia5.dto.auth.ManufacturerAuthResponse;
import com.nckh.dia5.dto.auth.ManufacturerRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.repository.ManufacturerUserRepository;
import com.nckh.dia5.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturerAuthService {

    private final AuthenticationManager authenticationManager;
    private final ManufacturerUserRepository manufacturerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public ManufacturerAuthResponse register(ManufacturerRegisterRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // Check if user already exists
        if (manufacturerUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Check if wallet address already exists (if provided)
        if (request.getWalletAddress() != null && !request.getWalletAddress().isEmpty()) {
            if (manufacturerUserRepository.existsByWalletAddress(request.getWalletAddress())) {
                throw new RuntimeException("Địa chỉ ví đã được sử dụng");
            }
        }

        // Create new manufacturer user
        ManufacturerUser manufacturerUser = ManufacturerUser.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .companyName(request.getCompanyName())
                .companyAddress(request.getCompanyAddress())
                .walletAddress(request.getWalletAddress())
                .licenseNumber(request.getLicenseNumber())
                .isVerified(false)
                .build();

        ManufacturerUser savedUser = manufacturerUserRepository.save(manufacturerUser);

        log.info("New manufacturer user registered: {} - {}", savedUser.getEmail(), savedUser.getCompanyName());

        // Create custom authentication token for manufacturer
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(), 
                null, 
                savedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        return ManufacturerAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(savedUser))
                .build();
    }

    @Transactional
    public ManufacturerAuthResponse login(LoginRequest request) {
        // Find manufacturer user
        ManufacturerUser user = manufacturerUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Check if user is verified (optional business rule)
        if (!user.getIsVerified()) {
            log.warn("Unverified manufacturer user attempting login: {}", user.getEmail());
            // You can uncomment the line below to enforce verification
            // throw new RuntimeException("Tài khoản chưa được xác minh");
        }

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        manufacturerUserRepository.save(user);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        log.info("Manufacturer user logged in: {} - {}", user.getEmail(), user.getCompanyName());

        return ManufacturerAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(user))
                .build();
    }

    public ManufacturerUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return manufacturerUserRepository.findByEmail(email).orElse(null);
    }

    private ManufacturerAuthResponse.UserInfo mapToUserInfo(ManufacturerUser user) {
        return ManufacturerAuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .companyName(user.getCompanyName())
                .companyAddress(user.getCompanyAddress())
                .walletAddress(user.getWalletAddress())
                .licenseNumber(user.getLicenseNumber())
                .isVerified(user.getIsVerified())
                .build();
    }
}
