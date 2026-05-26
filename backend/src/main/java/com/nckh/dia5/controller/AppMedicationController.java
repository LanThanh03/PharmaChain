package com.nckh.dia5.controller;

import com.nckh.dia5.dto.AddMedicationRequest;
import com.nckh.dia5.dto.DispenseInstructionDTO;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.MedicationReminder;
import com.nckh.dia5.model.UserMedicationRecord;
import com.nckh.dia5.service.DispenseInstructionService;
import com.nckh.dia5.service.MedicationRecordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Mobile App - Medication Management
 * 
 * Endpoints for:
 * - Getting dispense instructions when scanning QR
 * - Adding medications to user's profile
 * - Managing reminders
 * - Tracking adherence
 */
@Slf4j
@RestController
@RequestMapping("/api/app/medications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppMedicationController {

    private final MedicationRecordService medicationRecordService;
    private final DispenseInstructionService dispenseInstructionService;
    private final com.nckh.dia5.repository.DrugProductRepository drugProductRepository;

    // ============================================================
    // MEDICATION RECORDS
    // ============================================================

    /**
     * Get all active medications for a user
     * GET /api/app/medications?userId=123
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveMedications(
            @RequestParam Long userId) {

        try {
            log.info("Getting active medications for user {}", userId);

            var medications = medicationRecordService.getActiveMedications(userId);
            var response = medications.stream()
                    .map(this::mapRecordToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Danh sách thuốc đang sử dụng"));

        } catch (Exception e) {
            log.error("Error getting medications: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get all medications history for a user
     * GET /api/app/medications/history?userId=123
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMedicationHistory(
            @RequestParam Long userId) {

        try {
            var medications = medicationRecordService.getAllMedications(userId);
            var response = medications.stream()
                    .map(this::mapRecordToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Lịch sử thuốc đã dùng"));

        } catch (Exception e) {
            log.error("Error getting history: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get a single medication record
     * GET /api/app/medications/{recordId}
     */
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMedication(
            @PathVariable Long recordId) {

        try {
            return medicationRecordService.getMedication(recordId)
                    .map(record -> ResponseEntity.ok(
                            ApiResponse.success(mapRecordToResponse(record), "Chi tiết thuốc")))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("Không tìm thấy thuốc", 404)));

        } catch (Exception e) {
            log.error("Error getting medication: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Add medication from QR scan (item code)
     * POST /api/app/medications/add-from-qr
     */
    @PostMapping("/add-from-qr")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addFromQRScan(
            @RequestBody AddFromQRRequest request) {

        try {
            log.info("Adding medication from QR for user {}: {}", request.getUserId(), request.getItemCode());

            var result = medicationRecordService.addByItemCode(request.getUserId(), request.getItemCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("recordId", result.getRecordId());
            response.put("remindersCreated", result.getRemindersCreated());

            if (!result.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.getMessage(), 400));
            }

            return ResponseEntity.ok(ApiResponse.success(response, result.getMessage()));

        } catch (Exception e) {
            log.error("Error adding from QR: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Add medication manually
     * POST /api/app/medications/add-manual
     */
    @PostMapping("/add-manual")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addManually(
            @RequestBody AddMedicationRequest request) {

        try {
            log.info("Adding medication manually for user {}: {}", request.getUserId(), request.getDrugName());

            var result = medicationRecordService.addManually(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("recordId", result.getRecordId());
            response.put("remindersCreated", result.getRemindersCreated());

            if (!result.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.getMessage(), 400));
            }

            return ResponseEntity.ok(ApiResponse.success(response, result.getMessage()));

        } catch (Exception e) {
            log.error("Error adding manually: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get instruction preview (before adding to profile)
     * GET /api/app/medications/preview/{itemCode}
     */
    @GetMapping("/preview/{itemCode}")
    public ResponseEntity<ApiResponse<DispenseInstructionDTO>> previewMedication(
            @PathVariable String itemCode) {

        try {
            log.info("Preview medication for item: {}", itemCode);

            return dispenseInstructionService.getInstructionByItemCode(itemCode)
                    .map(dto -> ResponseEntity.ok(
                            ApiResponse.success(dto, "Thông tin thuốc đã mua")))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error(
                                    "Không tìm thấy thông tin. Thuốc chưa được hiệu thuốc ghi hướng dẫn.", 404)));

        } catch (Exception e) {
            log.error("Error preview: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Pause a medication
     * PUT /api/app/medications/{recordId}/pause
     */
    @PutMapping("/{recordId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseMedication(
            @PathVariable Long recordId,
            @RequestBody(required = false) PauseRequest request) {

        try {
            String reason = request != null ? request.getReason() : null;
            boolean success = medicationRecordService.pauseMedication(recordId, reason);

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy thuốc", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã tạm dừng nhắc nhở"));

        } catch (Exception e) {
            log.error("Error pausing: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Resume a medication
     * PUT /api/app/medications/{recordId}/resume
     */
    @PutMapping("/{recordId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeMedication(@PathVariable Long recordId) {

        try {
            boolean success = medicationRecordService.resumeMedication(recordId);

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy thuốc", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã tiếp tục nhắc nhở"));

        } catch (Exception e) {
            log.error("Error resuming: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Update reminder times
     * PUT /api/app/medications/{recordId}/update-times
     */
    @PutMapping("/{recordId}/update-times")
    public ResponseEntity<ApiResponse<Void>> updateReminderTimes(
            @PathVariable Long recordId,
            @RequestBody UpdateTimesRequest request) {

        try {
            if (request.getTimes() == null || request.getTimes().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Danh sách giờ uống không được trống", 400));
            }
            
            boolean success = medicationRecordService.updateReminderTimes(recordId, request.getTimes());

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy thuốc", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật giờ uống thuốc"));

        } catch (Exception e) {
            log.error("Error updating times: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Mark medication as completed
     * PUT /api/app/medications/{recordId}/complete
     */
    @PutMapping("/{recordId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeMedication(@PathVariable Long recordId) {

        try {
            boolean success = medicationRecordService.completeMedication(recordId);

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy thuốc", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã hoàn thành liệu trình"));

        } catch (Exception e) {
            log.error("Error completing: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    // ============================================================
    // REMINDERS
    // ============================================================

    /**
     * Get all reminders for a specific record
     * GET /api/app/medications/records/{recordId}/reminders
     */
    @GetMapping("/records/{recordId}/reminders")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRemindersByRecord(
            @PathVariable Long recordId) {

        try {
            var reminders = medicationRecordService.getRemindersByRecordId(recordId);
            var response = reminders.stream()
                    .map(this::mapReminderToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Danh sách nhắc nhở của thuốc"));

        } catch (Exception e) {
            log.error("Error getting reminders for record: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get today's reminders for a user
     * GET /api/app/medications/reminders/today?userId=123
     */
    @GetMapping("/reminders/today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodayReminders(
            @RequestParam Long userId) {

        try {
            var reminders = medicationRecordService.getTodayReminders(userId);
            var response = reminders.stream()
                    .map(this::mapReminderToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Nhắc nhở hôm nay"));

        } catch (Exception e) {
            log.error("Error getting today reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get upcoming reminders (next 60 minutes)
     * GET /api/app/medications/reminders/upcoming?userId=123
     */
    @GetMapping("/reminders/upcoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUpcomingReminders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "60") int minutes) {

        try {
            var reminders = medicationRecordService.getUpcomingReminders(userId, minutes);
            var response = reminders.stream()
                    .map(this::mapReminderToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Nhắc nhở sắp tới"));

        } catch (Exception e) {
            log.error("Error getting upcoming reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Mark reminder as taken
     * PUT /api/app/medications/reminders/{reminderId}/taken
     */
    @PutMapping("/reminders/{reminderId}/taken")
    public ResponseEntity<ApiResponse<Void>> markAsTaken(
            @PathVariable Long reminderId,
            @RequestBody(required = false) TakenRequest request) {

        try {
            String notes = request != null ? request.getNotes() : null;
            boolean success = medicationRecordService.markReminderAsTaken(reminderId, notes);

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy nhắc nhở", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã đánh dấu uống thuốc"));

        } catch (Exception e) {
            log.error("Error marking as taken: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Mark reminder as skipped
     * PUT /api/app/medications/reminders/{reminderId}/skip
     */
    @PutMapping("/reminders/{reminderId}/skip")
    public ResponseEntity<ApiResponse<Void>> markAsSkipped(
            @PathVariable Long reminderId,
            @RequestBody(required = false) SkipRequest request) {

        try {
            String reason = request != null ? request.getReason() : "Không có lý do";
            boolean success = medicationRecordService.markReminderAsSkipped(reminderId, reason);

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy nhắc nhở", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã bỏ qua nhắc nhở"));

        } catch (Exception e) {
            log.error("Error marking as skipped: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Action reminder by recordId and time
     * PUT /api/app/medications/records/{recordId}/reminders/action
     */
    @PutMapping("/records/{recordId}/reminders/action")
    public ResponseEntity<ApiResponse<Void>> actionReminderByTime(
            @PathVariable Long recordId,
            @RequestBody ActionByTimeRequest request) {

        try {
            LocalDate date = request.getScheduledDate() != null ? request.getScheduledDate() : LocalDate.now();
            LocalTime time = request.getScheduledTime();
            String action = request.getAction(); // TAKEN or SKIPPED
            String notes = request.getNotes() != null ? request.getNotes() : "Action from background notification";

            boolean success = false;
            
            if ("TAKEN".equalsIgnoreCase(action)) {
                success = medicationRecordService.markReminderAsTakenByTime(recordId, date, time, notes);
            } else if ("SKIPPED".equalsIgnoreCase(action)) {
                success = medicationRecordService.markReminderAsSkippedByTime(recordId, date, time, notes);
            }

            if (!success) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Không tìm thấy nhắc nhở", 404));
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật nhắc nhở"));

        } catch (Exception e) {
            log.error("Error actioning reminder: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get today's summary for a user
     * GET /api/app/medications/summary/today?userId=123
     */
    @GetMapping("/summary/today")
    public ResponseEntity<ApiResponse<MedicationRecordService.TodaySummary>> getTodaySummary(
            @RequestParam Long userId) {

        try {
            var summary = medicationRecordService.getTodaySummary(userId);
            return ResponseEntity.ok(ApiResponse.success(summary, "Tổng kết hôm nay"));

        } catch (Exception e) {
            log.error("Error getting summary: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private Map<String, Object> mapRecordToResponse(UserMedicationRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("drugName", record.getDrugName());
        map.put("batchNumber", record.getBatchNumber());
        map.put("itemCode", record.getItemCode());
        map.put("manufacturer", record.getManufacturer());
        map.put("expiryDate", record.getExpiryDate());
        map.put("dosage", record.getDosage());
        map.put("frequency", record.getFrequency());
        map.put("mealRelation", record.getMealRelation() != null ? record.getMealRelation().name() : null);
        map.put("reminderTimes", record.getReminderTimesArray());
        map.put("startDate", record.getStartDate());
        map.put("endDate", record.getEndDate());
        map.put("totalDoses", record.getTotalDoses());
        map.put("takenDoses", record.getTakenDoses());
        map.put("missedDoses", record.getMissedDoses());
        map.put("remainingDoses", record.getRemainingDoses());
        map.put("adherenceRate", record.getAdherenceRate());
        map.put("daysRemaining", record.getDaysRemaining());
        map.put("isActive", record.getIsActive());
        map.put("isCompleted", record.getIsCompleted());
        map.put("isPaused", record.getIsPaused());
        map.put("isExpiringSoon", record.isExpiringSoon(30));
        map.put("pharmacyName", record.getPharmacyName());
        map.put("specialInstructions", record.getSpecialInstructions());
        map.put("createdAt", record.getCreatedAt());

        if (record.getProductItem() != null && record.getProductItem().getDrugProduct() != null) {
            com.nckh.dia5.model.DrugProduct dp = record.getProductItem().getDrugProduct();
            map.put("imageUrl", dp.getImageUrl());
            map.put("description", dp.getDescription());
            map.put("activeIngredient", dp.getActiveIngredient());
            map.put("category", dp.getCategory());
            map.put("drugDosage", dp.getDosage());
            map.put("unit", dp.getUnit());
            map.put("storageConditions", dp.getStorageConditions());
            map.put("shelfLife", dp.getShelfLife());
        } else if (record.getDrugName() != null && !record.getDrugName().isEmpty()) {
            java.util.List<com.nckh.dia5.model.DrugProduct> matches = drugProductRepository.findByName(record.getDrugName());
            if (matches != null && !matches.isEmpty()) {
                com.nckh.dia5.model.DrugProduct dp = matches.get(0);
                map.put("imageUrl", dp.getImageUrl());
                map.put("description", dp.getDescription());
                map.put("activeIngredient", dp.getActiveIngredient());
                map.put("category", dp.getCategory());
                map.put("drugDosage", dp.getDosage());
                map.put("unit", dp.getUnit());
                map.put("storageConditions", dp.getStorageConditions());
                map.put("shelfLife", dp.getShelfLife());
            }
        }

        return map;
    }

    private Map<String, Object> mapReminderToResponse(MedicationReminder reminder) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reminder.getId());
        map.put("recordId", reminder.getRecordId());
        map.put("scheduledDate", reminder.getScheduledDate());
        map.put("scheduledTime", reminder.getScheduledTime());
        map.put("status", reminder.getStatus().name());
        map.put("statusDisplay", reminder.getStatusDisplay());
        map.put("takenAt", reminder.getTakenAt());
        map.put("notes", reminder.getNotes());
        map.put("isPastDue", reminder.isPastDue());

        // Add drug info from medication record
        if (reminder.getMedicationRecord() != null) {
            map.put("drugName", reminder.getMedicationRecord().getDrugName());
            map.put("dosage", reminder.getMedicationRecord().getDosage());
            map.put("mealRelation", reminder.getMedicationRecord().getMealRelation() != null ?
                    reminder.getMedicationRecord().getMealRelation().name() : null);
        }

        return map;
    }

    // ============================================================
    // REQUEST CLASSES
    // ============================================================

    @Data
    public static class AddFromQRRequest {
        private Long userId;
        private String itemCode;
    }

    @Data
    public static class PauseRequest {
        private String reason;
    }

    @Data
    public static class TakenRequest {
        private String notes;
    }

    @Data
    public static class SkipRequest {
        private String reason;
    }

    @Data
    public static class ActionByTimeRequest {
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private String action; // TAKEN or SKIPPED
        private String notes;
    }

    @Data
    public static class UpdateTimesRequest {
        private String times;
    }
}
