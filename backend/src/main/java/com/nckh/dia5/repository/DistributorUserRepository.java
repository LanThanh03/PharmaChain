package com.nckh.dia5.repository;

import com.nckh.dia5.model.DistributorUser;
import com.nckh.dia5.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorUserRepository extends JpaRepository<DistributorUser, String> {

    Optional<DistributorUser> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByWalletAddress(String walletAddress);
    
    List<DistributorUser> findByRole(UserRole role);
    
    List<DistributorUser> findByIsActive(Boolean isActive);
    
    List<DistributorUser> findByIsVerified(Boolean isVerified);
    
    @Query("SELECT d FROM DistributorUser d WHERE d.role = :role AND d.isActive = true AND d.isVerified = true")
    List<DistributorUser> findActiveVerifiedByRole(@Param("role") UserRole role);
    
    @Query("SELECT d FROM DistributorUser d WHERE d.licenseExpiryDate < :date AND d.isActive = true")
    List<DistributorUser> findByExpiredLicense(@Param("date") LocalDateTime date);
    
    Optional<DistributorUser> findByWalletAddress(String walletAddress);
    
    @Query("SELECT d FROM DistributorUser d WHERE d.companyName ILIKE %:name%")
    List<DistributorUser> findByCompanyNameContaining(@Param("name") String name);
}

