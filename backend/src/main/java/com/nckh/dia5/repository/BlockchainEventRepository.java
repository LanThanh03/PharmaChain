package com.nckh.dia5.repository;

import com.nckh.dia5.model.BlockchainEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho BlockchainEvent
 */
@Repository
public interface BlockchainEventRepository extends JpaRepository<BlockchainEvent, Long> {

    /**
     * Kiểm tra event đã tồn tại chưa
     */
    boolean existsByTransactionHashAndLogIndex(String transactionHash, BigInteger logIndex);

    /**
     * Lấy events chưa được process
     */
    List<BlockchainEvent> findByProcessedFalseOrderByBlockNumberAscLogIndexAsc();

    /**
     * Lấy events theo batch ID
     */
    List<BlockchainEvent> findByBatchIdOrderByIndexedAtAsc(BigInteger batchId);

    /**
     * Lấy events theo shipment ID  
     */
    List<BlockchainEvent> findByShipmentIdOrderByIndexedAtAsc(BigInteger shipmentId);

    /**
     * Lấy events theo loại
     */
    List<BlockchainEvent> findByEventTypeOrderByIndexedAtDesc(String eventType);

    /**
     * Lấy block number cao nhất đã index
     */
    @Query("SELECT MAX(e.blockNumber) FROM BlockchainEvent e")
    Optional<BigInteger> findMaxBlockNumber();

    /**
     * Lấy events theo address (from hoặc to)
     */
    @Query("SELECT e FROM BlockchainEvent e WHERE e.fromAddress = :address OR e.toAddress = :address ORDER BY e.indexedAt DESC")
    List<BlockchainEvent> findByAddress(@Param("address") String address);

    /**
     * Đếm số events chưa process
     */
    long countByProcessedFalse();
}
