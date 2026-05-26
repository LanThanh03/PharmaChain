package com.nckh.dia5.repository;

import com.nckh.dia5.model.DistributorInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorInventoryRepository extends JpaRepository<DistributorInventory, Long> {

    // Find all inventory for a specific distributor
    List<DistributorInventory> findByDistributorId(Long distributorId);

    // Find by distributor and batch
    Optional<DistributorInventory> findByDistributorIdAndDrugBatchId(Long distributorId, Long batchId);

    // Find by distributor wallet address
    @Query("SELECT di FROM DistributorInventory di WHERE di.distributor.walletAddress = :walletAddress")
    List<DistributorInventory> findByDistributorWalletAddress(@Param("walletAddress") String walletAddress);

    // Find low stock items
    @Query("SELECT di FROM DistributorInventory di WHERE di.distributor.id = :distributorId AND di.availableQuantity <= di.minStockLevel")
    List<DistributorInventory> findLowStockItems(@Param("distributorId") Long distributorId);

    // Find expiring soon items (within 30 days)
    @Query("SELECT di FROM DistributorInventory di WHERE di.distributor.id = :distributorId AND di.expiryDate <= CURRENT_TIMESTAMP + 30 DAY")
    List<DistributorInventory> findExpiringSoonItems(@Param("distributorId") Long distributorId);

    // Find by status
    List<DistributorInventory> findByDistributorIdAndStatus(Long distributorId, DistributorInventory.InventoryStatus status);

    // Search by drug name
    @Query("SELECT di FROM DistributorInventory di WHERE di.distributor.id = :distributorId AND LOWER(di.drugName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<DistributorInventory> searchByDrugName(@Param("distributorId") Long distributorId, @Param("searchTerm") String searchTerm);

    // Get total inventory value for distributor
    @Query("SELECT COALESCE(SUM(di.totalValue), 0) FROM DistributorInventory di WHERE di.distributor.id = :distributorId")
    Double getTotalInventoryValue(@Param("distributorId") Long distributorId);

    // Find by drug batch ID (for batch consistency check)
    List<DistributorInventory> findByDrugBatchId(Long batchId);
}
