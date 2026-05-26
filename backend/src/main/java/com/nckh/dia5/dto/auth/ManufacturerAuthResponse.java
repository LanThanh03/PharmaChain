package com.nckh.dia5.dto.auth;

import com.nckh.dia5.model.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManufacturerAuthResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String name;
        private UserRole role;
        private String companyName;
        private String companyAddress;
        private String walletAddress;
        private String licenseNumber;
        private Boolean isVerified;
    }
}
