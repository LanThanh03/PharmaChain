package com.nckh.dia5.repository;

import com.nckh.dia5.model.ManufacturerUser;
import com.nckh.dia5.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManufacturerUserRepository extends JpaRepository<ManufacturerUser, String> {
    
    Optional<ManufacturerUser> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByWalletAddress(String walletAddress);
    
    List<ManufacturerUser> findByRole(UserRole role);
    
    List<ManufacturerUser> findByIsVerified(Boolean isVerified);
    
    @Query("SELECT m FROM ManufacturerUser m WHERE m.role = :role AND m.isVerified = true")
    List<ManufacturerUser> findVerifiedByRole(@Param("role") UserRole role);
    
    Optional<ManufacturerUser> findByWalletAddress(String walletAddress);
    
    @Query("SELECT m FROM ManufacturerUser m WHERE m.companyName ILIKE %:name%")
    List<ManufacturerUser> findByCompanyNameContaining(@Param("name") String name);
}
