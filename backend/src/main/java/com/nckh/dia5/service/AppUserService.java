package com.nckh.dia5.service;

import com.nckh.dia5.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing AppUser entities.
 * Handles auto-creation of app users when needed.
 * FK constraint has been removed from DB, so this is a best-effort approach.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final EntityManager entityManager;

    /**
     * Ensures the given userId exists in app_users table.
     * Since FK constraint has been removed, this is now a best-effort operation.
     * If user doesn't exist, we create it automatically.
     */
    @Transactional
    public void ensureAppUserExists(Long userId) {
        if (userId == null) {
            log.warn("userId is null, skipping user creation");
            return;
        }

        if (appUserRepository.existsById(userId)) {
            return; // Already exists
        }

        log.info("Auto-creating app_user with ID {}", userId);

        try {
            int rows = entityManager.createNativeQuery(
                    "INSERT IGNORE INTO app_users (id, email, full_name, is_active, created_at, updated_at) " +
                    "VALUES (:id, :email, :fullName, :isActive, NOW(), NOW())")
                .setParameter("id", userId)
                .setParameter("email", "user_mobile_" + userId + "@demo.com")
                .setParameter("fullName", "Người dùng " + userId)
                .setParameter("isActive", true)
                .executeUpdate();

            if (rows > 0) {
                log.info("Successfully created app_user with ID {}", userId);
            }
        } catch (Exception e) {
            // FK constraint removed - this is not critical anymore
            log.warn("Could not auto-create user {}: {} (non-critical)", userId, e.getMessage());
        }
    }
}
