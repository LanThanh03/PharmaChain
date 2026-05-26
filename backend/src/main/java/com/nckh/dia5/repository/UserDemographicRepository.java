package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserDemographic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDemographicRepository extends JpaRepository<UserDemographic, Integer> {

    Optional<UserDemographic> findByUserId(String userId);

    @Query("SELECT ud FROM UserDemographic ud WHERE ud.user.id = :userId")
    Optional<UserDemographic> findByUserIdWithJoin(@Param("userId") String userId);

    @Query("SELECT COUNT(ud) FROM UserDemographic ud WHERE ud.gender = :gender")
    Long countByGender(@Param("gender") UserDemographic.Gender gender);

    @Query("SELECT COUNT(ud) FROM UserDemographic ud WHERE ud.birthYear BETWEEN :startYear AND :endYear")
    Long countByAgeRange(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);

    @Query("SELECT COUNT(ud) FROM UserDemographic ud WHERE ud.province.id = :provinceId")
    Long countByProvince(@Param("provinceId") Integer provinceId);
}
