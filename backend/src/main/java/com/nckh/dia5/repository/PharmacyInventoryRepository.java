package com.nckh.dia5.repository;

import com.nckh.dia5.model.PharmacyInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyInventoryRepository extends JpaRepository<PharmacyInventory, Long> {

    // Find all inventory for a specific pharmacy
    List<PharmacyInventory> findByPharmacyId(Long pharmacyId);

    // Find by pharmacy and batch
    Optional<PharmacyInventory> findByPharmacyIdAndDrugBatchId(Long pharmacyId, Long batchId);

    // Find by pharmacy wallet address
    @Query("SELECT pi FROM PharmacyInventory pi WHERE pi.pharmacy.walletAddress = :walletAddress")
    List<PharmacyInventory> findByPharmacyWalletAddress(@Param("walletAddress") String walletAddress);

    // Find low stock items
    @Query("SELECT pi FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId AND pi.availableQuantity <= pi.minStockLevel")
    List<PharmacyInventory> findLowStockItems(@Param("pharmacyId") Long pharmacyId);

    // Find items need reorder
    @Query("SELECT pi FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId AND pi.availableQuantity <= pi.reorderPoint")
    List<PharmacyInventory> findItemsNeedReorder(@Param("pharmacyId") Long pharmacyId);

    // Find expiring soon items (within 30 days)
    @Query("SELECT pi FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId AND pi.expiryDate <= CURRENT_TIMESTAMP + 30 DAY")
    List<PharmacyInventory> findExpiringSoonItems(@Param("pharmacyId") Long pharmacyId);

    // Find by status
    List<PharmacyInventory> findByPharmacyIdAndStatus(Long pharmacyId, PharmacyInventory.PharmacyInventoryStatus status);

    // Search by drug name
    @Query("SELECT pi FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId AND LOWER(pi.drugName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PharmacyInventory> searchByDrugName(@Param("pharmacyId") Long pharmacyId, @Param("searchTerm") String searchTerm);

    // Find featured products
    List<PharmacyInventory> findByPharmacyIdAndIsFeaturedTrue(Long pharmacyId);

    // Find products on sale
    List<PharmacyInventory> findByPharmacyIdAndIsOnSaleTrue(Long pharmacyId);

    // Get total inventory value for pharmacy
    @Query("SELECT COALESCE(SUM(pi.totalValue), 0) FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId")
    Double getTotalInventoryValue(@Param("pharmacyId") Long pharmacyId);

    // Get total retail value for pharmacy
    @Query("SELECT COALESCE(SUM(pi.availableQuantity * pi.retailPrice), 0) FROM PharmacyInventory pi WHERE pi.pharmacy.id = :pharmacyId")
    Double getTotalRetailValue(@Param("pharmacyId") Long pharmacyId);

    // Find by drug batch ID (for batch consistency check)
    List<PharmacyInventory> findByDrugBatchId(Long batchId);
}
