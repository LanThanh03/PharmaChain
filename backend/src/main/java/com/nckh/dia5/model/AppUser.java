package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for mobile app users (consumers)
 * These are end-users who scan QR codes and track their medications
 */
@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "medicationRecords"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 20)
    @Column(name = "phone", unique = true)
    private String phone;

    @Size(max = 100)
    @Column(name = "email", unique = true)
    private String email;

    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;

    @Size(max = 100)
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Size(max = 500)
    @Column(name = "avatar_url")
    private String avatarUrl;

    // Medical information
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies; // JSON array

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions; // JSON array

    @Size(max = 5)
    @Column(name = "blood_type")
    private String bloodType;

    // Settings
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Size(max = 50)
    @Column(name = "reminder_sound")
    private String reminderSound = "default";

    @Size(max = 10)
    @Column(name = "language")
    private String language = "vi";

    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Push notification
    @Size(max = 500)
    @Column(name = "fcm_token")
    private String fcmToken;

    @Size(max = 500)
    @Column(name = "device_info")
    private String deviceInfo;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Note: Removed @OneToMany relationship to UserMedicationRecord
    // to avoid FK constraint issues. Records are queried by userId at application level.

    // Enums
    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
