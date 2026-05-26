package com.nckh.dia5.controller;

import com.nckh.dia5.dto.DispenseInstructionDTO;
import com.nckh.dia5.dto.DispenseWithInstructionsRequest;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.service.DispenseInstructionService;
import com.nckh.dia5.util.RawInputCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for dispensing items WITH usage instructions
 * 
 * New endpoint that extends the basic dispense flow:
 * - BƯỚC 1: Quét QR -> parse itemCode
 * - BƯỚC 2: Nhập thông tin khách hàng & hướng dẫn sử dụng
 * - BƯỚC 3: Dispense (update DB + blockchain)
 * - BƯỚC 4: Lưu hướng dẫn sử dụng vào dispense_instructions
 */
@Slf4j
@RestController
@RequestMapping("/api/pharmacy/dispense-with-instructions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DispenseWithInstructionsController {

    private final DispenseInstructionService dispenseInstructionService;
    private final RawInputCleaner rawInputCleaner;

    /**
     * Dispense item with full usage instructions
     * POST /api/pharmacy/dispense-with-instructions
     * 
     * This is the enhanced version of the basic dispense endpoint
     * Called when pharmacist wants to provide detailed usage instructions
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> dispenseWithInstructions(
            @RequestBody DispenseWithInstructionsRequest request,
            @RequestHeader(value = "X-Pharmacy-Id", required = false) Long pharmacyId,
            @RequestHeader(value = "X-Pharmacy-Name", required = false) String pharmacyName) {

        try {
            // Validate request
            if (request == null || !request.isValid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Thiếu mã sản phẩm (itemCode)", 400));
            }

            String itemCode = request.getItemCode();

            // Auto-clean corrupt input
            if (rawInputCleaner.isCorrupt(itemCode)) {
                log.warn("Detected corrupt input, cleaning...");
                List<String> extracted = rawInputCleaner.extractItemCodes(itemCode);
                if (!extracted.isEmpty()) {
                    itemCode = extracted.get(0);
                    request.setItemCode(itemCode);
                    log.info("Cleaned itemCode: {}", itemCode);
                } else {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Không thể đọc mã sản phẩm. Vui lòng quét lại QR code.", 400));
                }
            }

            // Get pharmacy info from headers or session
            if (pharmacyId == null) {
                pharmacyId = 1L; // Default for testing
            }
            if (pharmacyName == null || pharmacyName.isBlank()) {
                pharmacyName = "Hiệu thuốc";
            }

            log.info("Dispense with instructions: itemCode={}, customer={}, pharmacyId={}",
                    itemCode, request.getCustomerPhone(), pharmacyId);

            // Perform dispense with instructions
            DispenseInstructionService.DispenseResult result =
                    dispenseInstructionService.dispenseWithInstructions(request, pharmacyId, pharmacyName);

            // Build response
            Map<String, Object> payload = new HashMap<>();
            payload.put("success", result.isSuccess());
            payload.put("itemCode", result.getItemCode());
            payload.put("drugName", result.getDrugName());
            payload.put("message", result.getMessage());
            payload.put("instructionId", result.getInstructionId());
            payload.put("alreadyDispensed", result.isAlreadyDispensed());
            if (result.getTransactionHash() != null) {
                payload.put("transactionHash", result.getTransactionHash());
            }

            if (!result.isSuccess()) {
                if (result.isAlreadyDispensed()) {
                    return ResponseEntity.status(409)
                            .body(ApiResponse.error("Thuốc đã bán!", 409));
                }
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.getMessage(), 400));
            }

            return ResponseEntity.ok(ApiResponse.success(payload, result.getMessage()));

        } catch (Exception e) {
            log.error("Dispense with instructions error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get instruction by item code (for mobile app QR scan)
     * GET /api/pharmacy/dispense-with-instructions/by-item/{itemCode}
     * 
     * Called when a consumer scans the QR code of a drug they purchased
     */
    @GetMapping("/by-item/{itemCode}")
    public ResponseEntity<ApiResponse<DispenseInstructionDTO>> getInstructionByItemCode(
            @PathVariable String itemCode) {

        try {
            // Clean item code if needed
            if (rawInputCleaner.isCorrupt(itemCode)) {
                List<String> extracted = rawInputCleaner.extractItemCodes(itemCode);
                if (!extracted.isEmpty()) {
                    itemCode = extracted.get(0);
                }
            }

            log.info("Getting instruction for item: {}", itemCode);

            return dispenseInstructionService.getInstructionByItemCode(itemCode)
                    .map(dto -> ResponseEntity.ok(ApiResponse.success(dto, "Tìm thấy hướng dẫn sử dụng")))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("Không tìm thấy thông tin hướng dẫn cho sản phẩm này", 404)));

        } catch (Exception e) {
            log.error("Get instruction error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get instructions by customer phone
     * GET /api/pharmacy/dispense-with-instructions/by-customer?phone=0901234567
     * 
     * Called by mobile app to get all medications purchased by a customer
     */
    @GetMapping("/by-customer")
    public ResponseEntity<ApiResponse<List<DispenseInstructionDTO>>> getByCustomerPhone(
            @RequestParam String phone) {

        try {
            log.info("Getting instructions for customer: {}", phone);

            var instructions = dispenseInstructionService.getInstructionsByCustomerPhone(phone);
            var dtos = instructions.stream()
                    .map(DispenseInstructionDTO::fromEntity)
                    .toList();

            return ResponseEntity.ok(
                    ApiResponse.success(dtos, "Tìm thấy " + dtos.size() + " đơn thuốc"));

        } catch (Exception e) {
            log.error("Get by customer error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Get today's dispense statistics for pharmacy
     * GET /api/pharmacy/dispense-with-instructions/stats/today
     */
    @GetMapping("/stats/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayStats(
            @RequestHeader(value = "X-Pharmacy-Id", required = false) Long pharmacyId) {

        try {
            if (pharmacyId == null) {
                pharmacyId = 1L;
            }

            var instructions = dispenseInstructionService.getInstructionsByPharmacy(pharmacyId);
            
            // Filter to today only
            var today = java.time.LocalDate.now();
            var todayInstructions = instructions.stream()
                    .filter(i -> i.getDispensedAt() != null && 
                                i.getDispensedAt().toLocalDate().equals(today))
                    .toList();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDispensed", todayInstructions.size());
            stats.put("withInstructions", todayInstructions.stream()
                    .filter(i -> i.getDosage() != null)
                    .count());
            stats.put("withCustomerInfo", todayInstructions.stream()
                    .filter(i -> i.getCustomerPhone() != null && !i.getCustomerPhone().isBlank())
                    .count());

            return ResponseEntity.ok(ApiResponse.success(stats, "Thống kê hôm nay"));

        } catch (Exception e) {
            log.error("Get stats error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }
}
