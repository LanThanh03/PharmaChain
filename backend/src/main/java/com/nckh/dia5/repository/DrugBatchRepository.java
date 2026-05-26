package com.nckh.dia5.repository;

import com.nckh.dia5.model.DrugBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrugBatchRepository extends JpaRepository<DrugBatch, Long> {

    Optional<DrugBatch> findByBatchId(BigInteger batchId);

    Optional<DrugBatch> findByQrCode(String qrCode);

    List<DrugBatch> findByManufacturerAddress(String manufacturerAddress);

    List<DrugBatch> findByCurrentOwner(String currentOwner);

    List<DrugBatch> findByStatus(DrugBatch.BatchStatus status);

    List<DrugBatch> findByCurrentOwnerAndStatus(String currentOwner, DrugBatch.BatchStatus status);

    List<DrugBatch> findByIsSynced(Boolean isSynced);

    @Query("SELECT db FROM DrugBatch db WHERE db.expiryDate BETWEEN :startDate AND :endDate")
    List<DrugBatch> findByExpiryDateBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT db FROM DrugBatch db WHERE db.drugName LIKE %:drugName%")
    Page<DrugBatch> findByDrugNameContaining(@Param("drugName") String drugName, Pageable pageable);

    @Query("SELECT db FROM DrugBatch db WHERE db.batchNumber LIKE %:batchNumber%")
    List<DrugBatch> findByBatchNumberContaining(@Param("batchNumber") String batchNumber);

    // Find by exact batch number (Số lô) - used for receiving goods
    Optional<DrugBatch> findByBatchNumber(String batchNumber);

    @Query("SELECT db FROM DrugBatch db WHERE db.manufacturerAddress = :address AND db.status = :status")
    List<DrugBatch> findByManufacturerAddressAndStatus(@Param("address") String address, 
                                                       @Param("status") DrugBatch.BatchStatus status);

    @Query("SELECT COUNT(db) FROM DrugBatch db WHERE db.manufacturerAddress = :address")
    Long countByManufacturerAddress(@Param("address") String address);

    @Query("SELECT COUNT(db) FROM DrugBatch db WHERE db.currentOwner = :owner")
    Long countByCurrentOwner(@Param("owner") String owner);

    @Query("SELECT db FROM DrugBatch db WHERE db.expiryDate < :currentDate")
    List<DrugBatch> findExpiredBatches(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT COUNT(db) FROM DrugBatch db WHERE db.drugName = :drugName AND db.manufacturerAddress = :address")
    Long countByDrugNameAndManufacturerAddress(@Param("drugName") String drugName, @Param("address") String address);

    @Query("SELECT COALESCE(SUM(db.quantity), 0) FROM DrugBatch db WHERE db.drugName = :drugName AND db.manufacturerAddress = :address")
    Long sumQuantityByDrugNameAndManufacturerAddress(@Param("drugName") String drugName, @Param("address") String address);

    @Query("SELECT db FROM DrugBatch db WHERE db.transactionHash = :hash")
    Optional<DrugBatch> findByTransactionHash(@Param("hash") String transactionHash);
}
