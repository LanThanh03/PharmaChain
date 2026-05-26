package com.nckh.dia5.repository;

import com.nckh.dia5.model.PharmaCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmaCompanyRepository extends JpaRepository<PharmaCompany, Long> {
    
    Optional<PharmaCompany> findByManufacturerUserId(String manufacturerUserId);
    
    Optional<PharmaCompany> findByWalletAddress(String walletAddress);
    
    boolean existsByManufacturerUserId(String manufacturerUserId);
    
    List<PharmaCompany> findByCompanyTypeAndIsActive(PharmaCompany.CompanyType companyType, Boolean isActive);
}
