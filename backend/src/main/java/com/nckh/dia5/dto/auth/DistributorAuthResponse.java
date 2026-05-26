package com.nckh.dia5.dto.auth;

import com.nckh.dia5.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributorAuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private DistributorUserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributorUserInfo {
        private String id;
        private String email;
        private String name;
        private UserRole role;
        private String walletAddress;
        private String companyName;
        private String companyAddress;
        private String phoneNumber;
        private String licenseNumber;
        private Boolean isActive;
        private Boolean isVerified;
        private Boolean isProfileComplete;
    }
}

