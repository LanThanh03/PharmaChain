package com.nckh.dia5.repository;

import com.nckh.dia5.model.ProductItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {

    // Find by item code
    Optional<ProductItem> findByItemCode(String itemCode);

    // Find by item code partial match
    List<ProductItem> findByItemCodeContaining(String itemCode);

    boolean existsByItemCode(String itemCode);

    // Find by batch
    List<ProductItem> findByDrugBatch(com.nckh.dia5.model.DrugBatch batch);

    List<ProductItem> findByDrugBatchId(Long batchId);

    Page<ProductItem> findByDrugBatchId(Long batchId, Pageable pageable);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.drugBatch.id = :batchId")
    Long countByBatchId(@Param("batchId") Long batchId);

    // Find by status
    List<ProductItem> findByCurrentStatus(ProductItem.ItemStatus status);

    Page<ProductItem> findByCurrentStatus(ProductItem.ItemStatus status, Pageable pageable);

    // Find by owner
    List<ProductItem> findByCurrentOwnerIdAndCurrentOwnerType(
            Long ownerId,
            ProductItem.OwnerType ownerType
    );

    Page<ProductItem> findByCurrentOwnerIdAndCurrentOwnerType(
            Long ownerId,
            ProductItem.OwnerType ownerType,
            Pageable pageable
    );

    // Find by drug product
    List<ProductItem> findByDrugProductId(Long drugProductId);

    Page<ProductItem> findByDrugProductId(Long drugProductId, Pageable pageable);

    // Expiry queries
    @Query("SELECT pi FROM ProductItem pi WHERE pi.expiryDate < :date")
    List<ProductItem> findExpiredItems(@Param("date") LocalDateTime date);

    @Query("SELECT pi FROM ProductItem pi WHERE pi.expiryDate BETWEEN :startDate AND :endDate")
    List<ProductItem> findExpiringSoonItems(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT pi FROM ProductItem pi " +
            "WHERE pi.currentOwnerId = :ownerId " +
            "AND pi.currentOwnerType = :ownerType " +
            "AND pi.expiryDate BETWEEN :startDate AND :endDate")
    List<ProductItem> findExpiringSoonItemsByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ProductItem.OwnerType ownerType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Statistics queries
    @Query("SELECT COUNT(pi) FROM ProductItem pi " +
            "WHERE pi.currentOwnerId = :ownerId " +
            "AND pi.currentOwnerType = :ownerType")
    Long countByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ProductItem.OwnerType ownerType
    );

    @Query("SELECT COUNT(pi) FROM ProductItem pi " +
            "WHERE pi.currentOwnerId = :ownerId " +
            "AND pi.currentOwnerType = :ownerType " +
            "AND pi.currentStatus = :status")
    Long countByOwnerAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ProductItem.OwnerType ownerType,
            @Param("status") ProductItem.ItemStatus status
    );

    // Search queries
    @Query("SELECT pi FROM ProductItem pi " +
            "WHERE pi.itemCode LIKE %:keyword% " +
            "OR pi.drugProduct.name LIKE %:keyword% " +
            "OR pi.drugBatch.batchNumber LIKE %:keyword%")
    Page<ProductItem> searchItems(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT pi FROM ProductItem pi " +
            "WHERE pi.currentOwnerId = :ownerId " +
            "AND pi.currentOwnerType = :ownerType " +
            "AND (pi.itemCode LIKE %:keyword% " +
            "OR pi.drugProduct.name LIKE %:keyword% " +
            "OR pi.drugBatch.batchNumber LIKE %:keyword%)")
    Page<ProductItem> searchItemsByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ProductItem.OwnerType ownerType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // QR Code queries
    Optional<ProductItem> findByQrCodeData(String qrCodeData);

    @Query("SELECT pi FROM ProductItem pi WHERE pi.qrCodeData IS NULL OR pi.qrImagePath IS NULL")
    List<ProductItem> findItemsWithoutQR();

    // Blockchain sync queries
    List<ProductItem> findByIsBlockchainSynced(Boolean synced);

    @Query("SELECT pi FROM ProductItem pi " +
            "WHERE pi.drugBatch.id = :batchId " +
            "AND pi.isBlockchainSynced = :synced")
    List<ProductItem> findByBatchIdAndBlockchainSynced(
            @Param("batchId") Long batchId,
            @Param("synced") Boolean synced
    );

    // Count queries for statistics
    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.isBlockchainSynced = :synced")
    Long countByIsBlockchainSynced(@Param("synced") Boolean synced);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.currentStatus = :status")
    Long countByCurrentStatus(@Param("status") ProductItem.ItemStatus status);

    // Bulk operations
    @Query("SELECT pi.itemCode FROM ProductItem pi WHERE pi.drugBatch.id = :batchId")
    List<String> findItemCodesByBatchId(@Param("batchId") Long batchId);
}

