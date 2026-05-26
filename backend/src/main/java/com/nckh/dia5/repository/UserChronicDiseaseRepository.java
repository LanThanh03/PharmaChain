package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserChronicDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChronicDiseaseRepository extends JpaRepository<UserChronicDisease, Integer> {

    @Query("SELECT ucd FROM UserChronicDisease ucd WHERE ucd.user.id = :userId ORDER BY ucd.diagnosedDate DESC")
    List<UserChronicDisease> findByUserId(@Param("userId") String userId);

    @Query("SELECT ucd FROM UserChronicDisease ucd WHERE ucd.user.id = :userId AND ucd.currentStatus = :status")
    List<UserChronicDisease> findByUserIdAndStatus(@Param("userId") String userId,
            @Param("status") UserChronicDisease.Status status);

    @Query("SELECT ucd FROM UserChronicDisease ucd WHERE ucd.user.id = :userId AND ucd.currentStatus = 'active'")
    List<UserChronicDisease> findActiveByUserId(@Param("userId") String userId);

    Optional<UserChronicDisease> findByUserIdAndDiseaseId(String userId, Integer diseaseId);

    @Query("SELECT COUNT(ucd) FROM UserChronicDisease ucd WHERE ucd.user.id = :userId AND ucd.currentStatus = 'active'")
    Long countActiveByUserId(@Param("userId") String userId);

    @Query("SELECT ucd FROM UserChronicDisease ucd WHERE ucd.disease.id = :diseaseId AND ucd.currentStatus = 'active'")
    List<UserChronicDisease> findActiveByDiseaseId(@Param("diseaseId") Integer diseaseId);
}
