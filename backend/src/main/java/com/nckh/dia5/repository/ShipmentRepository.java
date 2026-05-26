package com.nckh.dia5.repository;

import com.nckh.dia5.model.Shipment;
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
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByShipmentCode(String shipmentCode);
    
    @Query("SELECT s FROM Shipment s WHERE s.shipmentCode = CONCAT('SHIP-', :shipmentId)")
    Optional<Shipment> findByShipmentId(@Param("shipmentId") BigInteger shipmentId);

    List<Shipment> findByDrugBatch(DrugBatch drugBatch);

    @Query("SELECT s FROM Shipment s WHERE s.fromCompany.walletAddress = :address")
    List<Shipment> findByFromAddress(@Param("address") String fromAddress);

    @Query("SELECT s FROM Shipment s WHERE s.toCompany.walletAddress = :address")
    List<Shipment> findByToAddress(@Param("address") String toAddress);

    List<Shipment> findByStatus(Shipment.ShipmentStatus status);

    @Query("SELECT s FROM Shipment s WHERE JSON_EXTRACT(s.notes, '$.is_synced') = :isSynced")
    List<Shipment> findByIsSynced(@Param("isSynced") Boolean isSynced);
    
    @Query("SELECT s FROM Shipment s WHERE JSON_EXTRACT(s.notes, '$.blockchain_id') = :blockchainId")
    Optional<Shipment> findByBlockchainId(@Param("blockchainId") String blockchainId);

    @Query("SELECT s FROM Shipment s WHERE s.fromCompany.walletAddress = :address OR s.toCompany.walletAddress = :address")
    List<Shipment> findByInvolvedAddress(@Param("address") String address);

    @Query("SELECT s FROM Shipment s WHERE s.drugBatch.batchId = :batchId")
    List<Shipment> findByBatchId(@Param("batchId") BigInteger batchId);

    @Query("SELECT s FROM Shipment s WHERE s.shipmentDate BETWEEN :startDate AND :endDate")
    List<Shipment> findByShipmentTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM Shipment s WHERE s.fromCompany.walletAddress = :from AND s.toCompany.walletAddress = :to")
    Page<Shipment> findByFromAddressAndToAddress(@Param("from") String fromAddress, 
                                                 @Param("to") String toAddress, 
                                                 Pageable pageable);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.fromCompany.walletAddress = :address")
    Long countByFromAddress(@Param("address") String address);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.toCompany.walletAddress = :address")
    Long countByToAddress(@Param("address") String address);

    @Query("SELECT s FROM Shipment s WHERE s.status = :status AND s.shipmentDate < :threshold")
    List<Shipment> findStaleShipments(@Param("status") Shipment.ShipmentStatus status, 
                                     @Param("threshold") LocalDateTime threshold);

    @Query("SELECT s FROM Shipment s WHERE s.createTxHash = :hash OR s.receiveTxHash = :hash")
    Optional<Shipment> findByTransactionHash(@Param("hash") String transactionHash);

    @Query("SELECT s FROM Shipment s JOIN s.drugBatch db WHERE db.drugName LIKE %:drugName%")
    List<Shipment> findByDrugName(@Param("drugName") String drugName);
}
