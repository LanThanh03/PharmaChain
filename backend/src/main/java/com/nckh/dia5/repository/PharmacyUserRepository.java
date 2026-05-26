package com.nckh.dia5.repository;

import com.nckh.dia5.model.PharmacyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacyUserRepository extends JpaRepository<PharmacyUser, Long> {
    Optional<PharmacyUser> findByEmail(String email);
    Optional<PharmacyUser> findByWalletAddress(String walletAddress);
    Optional<PharmacyUser> findByPharmacyCode(String pharmacyCode);
    boolean existsByEmail(String email);
    boolean existsByWalletAddress(String walletAddress);
}
