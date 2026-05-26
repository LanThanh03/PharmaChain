package com.nckh.dia5.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributorDto {
    
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String walletAddress;
    private String licenseNumber;
    private String website;
    private String contactPerson;
    private String status;
}
