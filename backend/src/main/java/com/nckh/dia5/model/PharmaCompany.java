package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pharma_companies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PharmaCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "establishment_date")
    private LocalDateTime establishmentDate;

    @Builder.Default
    @Column(name = "blockchain_verified")
    private Boolean blockchainVerified = false;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "manufacturer_user_id", length = 36)
    private String manufacturerUserId; // Link to ManufacturerUser.id

    @Builder.Default
    @Column(name = "status")
    private String status = "ACTIVE";

    public enum CompanyType {
        MANUFACTURER,
        DISTRIBUTOR,
        PHARMACY
    }

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
