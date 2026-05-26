package com.nckh.dia5.repository;

import com.nckh.dia5.model.RawMaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RawMaterialBatchRepository extends JpaRepository<RawMaterialBatch, Long> {
    Optional<RawMaterialBatch> findByBatchNumber(String batchNumber);
}
