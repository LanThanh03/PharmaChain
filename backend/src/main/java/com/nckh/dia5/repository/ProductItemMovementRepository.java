package com.nckh.dia5.repository;

import com.nckh.dia5.model.ProductItemMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductItemMovementRepository extends JpaRepository<ProductItemMovement, Long> {

    // Find by item
    List<ProductItemMovement> findByProductItemIdOrderByMovementTimestampAsc(Long itemId);

    Page<ProductItemMovement> findByProductItemId(Long itemId, Pageable pageable);

    // Find by batch
    List<ProductItemMovement> findByDrugBatchId(Long batchId);

    Page<ProductItemMovement> findByDrugBatchId(Long batchId, Pageable pageable);

    // Find by movement type
    List<ProductItemMovement> findByMovementType(ProductItemMovement.MovementType movementType);

    Page<ProductItemMovement> findByMovementType(
            ProductItemMovement.MovementType movementType,
            Pageable pageable
    );

    // Find by company (from)
    List<ProductItemMovement> findByFromCompanyIdAndFromCompanyType(
            Long companyId,
            com.nckh.dia5.model.ProductItem.OwnerType companyType
    );

    // Find by company (to)
    List<ProductItemMovement> findByToCompanyIdAndToCompanyType(
            Long companyId,
            com.nckh.dia5.model.ProductItem.OwnerType companyType
    );

    Page<ProductItemMovement> findByToCompanyIdAndToCompanyType(
            Long companyId,
            com.nckh.dia5.model.ProductItem.OwnerType companyType,
            Pageable pageable
    );

    // Find by shipment
    List<ProductItemMovement> findByShipmentId(Long shipmentId);

    // Time-based queries
    List<ProductItemMovement> findByMovementTimestampBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT pim FROM ProductItemMovement pim " +
            "WHERE pim.toCompanyId = :companyId " +
            "AND pim.toCompanyType = :companyType " +
            "AND pim.movementTimestamp BETWEEN :startDate AND :endDate")
    List<ProductItemMovement> findMovementsByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("companyType") com.nckh.dia5.model.ProductItem.OwnerType companyType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Statistics
    @Query("SELECT COUNT(pim) FROM ProductItemMovement pim " +
            "WHERE pim.toCompanyId = :companyId " +
            "AND pim.toCompanyType = :companyType " +
            "AND pim.movementType = :movementType")
    Long countByCompanyAndMovementType(
            @Param("companyId") Long companyId,
            @Param("companyType") com.nckh.dia5.model.ProductItem.OwnerType companyType,
            @Param("movementType") ProductItemMovement.MovementType movementType
    );

    // Blockchain sync
    List<ProductItemMovement> findByIsBlockchainSynced(Boolean synced);

    @Query("SELECT pim FROM ProductItemMovement pim " +
            "WHERE pim.productItem.id = :itemId " +
            "AND pim.isBlockchainSynced = true " +
            "ORDER BY pim.movementTimestamp DESC")
    List<ProductItemMovement> findSyncedMovementsByItem(@Param("itemId") Long itemId);
}

