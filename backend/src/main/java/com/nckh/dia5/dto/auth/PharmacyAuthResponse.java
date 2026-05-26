package com.nckh.dia5.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyAuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private PharmacyUserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacyUserInfo {
        private Long id;
        private String email;
        private String pharmacyName;
        private String pharmacyCode;
        private String walletAddress;
        private String address;
        private String phone;
        private Boolean isActive;
        private Boolean isProfileComplete;
    }
}
