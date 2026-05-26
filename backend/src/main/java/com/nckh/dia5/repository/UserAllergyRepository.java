package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Integer> {

    @Query("SELECT ua FROM UserAllergy ua WHERE ua.user.id = :userId ORDER BY ua.severity DESC")
    List<UserAllergy> findByUserId(@Param("userId") String userId);

    @Query("SELECT ua FROM UserAllergy ua WHERE ua.user.id = :userId AND ua.isActive = true ORDER BY ua.severity DESC")
    List<UserAllergy> findActiveByUserId(@Param("userId") String userId);

    Optional<UserAllergy> findByUserIdAndAllergenId(String userId, Integer allergenId);

    @Query("SELECT COUNT(ua) FROM UserAllergy ua WHERE ua.user.id = :userId AND ua.isActive = true")
    Long countActiveByUserId(@Param("userId") String userId);

    @Query("SELECT ua FROM UserAllergy ua WHERE ua.severity = :severity AND ua.isActive = true")
    List<UserAllergy> findBySeverity(@Param("severity") UserAllergy.Severity severity);

    @Query("SELECT ua FROM UserAllergy ua WHERE ua.allergen.id = :allergenId AND ua.isActive = true")
    List<UserAllergy> findActiveByAllergenId(@Param("allergenId") Integer allergenId);

    @Query("SELECT ua FROM UserAllergy ua WHERE ua.user.id = :userId AND " +
            "ua.severity IN ('severe', 'anaphylaxis') AND ua.isActive = true")
    List<UserAllergy> findSevereAllergiesByUserId(@Param("userId") String userId);
}
