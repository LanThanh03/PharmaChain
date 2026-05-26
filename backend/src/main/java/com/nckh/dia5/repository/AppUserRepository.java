package com.nckh.dia5.repository;

import com.nckh.dia5.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for mobile app users
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByPhone(String phone);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByPhoneOrEmail(String phone, String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.phone = :identifier OR u.email = :identifier")
    Optional<AppUser> findByPhoneOrEmailSingle(@Param("identifier") String identifier);

    @Query("SELECT u FROM AppUser u WHERE u.isActive = true AND u.fcmToken IS NOT NULL")
    java.util.List<AppUser> findAllWithFcmToken();
}
