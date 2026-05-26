package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity for medication reminders - tracks each scheduled dose and its status
 * Used for push notifications and adherence tracking
 */
@Entity
@Table(name = "medication_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MedicationReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    // Scheduled time
    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @NotNull
    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    // Status
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;

    // When taken
    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "skip_reason")
    private String skipReason;

    // Push notification tracking
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "reminders"})
    private UserMedicationRecord medicationRecord;

    // Enums
    public enum ReminderStatus {
        PENDING,    // Chưa đến giờ
        NOTIFIED,   // Đã gửi thông báo
        TAKEN,      // Đã uống
        MISSED,     // Bỏ lỡ
        SKIPPED     // Bỏ qua (có lý do)
    }

    // Helper methods
    public boolean isPastDue() {
        LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);
        return LocalDateTime.now().isAfter(scheduledDateTime) && status == ReminderStatus.PENDING;
    }

    public boolean isUpcoming(int minutesAhead) {
        LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);
        LocalDateTime now = LocalDateTime.now();
        return scheduledDateTime.isAfter(now) && 
               scheduledDateTime.isBefore(now.plusMinutes(minutesAhead));
    }

    public void markAsTaken(String notes) {
        this.status = ReminderStatus.TAKEN;
        this.takenAt = LocalDateTime.now();
        this.notes = notes;
        
        // Calculate response time
        LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(scheduledDateTime, this.takenAt);
        this.responseTimeMinutes = (int) minutes;
    }

    public void markAsMissed() {
        this.status = ReminderStatus.MISSED;
    }

    public void markAsSkipped(String reason) {
        this.status = ReminderStatus.SKIPPED;
        this.skipReason = reason;
    }

    public String getStatusDisplay() {
        return switch (status) {
            case PENDING -> "Chờ";
            case NOTIFIED -> "Đã nhắc";
            case TAKEN -> "Đã uống";
            case MISSED -> "Bỏ lỡ";
            case SKIPPED -> "Bỏ qua";
        };
    }
}
