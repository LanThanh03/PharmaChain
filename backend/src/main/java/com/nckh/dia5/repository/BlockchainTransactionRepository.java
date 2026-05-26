package com.nckh.dia5.repository;

import com.nckh.dia5.model.BlockchainTransaction;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.Shipment;
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
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {

    Optional<BlockchainTransaction> findByTransactionHash(String transactionHash);

    boolean existsByTransactionHash(String transactionHash);

    List<BlockchainTransaction> findByFromAddress(String fromAddress);

    List<BlockchainTransaction> findByToAddress(String toAddress);

    List<BlockchainTransaction> findByStatus(BlockchainTransaction.TransactionStatus status);

    List<BlockchainTransaction> findByFunctionName(String functionName);

    List<BlockchainTransaction> findByDrugBatch(DrugBatch drugBatch);

    List<BlockchainTransaction> findByShipment(Shipment shipment);

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.fromAddress = :address OR bt.toAddress = :address")
    List<BlockchainTransaction> findByInvolvedAddress(@Param("address") String address);

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.blockNumber BETWEEN :startBlock AND :endBlock")
    List<BlockchainTransaction> findByBlockNumberBetween(@Param("startBlock") BigInteger startBlock, 
                                                        @Param("endBlock") BigInteger endBlock);

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.timestamp BETWEEN :startDate AND :endDate")
    Page<BlockchainTransaction> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate, 
                                                      Pageable pageable);

    @Query("SELECT COUNT(bt) FROM BlockchainTransaction bt WHERE bt.fromAddress = :address")
    Long countByFromAddress(@Param("address") String address);

    @Query("SELECT COUNT(bt) FROM BlockchainTransaction bt WHERE bt.toAddress = :address")
    Long countByToAddress(@Param("address") String address);

    @Query("SELECT COUNT(bt) FROM BlockchainTransaction bt WHERE bt.status = :status")
    Long countByStatus(@Param("status") BlockchainTransaction.TransactionStatus status);

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.functionName = :functionName AND bt.status = :status")
    List<BlockchainTransaction> findByFunctionNameAndStatus(@Param("functionName") String functionName, 
                                                           @Param("status") BlockchainTransaction.TransactionStatus status);

    @Query("SELECT MAX(bt.blockNumber) FROM BlockchainTransaction bt")
    Optional<BigInteger> findMaxBlockNumber();

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.errorMessage IS NOT NULL")
    List<BlockchainTransaction> findFailedTransactions();

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.drugBatch.batchId = :batchId ORDER BY bt.timestamp ASC")
    List<BlockchainTransaction> findByBatchIdOrderByTimestamp(@Param("batchId") BigInteger batchId);

    @Query("SELECT bt FROM BlockchainTransaction bt WHERE bt.shipment.shipmentCode = CONCAT('SHIP-', :shipmentId) ORDER BY bt.timestamp ASC")
    List<BlockchainTransaction> findByShipmentIdOrderByTimestamp(@Param("shipmentId") BigInteger shipmentId);
}
