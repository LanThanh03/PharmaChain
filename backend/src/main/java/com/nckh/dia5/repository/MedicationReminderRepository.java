package com.nckh.dia5.repository;

import com.nckh.dia5.model.MedicationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository for medication reminders (scheduled doses)
 */
@Repository
public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {

    // Find by record
    List<MedicationReminder> findByRecordIdOrderByScheduledDateDescScheduledTimeDesc(Long recordId);

    // Find by record (ascending)
    List<MedicationReminder> findByRecordIdOrderByScheduledDateAscScheduledTimeAsc(Long recordId);

    // Find by record and date
    List<MedicationReminder> findByRecordIdAndScheduledDate(Long recordId, LocalDate date);

    // Find by record, date, and time
    java.util.Optional<MedicationReminder> findByRecordIdAndScheduledDateAndScheduledTime(Long recordId, LocalDate date, LocalTime time);

    // Find today's reminders for a record
    @Query("SELECT r FROM MedicationReminder r WHERE r.recordId = :recordId " +
           "AND r.scheduledDate = CURRENT_DATE ORDER BY r.scheduledTime")
    List<MedicationReminder> findTodayByRecord(@Param("recordId") Long recordId);

    // Find all today's reminders for a user (via medication records)
    @Query("SELECT r FROM MedicationReminder r JOIN UserMedicationRecord m ON r.recordId = m.id " +
           "WHERE m.userId = :userId AND r.scheduledDate = CURRENT_DATE ORDER BY r.scheduledTime")
    List<MedicationReminder> findTodayByUserId(@Param("userId") Long userId);

    // Find pending reminders that are due (for notification sending)
    @Query("SELECT r FROM MedicationReminder r WHERE r.status = 'PENDING' " +
           "AND r.scheduledDate = :date AND r.scheduledTime <= :time " +
           "AND r.notificationSent = false")
    List<MedicationReminder> findDuePendingReminders(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    // Find upcoming reminders (within next X minutes)
    @Query("SELECT r FROM MedicationReminder r JOIN UserMedicationRecord m ON r.recordId = m.id " +
           "WHERE m.userId = :userId AND r.status = 'PENDING' " +
           "AND r.scheduledDate = CURRENT_DATE AND r.scheduledTime BETWEEN :fromTime AND :toTime")
    List<MedicationReminder> findUpcomingByUserId(
            @Param("userId") Long userId,
            @Param("fromTime") LocalTime fromTime,
            @Param("toTime") LocalTime toTime);

    // Find missed reminders (pending but past due)
    @Query("SELECT r FROM MedicationReminder r WHERE r.status = 'PENDING' " +
           "AND (r.scheduledDate < :date OR (r.scheduledDate = :date AND r.scheduledTime < :time))")
    List<MedicationReminder> findMissedReminders(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    // Count by status for a record
    long countByRecordIdAndStatus(Long recordId, MedicationReminder.ReminderStatus status);

    // Count today's taken for user
    @Query("SELECT COUNT(r) FROM MedicationReminder r JOIN UserMedicationRecord m ON r.recordId = m.id " +
           "WHERE m.userId = :userId AND r.scheduledDate = CURRENT_DATE AND r.status = 'TAKEN'")
    long countTodayTakenByUserId(@Param("userId") Long userId);

    // Count today's pending for user
    @Query("SELECT COUNT(r) FROM MedicationReminder r JOIN UserMedicationRecord m ON r.recordId = m.id " +
           "WHERE m.userId = :userId AND r.scheduledDate = CURRENT_DATE AND r.status = 'PENDING'")
    long countTodayPendingByUserId(@Param("userId") Long userId);

    // Mark old pending reminders as missed
    @Modifying
    @Query("UPDATE MedicationReminder r SET r.status = 'MISSED', r.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE r.status = 'PENDING' " +
           "AND (r.scheduledDate < :date OR (r.scheduledDate = :date AND r.scheduledTime < :cutoffTime))")
    int markMissedReminders(
            @Param("date") LocalDate date,
            @Param("cutoffTime") LocalTime cutoffTime);

    // Delete reminders for a medication record
    void deleteByRecordId(Long recordId);

    // Delete reminders by record ID and status
    void deleteByRecordIdAndStatus(Long recordId, MedicationReminder.ReminderStatus status);

    // Find reminders needing notification (for batch processing)
    @Query("SELECT r FROM MedicationReminder r JOIN UserMedicationRecord m ON r.recordId = m.id " +
           "JOIN AppUser u ON m.userId = u.id " +
           "WHERE r.status = 'PENDING' AND r.notificationSent = false " +
           "AND r.scheduledDate = :date AND r.scheduledTime BETWEEN :fromTime AND :toTime " +
           "AND u.fcmToken IS NOT NULL AND u.notificationEnabled = true")
    List<MedicationReminder> findRemindersToNotify(
            @Param("date") LocalDate date,
            @Param("fromTime") LocalTime fromTime,
            @Param("toTime") LocalTime toTime);
}
