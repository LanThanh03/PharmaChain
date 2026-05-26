package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserLifestyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLifestyleRepository extends JpaRepository<UserLifestyle, Integer> {

    Optional<UserLifestyle> findByUserId(String userId);

    @Query("SELECT ul FROM UserLifestyle ul WHERE ul.smokingStatus = :status")
    List<UserLifestyle> findBySmokingStatus(@Param("status") UserLifestyle.SmokingStatus status);

    @Query("SELECT ul FROM UserLifestyle ul WHERE ul.alcoholFrequency = :frequency")
    List<UserLifestyle> findByAlcoholFrequency(@Param("frequency") UserLifestyle.Frequency frequency);

    @Query("SELECT ul FROM UserLifestyle ul WHERE ul.exerciseFrequency = :frequency")
    List<UserLifestyle> findByExerciseFrequency(@Param("frequency") UserLifestyle.ExerciseFrequency frequency);

    @Query("SELECT ul FROM UserLifestyle ul WHERE ul.stressLevel = :level")
    List<UserLifestyle> findByStressLevel(@Param("level") UserLifestyle.StressLevel level);

    @Query("SELECT COUNT(ul) FROM UserLifestyle ul WHERE ul.smokingStatus = 'current'")
    Long countCurrentSmokers();

    @Query("SELECT COUNT(ul) FROM UserLifestyle ul WHERE ul.exerciseFrequency IN ('weekly', 'daily')")
    Long countRegularExercisers();
}
