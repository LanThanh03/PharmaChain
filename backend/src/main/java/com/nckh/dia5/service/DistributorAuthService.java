package com.nckh.dia5.service;

import com.nckh.dia5.dto.auth.DistributorAuthResponse;
import com.nckh.dia5.dto.auth.DistributorRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.repository.DistributorUserRepository;
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
public class DistributorAuthService {

    private final AuthenticationManager authenticationManager;
    private final DistributorUserRepository distributorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public DistributorAuthResponse register(DistributorRegisterRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // Check if user already exists
        if (distributorUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Check if wallet address already exists (if provided)
        if (request.getWalletAddress() != null && !request.getWalletAddress().isEmpty()) {
            if (distributorUserRepository.existsByWalletAddress(request.getWalletAddress())) {
                throw new RuntimeException("Địa chỉ ví đã được sử dụng");
            }
        }

        // Create new distributor user
        DistributorUser user = DistributorUser.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .companyName(request.getCompanyName())
                .companyAddress(request.getCompanyAddress())
                .phoneNumber(request.getPhoneNumber())
                .walletAddress(request.getWalletAddress())
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .isActive(true)
                .isVerified(false) // Requires admin verification
                .isProfileComplete(true) // Profile is complete upon registration
                .build();

        DistributorUser savedUser = distributorUserRepository.save(user);

        log.info("New distributor user registered: {} - {}", savedUser.getEmail(), savedUser.getCompanyName());

        // Create custom authentication token for distributor
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(), 
                null, 
                savedUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        return DistributorAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(savedUser))
                .build();
    }

    @Transactional
    public DistributorAuthResponse login(LoginRequest request) {
        // Find distributor user
        DistributorUser user = distributorUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Check if user is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        distributorUserRepository.save(user);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        log.info("Distributor user logged in: {} - {}", user.getEmail(), user.getCompanyName());

        return DistributorAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(user))
                .build();
    }

    public DistributorUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return distributorUserRepository.findByEmail(email).orElse(null);
    }

    private DistributorAuthResponse.DistributorUserInfo mapToUserInfo(DistributorUser user) {
        return DistributorAuthResponse.DistributorUserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .walletAddress(user.getWalletAddress())
                .companyName(user.getCompanyName())
                .companyAddress(user.getCompanyAddress())
                .phoneNumber(user.getPhoneNumber())
                .licenseNumber(user.getLicenseNumber())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .isProfileComplete(user.getIsProfileComplete())
                .build();
    }
}

