package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserFamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFamilyHistoryRepository extends JpaRepository<UserFamilyHistory, Integer> {

    @Query("SELECT ufh FROM UserFamilyHistory ufh WHERE ufh.user.id = :userId ORDER BY ufh.relationship, ufh.ageOfOnset")
    List<UserFamilyHistory> findByUserId(@Param("userId") String userId);

    @Query("SELECT ufh FROM UserFamilyHistory ufh WHERE ufh.user.id = :userId AND ufh.relationship = :relationship")
    List<UserFamilyHistory> findByUserIdAndRelationship(@Param("userId") String userId,
            @Param("relationship") UserFamilyHistory.Relationship relationship);

    @Query("SELECT ufh FROM UserFamilyHistory ufh WHERE ufh.disease.id = :diseaseId")
    List<UserFamilyHistory> findByDiseaseId(@Param("diseaseId") Integer diseaseId);

    @Query("SELECT ufh FROM UserFamilyHistory ufh WHERE ufh.user.id = :userId AND ufh.disease.id = :diseaseId")
    List<UserFamilyHistory> findByUserIdAndDiseaseId(@Param("userId") String userId,
            @Param("diseaseId") Integer diseaseId);

    @Query("SELECT COUNT(ufh) FROM UserFamilyHistory ufh WHERE ufh.user.id = :userId")
    Long countByUserId(@Param("userId") String userId);
}
