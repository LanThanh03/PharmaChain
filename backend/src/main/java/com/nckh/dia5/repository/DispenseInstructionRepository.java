package com.nckh.dia5.repository;

import com.nckh.dia5.model.DispenseInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for dispense instructions (medication usage instructions from pharmacy)
 */
@Repository
public interface DispenseInstructionRepository extends JpaRepository<DispenseInstruction, Long> {

    // Find by product item
    Optional<DispenseInstruction> findByProductItemId(Long productItemId);
    
    @org.springframework.transaction.annotation.Transactional
    void deleteByProductItemId(Long productItemId);

    List<DispenseInstruction> findByProductItemIdIn(List<Long> productItemIds);

    // Find by item code
    Optional<DispenseInstruction> findByItemCode(String itemCode);

    // Find by customer phone
    List<DispenseInstruction> findByCustomerPhoneOrderByDispensedAtDesc(String customerPhone);

    // Find by pharmacy
    List<DispenseInstruction> findByPharmacyIdOrderByDispensedAtDesc(Long pharmacyId);

    // Find by app user
    List<DispenseInstruction> findByCustomerAppUserIdOrderByDispensedAtDesc(Long appUserId);

    // Find recent dispenses for a customer
    @Query("SELECT di FROM DispenseInstruction di WHERE di.customerPhone = :phone " +
           "AND di.dispensedAt >= :since ORDER BY di.dispensedAt DESC")
    List<DispenseInstruction> findRecentByCustomerPhone(
            @Param("phone") String phone,
            @Param("since") LocalDateTime since);

    // Find by pharmacy in date range
    @Query("SELECT di FROM DispenseInstruction di WHERE di.pharmacyId = :pharmacyId " +
           "AND di.dispensedAt BETWEEN :startDate AND :endDate ORDER BY di.dispensedAt DESC")
    List<DispenseInstruction> findByPharmacyAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count dispenses by pharmacy today
    @Query("SELECT COUNT(di) FROM DispenseInstruction di WHERE di.pharmacyId = :pharmacyId " +
           "AND DATE(di.dispensedAt) = CURRENT_DATE")
    long countTodayByPharmacy(@Param("pharmacyId") Long pharmacyId);

    // Check if item was already dispensed
    boolean existsByProductItemId(Long productItemId);
}
