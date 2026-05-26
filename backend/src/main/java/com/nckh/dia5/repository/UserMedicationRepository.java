package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMedicationRepository extends JpaRepository<UserMedication, Integer> {

    @Query("SELECT um FROM UserMedication um WHERE um.user.id = :userId ORDER BY um.startDate DESC")
    List<UserMedication> findByUserId(@Param("userId") String userId);

    @Query("SELECT um FROM UserMedication um WHERE um.user.id = :userId AND um.isActive = true ORDER BY um.startDate DESC")
    List<UserMedication> findActiveByUserId(@Param("userId") String userId);

    Optional<UserMedication> findByUserIdAndMedicationId(String userId, Integer medicationId);

    @Query("SELECT COUNT(um) FROM UserMedication um WHERE um.user.id = :userId AND um.isActive = true")
    Long countActiveByUserId(@Param("userId") String userId);

    @Query("SELECT um FROM UserMedication um WHERE um.medication.id = :medicationId AND um.isActive = true")
    List<UserMedication> findActiveByMedicationId(@Param("medicationId") Integer medicationId);

    @Query("SELECT um FROM UserMedication um WHERE um.user.id = :userId AND " +
            "(um.endDate IS NULL OR um.endDate >= :currentDate) AND um.isActive = true")
    List<UserMedication> findCurrentMedicationsByUserId(@Param("userId") String userId,
            @Param("currentDate") LocalDate currentDate);

    @Query("SELECT um FROM UserMedication um WHERE um.adherenceLevel = :level")
    List<UserMedication> findByAdherenceLevel(@Param("level") UserMedication.AdherenceLevel level);
}
