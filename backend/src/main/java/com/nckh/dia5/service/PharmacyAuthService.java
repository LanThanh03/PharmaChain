package com.nckh.dia5.service;

import com.nckh.dia5.dto.auth.PharmacyAuthResponse;
import com.nckh.dia5.dto.auth.PharmacyRegisterRequest;
import com.nckh.dia5.dto.auth.LoginRequest;
import com.nckh.dia5.model.PharmacyUser;
import com.nckh.dia5.repository.PharmacyUserRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyAuthService {

    private final PharmacyUserRepository pharmacyUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public PharmacyAuthResponse register(PharmacyRegisterRequest request) {
        // Check if email already exists
        if (pharmacyUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Create new pharmacy user
        PharmacyUser pharmacyUser = new PharmacyUser();
        pharmacyUser.setEmail(request.getEmail());
        pharmacyUser.setPassword(passwordEncoder.encode(request.getPassword()));
        pharmacyUser.setPharmacyName(request.getPharmacyName());
        pharmacyUser.setPharmacyCode(generatePharmacyCode());
        pharmacyUser.setWalletAddress(request.getWalletAddress());
        pharmacyUser.setAddress(request.getAddress());
        pharmacyUser.setPhone(request.getPhone());
        pharmacyUser.setIsActive(true);
        pharmacyUser.setIsProfileComplete(false);
        pharmacyUser.setCreatedAt(LocalDateTime.now());

        PharmacyUser savedUser = pharmacyUserRepository.save(pharmacyUser);

        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String jwt = tokenProvider.generateToken(authentication);

        log.info("New pharmacy user registered: {} - {}", savedUser.getEmail(), savedUser.getPharmacyName());

        return PharmacyAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(savedUser))
                .build();
    }

    @Transactional
    public PharmacyAuthResponse login(LoginRequest request) {
        // Find pharmacy user
        PharmacyUser user = pharmacyUserRepository.findByEmail(request.getEmail())
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
        pharmacyUserRepository.save(user);

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        log.info("Pharmacy user logged in: {} - {}", user.getEmail(), user.getPharmacyName());

        return PharmacyAuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(mapToUserInfo(user))
                .build();
    }

    public PharmacyUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return pharmacyUserRepository.findByEmail(email).orElse(null);
    }

    public PharmacyAuthResponse.PharmacyUserInfo getCurrentPharmacyInfo() {
        PharmacyUser user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng");
        }
        return mapToUserInfo(user);
    }

    private PharmacyAuthResponse.PharmacyUserInfo mapToUserInfo(PharmacyUser user) {
        return PharmacyAuthResponse.PharmacyUserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .pharmacyName(user.getPharmacyName())
                .pharmacyCode(user.getPharmacyCode())
                .walletAddress(user.getWalletAddress())
                .address(user.getAddress())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .isProfileComplete(user.getIsProfileComplete())
                .build();
    }

    private String generatePharmacyCode() {
        // Generate unique pharmacy code (HT-YYYY-NNN)
        long count = pharmacyUserRepository.count();
        int year = LocalDateTime.now().getYear();
        return String.format("HT-%d-%03d", year, count + 1);
    }
}
