package com.nckh.dia5.repository;

import com.nckh.dia5.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Iterable<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since")
    Iterable<User> findUsersLoggedInSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    Long countNewUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT u FROM User u WHERE u.isProfileComplete = false")
    Iterable<User> findUsersWithIncompleteProfiles();
}
