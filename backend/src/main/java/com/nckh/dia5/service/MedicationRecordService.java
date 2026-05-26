package com.nckh.dia5.service;

import com.nckh.dia5.dto.AddMedicationRequest;
import com.nckh.dia5.model.*;
import com.nckh.dia5.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user medication records and reminders
 * Handles the mobile app's medication tracking features
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationRecordService {

    private final UserMedicationRecordRepository recordRepository;
    private final MedicationReminderRepository reminderRepository;
    private final DispenseInstructionRepository instructionRepository;
    private final ProductItemRepository productItemRepository;
    private final AppUserRepository appUserRepository;
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final AuthService authService;
    private final AppUserService appUserService;

    /**
     * Add a medication to user's profile from a dispensed item (QR scan)
     */
    @Transactional
    public AddMedicationResult addFromDispenseInstruction(Long userId, Long dispenseInstructionId) {
        log.info("Adding medication from dispense instruction {} for user {}", dispenseInstructionId, userId);

        // Get dispense instruction
        Optional<DispenseInstruction> instructionOpt = instructionRepository.findById(dispenseInstructionId);
        if (instructionOpt.isEmpty()) {
            return AddMedicationResult.error("Không tìm thấy thông tin hướng dẫn sử dụng");
        }

        DispenseInstruction instruction = instructionOpt.get();

        // Ensure user exists
        appUserService.ensureAppUserExists(userId);

        // Check if already added
        if (recordRepository.existsByUserIdAndItemCodeAndIsActiveTrue(userId, instruction.getItemCode())) {
            return AddMedicationResult.error("Thuốc này đã có trong hồ sơ của bạn");
        }

        // Create medication record
        UserMedicationRecord record = createRecordFromInstruction(userId, instruction);
        UserMedicationRecord saved = recordRepository.save(record);

        // Generate reminders
        int remindersCreated = generateReminders(saved);

        log.info("Created medication record {} with {} reminders", saved.getId(), remindersCreated);

        return AddMedicationResult.success(saved.getId(), "Đã thêm thuốc vào hồ sơ", remindersCreated);
    }

    /**
     * Add a medication to user's profile by item code (QR scan)
     */
    @Transactional
    public AddMedicationResult addByItemCode(Long userId, String itemCode) {
        log.info("Adding medication by item code {} for user {}", itemCode, userId);

        // Find dispense instruction by item code
        Optional<DispenseInstruction> instructionOpt = instructionRepository.findByItemCode(itemCode);
        if (instructionOpt.isEmpty()) {
            return AddMedicationResult.error("Không tìm thấy thông tin thuốc đã mua. Vui lòng liên hệ hiệu thuốc.");
        }

        return addFromDispenseInstruction(userId, instructionOpt.get().getId());
    }

    /**
     * Add a medication manually (user enters info)
     */
    @Transactional
    public AddMedicationResult addManually(AddMedicationRequest request) {
        log.info("Adding medication manually for user {}: {}", request.getUserId(), request.getDrugName());

        appUserService.ensureAppUserExists(request.getUserId());

        // Create record from manual input
        UserMedicationRecord record = new UserMedicationRecord();
        record.setUserId(request.getUserId());
        record.setDrugName(request.getDrugName());
        record.setManufacturer(request.getManufacturer());
        record.setExpiryDate(request.getExpiryDate());
        record.setDosage(request.getDosage() != null ? request.getDosage() : "1 viên");
        record.setFrequency(request.getFrequency() != null ? request.getFrequency() : 3);
        record.setMealRelation(request.getMealRelationEnum());
        record.setReminderTimes(request.getReminderTimes() != null ? request.getReminderTimes() : "08:00,12:00,20:00");
        record.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        record.setEndDate(request.getEndDate());
        record.setNotes(request.getNotes());
        record.setIsActive(true);

        UserMedicationRecord saved = recordRepository.save(record);

        // Generate reminders
        int remindersCreated = generateReminders(saved);

        return AddMedicationResult.success(saved.getId(), "Đã thêm thuốc vào hồ sơ", remindersCreated);
    }

    /**
     * Get all active medications for a user
     */
    @Transactional(readOnly = true)
    public List<UserMedicationRecord> getActiveMedications(Long userId) {
        return recordRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get all medications for a user (history)
     */
    @Transactional(readOnly = true)
    public List<UserMedicationRecord> getAllMedications(Long userId) {
        return recordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get medication by ID
     */
    public Optional<UserMedicationRecord> getMedication(Long recordId) {
        return recordRepository.findById(recordId);
    }

    /**
     * Get all reminders for a specific record
     */
    @Transactional(readOnly = true)
    public List<MedicationReminder> getRemindersByRecordId(Long recordId) {
        return reminderRepository.findByRecordIdOrderByScheduledDateDescScheduledTimeDesc(recordId);
    }

    /**
     * Get today's reminders for a user
     */
    public List<MedicationReminder> getTodayReminders(Long userId) {
        return reminderRepository.findTodayByUserId(userId);
    }

    /**
     * Get upcoming reminders for a user (within next X minutes)
     */
    public List<MedicationReminder> getUpcomingReminders(Long userId, int minutes) {
        LocalTime now = LocalTime.now();
        return reminderRepository.findUpcomingByUserId(userId, now, now.plusMinutes(minutes));
    }

    /**
     * Mark a reminder as taken
     */
    @Transactional
    public boolean markReminderAsTaken(Long reminderId, String notes) {
        Optional<MedicationReminder> reminderOpt = reminderRepository.findById(reminderId);
        if (reminderOpt.isEmpty()) {
            return false;
        }

        MedicationReminder reminder = reminderOpt.get();
        reminder.markAsTaken(notes);
        reminderRepository.save(reminder);

        // Note: trigger will update the medication record's taken_doses

        log.info("Marked reminder {} as taken", reminderId);
        return true;
    }

    /**
     * Mark a reminder as skipped
     */
    @Transactional
    public boolean markReminderAsSkipped(Long reminderId, String reason) {
        Optional<MedicationReminder> reminderOpt = reminderRepository.findById(reminderId);
        if (reminderOpt.isEmpty()) {
            return false;
        }

        MedicationReminder reminder = reminderOpt.get();
        reminder.markAsSkipped(reason);
        reminderRepository.save(reminder);

        log.info("Marked reminder {} as skipped: {}", reminderId, reason);
        return true;
    }

    @Transactional
    public boolean markReminderAsTakenByTime(Long recordId, LocalDate date, LocalTime time, String notes) {
        Optional<MedicationReminder> reminderOpt = reminderRepository.findByRecordIdAndScheduledDateAndScheduledTime(recordId, date, time);
        if (reminderOpt.isEmpty()) {
            return false;
        }

        MedicationReminder reminder = reminderOpt.get();
        reminder.markAsTaken(notes);
        reminderRepository.save(reminder);

        log.info("Marked reminder {} as taken by time", reminder.getId());
        return true;
    }

    @Transactional
    public boolean markReminderAsSkippedByTime(Long recordId, LocalDate date, LocalTime time, String reason) {
        Optional<MedicationReminder> reminderOpt = reminderRepository.findByRecordIdAndScheduledDateAndScheduledTime(recordId, date, time);
        if (reminderOpt.isEmpty()) {
            return false;
        }

        MedicationReminder reminder = reminderOpt.get();
        reminder.markAsSkipped(reason);
        reminderRepository.save(reminder);

        log.info("Marked reminder {} as skipped by time: {}", reminder.getId(), reason);
        return true;
    }

    /**
     * Pause a medication
     */
    @Transactional
    public boolean pauseMedication(Long recordId, String reason) {
        Optional<UserMedicationRecord> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        UserMedicationRecord record = recordOpt.get();
        record.setIsPaused(true);
        record.setPauseReason(reason);
        recordRepository.save(record);

        log.info("Paused medication {}: {}", recordId, reason);
        return true;
    }

    /**
     * Resume a medication
     */
    @Transactional
    public boolean resumeMedication(Long recordId) {
        Optional<UserMedicationRecord> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        UserMedicationRecord record = recordOpt.get();
        record.setIsPaused(false);
        record.setPauseReason(null);
        recordRepository.save(record);

        log.info("Resumed medication {}", recordId);
        return true;
    }

    /**
     * Update reminder times for a medication
     */
    @Transactional
    public boolean updateReminderTimes(Long recordId, String newTimes) {
        Optional<UserMedicationRecord> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        UserMedicationRecord record = recordOpt.get();
        record.setReminderTimes(newTimes);
        recordRepository.save(record);

        // Delete all future pending reminders
        reminderRepository.deleteByRecordIdAndStatus(recordId, MedicationReminder.ReminderStatus.PENDING);

        // Regenerate from today to end date
        if (record.getEndDate() == null) {
            record.setEndDate(LocalDate.now().plusDays(30)); // default fallback
        }
        
        LocalDate currentDate = LocalDate.now();
        if (record.getEndDate().isBefore(currentDate)) {
            return true; // Already ended, no new reminders
        }
        
        String[] times = newTimes.split(",");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<MedicationReminder> newReminders = new ArrayList<>();
        
        while (!currentDate.isAfter(record.getEndDate())) {
            for (String timeStr : times) {
                try {
                    LocalTime time = LocalTime.parse(timeStr.trim(), timeFormatter);
                    
                    // Skip times in the past for today
                    if (currentDate.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
                        continue;
                    }

                    MedicationReminder reminder = new MedicationReminder();
                    reminder.setRecordId(record.getId());
                    reminder.setScheduledDate(currentDate);
                    reminder.setScheduledTime(time);
                    reminder.setStatus(MedicationReminder.ReminderStatus.PENDING);

                    newReminders.add(reminder);
                } catch (Exception e) {
                    log.warn("Invalid time format: {}", timeStr);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        if (!newReminders.isEmpty()) {
            reminderRepository.saveAll(newReminders);
            
            // Recount total doses
            long takenDoses = reminderRepository.countByRecordIdAndStatus(recordId, MedicationReminder.ReminderStatus.TAKEN);
            long missedDoses = reminderRepository.countByRecordIdAndStatus(recordId, MedicationReminder.ReminderStatus.MISSED);
            long skippedDoses = reminderRepository.countByRecordIdAndStatus(recordId, MedicationReminder.ReminderStatus.SKIPPED);
            long totalDoses = takenDoses + missedDoses + skippedDoses + newReminders.size();
            
            record.setTotalDoses((int)totalDoses);
            recordRepository.save(record);
        }

        log.info("Updated reminder times for medication {} to {}", recordId, newTimes);
        return true;
    }

    /**
     * Complete a medication (finished the course)
     */
    @Transactional
    public boolean completeMedication(Long recordId) {
        Optional<UserMedicationRecord> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        UserMedicationRecord record = recordOpt.get();
        record.setIsActive(false);
        record.setIsCompleted(true);
        recordRepository.save(record);

        log.info("Completed medication {}", recordId);
        return true;
    }

    /**
     * Get today's summary for a user
     */
    public TodaySummary getTodaySummary(Long userId) {
        long taken = reminderRepository.countTodayTakenByUserId(userId);
        long pending = reminderRepository.countTodayPendingByUserId(userId);
        List<MedicationReminder> todayReminders = reminderRepository.findTodayByUserId(userId);

        int total = todayReminders.size();
        long missed = todayReminders.stream()
                .filter(r -> r.getStatus() == MedicationReminder.ReminderStatus.MISSED)
                .count();

        double adherence = total > 0 ? (taken * 100.0 / total) : 100.0;

        return TodaySummary.builder()
                .totalReminders((int) total)
                .takenCount((int) taken)
                .pendingCount((int) pending)
                .missedCount((int) missed)
                .adherencePercentage(adherence)
                .build();
    }

    // =========== Private Helper Methods ===========

    private UserMedicationRecord createRecordFromInstruction(Long userId, DispenseInstruction instruction) {
        UserMedicationRecord record = new UserMedicationRecord();
        record.setUserId(userId);
        record.setProductItemId(instruction.getProductItemId());
        record.setDispenseInstructionId(instruction.getId());

        // Drug info
        record.setDrugName(instruction.getDrugName());
        record.setBatchNumber(instruction.getBatchNumber());
        record.setItemCode(instruction.getItemCode());

        // Get manufacturer and expiry from product item
        productItemRepository.findById(instruction.getProductItemId()).ifPresent(item -> {
            record.setManufacturer(item.getDrugBatch().getManufacturer());
            record.setExpiryDate(item.getExpiryDate().toLocalDate());
        });

        // Schedule from instruction
        record.setDosage(instruction.getDosage());
        record.setFrequency(instruction.getFrequency());
        record.setMealRelation(instruction.getMealRelation());
        record.setReminderTimes(instruction.getSpecificTimes());
        record.setStartDate(LocalDate.now());

        // Calculate end date
        if (instruction.getDurationDays() != null && instruction.getDurationDays() > 0) {
            record.setEndDate(LocalDate.now().plusDays(instruction.getDurationDays() - 1));
        }

        // Special instructions
        record.setSpecialInstructions(instruction.getSpecialNotes());

        // Pharmacy info
        pharmaCompanyRepository.findById(instruction.getPharmacyId()).ifPresent(pharmacy -> {
            record.setPharmacyName(pharmacy.getName());
            record.setPharmacyId(pharmacy.getId());
        });
        record.setPurchasedAt(instruction.getDispensedAt());

        record.setIsActive(true);

        return record;
    }

    private int generateReminders(UserMedicationRecord record) {
        if (record.getStartDate() == null || record.getReminderTimes() == null) {
            return 0;
        }

        LocalDate endDate = record.getEndDate() != null ? record.getEndDate() : record.getStartDate().plusDays(6);
        String[] times = record.getReminderTimes().split(",");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<MedicationReminder> reminders = new ArrayList<>();
        LocalDate currentDate = record.getStartDate();

        while (!currentDate.isAfter(endDate)) {
            for (String timeStr : times) {
                try {
                    LocalTime time = LocalTime.parse(timeStr.trim(), timeFormatter);

                    MedicationReminder reminder = new MedicationReminder();
                    reminder.setRecordId(record.getId());
                    reminder.setScheduledDate(currentDate);
                    reminder.setScheduledTime(time);
                    reminder.setStatus(MedicationReminder.ReminderStatus.PENDING);

                    reminders.add(reminder);
                } catch (Exception e) {
                    log.warn("Invalid time format: {}", timeStr);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        if (!reminders.isEmpty()) {
            reminderRepository.saveAll(reminders);

            // Update total doses
            record.setTotalDoses(reminders.size());
            recordRepository.save(record);
        }

        return reminders.size();
    }

    // =========== Result Classes ===========

    @Data
    @lombok.Builder
    public static class AddMedicationResult {
        private boolean success;
        private String message;
        private Long recordId;
        private int remindersCreated;

        public static AddMedicationResult success(Long recordId, String message, int reminders) {
            return AddMedicationResult.builder()
                    .success(true)
                    .message(message)
                    .recordId(recordId)
                    .remindersCreated(reminders)
                    .build();
        }

        public static AddMedicationResult error(String message) {
            return AddMedicationResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    @Data
    @lombok.Builder
    public static class TodaySummary {
        private int totalReminders;
        private int takenCount;
        private int pendingCount;
        private int missedCount;
        private double adherencePercentage;
    }
}
