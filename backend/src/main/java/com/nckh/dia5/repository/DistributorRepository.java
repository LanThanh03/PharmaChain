package com.nckh.dia5.repository;

import com.nckh.dia5.model.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    List<Distributor> findByStatus(Distributor.DistributorStatus status);

    Optional<Distributor> findByWalletAddress(String walletAddress);

    Optional<Distributor> findByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM Distributor d WHERE d.name LIKE %:name%")
    List<Distributor> findByNameContaining(@Param("name") String name);

    @Query("SELECT d FROM Distributor d WHERE d.email = :email")
    Optional<Distributor> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(d) FROM Distributor d WHERE d.status = :status")
    Long countByStatus(@Param("status") Distributor.DistributorStatus status);
}
