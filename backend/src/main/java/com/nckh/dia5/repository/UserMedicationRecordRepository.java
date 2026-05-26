package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserMedicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for user medication records (medications tracked in mobile app)
 */
@Repository
public interface UserMedicationRecordRepository extends JpaRepository<UserMedicationRecord, Long> {

    // Find by user
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"productItem", "productItem.drugProduct"})
    List<UserMedicationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find active medications for user
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"productItem", "productItem.drugProduct"})
    List<UserMedicationRecord> findByUserIdAndIsActiveTrue(Long userId);

    // Find by user and item code
    Optional<UserMedicationRecord> findByUserIdAndItemCode(Long userId, String itemCode);

    // Find by product item
    Optional<UserMedicationRecord> findByProductItemId(Long productItemId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByProductItemId(Long productItemId);

    // Find by dispense instruction
    Optional<UserMedicationRecord> findByDispenseInstructionId(Long dispenseInstructionId);

    // Find completed medications
    List<UserMedicationRecord> findByUserIdAndIsCompletedTrue(Long userId);

    // Find medications expiring soon
    @Query("SELECT r FROM UserMedicationRecord r WHERE r.userId = :userId AND r.isActive = true " +
           "AND r.expiryDate IS NOT NULL AND r.expiryDate <= :expiryDate")
    List<UserMedicationRecord> findExpiringSoon(
            @Param("userId") Long userId,
            @Param("expiryDate") LocalDate expiryDate);

    // Find medications ending soon
    @Query("SELECT r FROM UserMedicationRecord r WHERE r.userId = :userId AND r.isActive = true " +
           "AND r.endDate IS NOT NULL AND r.endDate <= :endDate")
    List<UserMedicationRecord> findEndingSoon(
            @Param("userId") Long userId,
            @Param("endDate") LocalDate endDate);

    // Count active medications for user
    long countByUserIdAndIsActiveTrue(Long userId);

    // Get adherence statistics
    @Query("SELECT AVG(r.adherenceRate) FROM UserMedicationRecord r WHERE r.userId = :userId AND r.isCompleted = true")
    Double getAverageAdherenceRate(@Param("userId") Long userId);

    // Check if user already has this medication active
    boolean existsByUserIdAndItemCodeAndIsActiveTrue(Long userId, String itemCode);

    // Find all active medications needing reminders today
    @Query("SELECT r FROM UserMedicationRecord r WHERE r.isActive = true " +
           "AND r.startDate <= CURRENT_DATE AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
    List<UserMedicationRecord> findAllActiveForToday();
}
