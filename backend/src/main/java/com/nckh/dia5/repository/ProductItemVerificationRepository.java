package com.nckh.dia5.repository;

import com.nckh.dia5.model.ProductItemVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductItemVerificationRepository extends JpaRepository<ProductItemVerification, Long> {

    // Find by item
    List<ProductItemVerification> findByProductItemIdOrderByScanTimestampDesc(Long itemId);

    Page<ProductItemVerification> findByProductItemId(Long itemId, Pageable pageable);

    @Query("SELECT COUNT(piv) FROM ProductItemVerification piv WHERE piv.productItem.id = :itemId")
    Long countByItemId(@Param("itemId") Long itemId);

    // Find by scanner
    List<ProductItemVerification> findByScannerIdAndScannerType(
            String scannerId,
            ProductItemVerification.ScannerType scannerType
    );

    Page<ProductItemVerification> findByScannerIdAndScannerType(
            String scannerId,
            ProductItemVerification.ScannerType scannerType,
            Pageable pageable
    );

    // Find by scanner type
    List<ProductItemVerification> findByScannerType(ProductItemVerification.ScannerType scannerType);

    Page<ProductItemVerification> findByScannerType(
            ProductItemVerification.ScannerType scannerType,
            Pageable pageable
    );

    // Find by verification result
    List<ProductItemVerification> findByVerificationResult(
            ProductItemVerification.VerificationResult result
    );

    Page<ProductItemVerification> findByVerificationResult(
            ProductItemVerification.VerificationResult result,
            Pageable pageable
    );

    // Time-based queries
    List<ProductItemVerification> findByScanTimestampBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT piv FROM ProductItemVerification piv " +
            "WHERE piv.scannerType = :scannerType " +
            "AND piv.scanTimestamp BETWEEN :startDate AND :endDate")
    List<ProductItemVerification> findByScannerTypeAndDateRange(
            @Param("scannerType") ProductItemVerification.ScannerType scannerType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Suspicious activity detection
    @Query("SELECT piv FROM ProductItemVerification piv " +
            "WHERE piv.verificationResult IN ('SUSPICIOUS', 'COUNTERFEIT') " +
            "ORDER BY piv.scanTimestamp DESC")
    List<ProductItemVerification> findSuspiciousVerifications();

    @Query("SELECT piv FROM ProductItemVerification piv " +
            "WHERE piv.verificationResult IN ('SUSPICIOUS', 'COUNTERFEIT') " +
            "AND piv.scanTimestamp >= :since " +
            "ORDER BY piv.scanTimestamp DESC")
    List<ProductItemVerification> findRecentSuspiciousVerifications(@Param("since") LocalDateTime since);

    // Statistics
    @Query("SELECT piv.scannerType, COUNT(piv) FROM ProductItemVerification piv " +
            "GROUP BY piv.scannerType")
    List<Object[]> countByScannerType();

    @Query("SELECT piv.verificationResult, COUNT(piv) FROM ProductItemVerification piv " +
            "GROUP BY piv.verificationResult")
    List<Object[]> countByVerificationResult();

    @Query("SELECT COUNT(piv) FROM ProductItemVerification piv " +
            "WHERE piv.productItem.id = :itemId " +
            "AND piv.scannerType = :scannerType")
    Long countByItemIdAndScannerType(
            @Param("itemId") Long itemId,
            @Param("scannerType") ProductItemVerification.ScannerType scannerType
    );

    // Recent scans
    @Query("SELECT piv FROM ProductItemVerification piv " +
            "WHERE piv.productItem.id = :itemId " +
            "ORDER BY piv.scanTimestamp DESC")
    List<ProductItemVerification> findRecentScansByItem(
            @Param("itemId") Long itemId,
            Pageable pageable
    );

    // IP-based queries (for fraud detection)
    @Query("SELECT piv FROM ProductItemVerification piv " +
            "WHERE piv.ipAddress = :ipAddress " +
            "AND piv.scanTimestamp >= :since " +
            "ORDER BY piv.scanTimestamp DESC")
    List<ProductItemVerification> findRecentScansByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    // Blockchain verification stats
    @Query("SELECT COUNT(piv) FROM ProductItemVerification piv " +
            "WHERE piv.blockchainVerified = true")
    Long countBlockchainVerified();

    @Query("SELECT AVG(piv.blockchainQueryTimeMs) FROM ProductItemVerification piv " +
            "WHERE piv.blockchainQueryTimeMs IS NOT NULL")
    Double getAverageBlockchainQueryTime();
}

