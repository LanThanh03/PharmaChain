package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.service.DispenseService;
import com.nckh.dia5.util.RawInputCleaner;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pharmacy dispensing endpoint (DB-first + async blockchain)
 *
 * Flow:
 * - BƯỚC 1: Nhân viên quét QR -> parse itemCode
 * - BƯỚC 2: Query DATABASE (<10ms via uk_item_code)
 * - BƯỚC 3: Kiểm tra trạng thái -> nếu SOLD: cảnh báo; else: OK
 * - BƯỚC 4: Update DATABASE (SYNC): status = SOLD, insert movement SALE
 * - BƯỚC 5: Ghi blockchain (ASYNC): recordItemSale(), emit ItemSold
 */
@Slf4j
@RestController
@RequestMapping("/api/pharmacy/dispense")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PharmacyDispenseController {

    private final DispenseService dispenseService;
    private final RawInputCleaner rawInputCleaner;

    /**
     * Dispense item to consumer
     * POST /api/pharmacy/dispense
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> dispense(@RequestBody DispenseRequest request) {
        try {
            if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu mã sản phẩm (itemCode)", 400));
            }

            String itemCode = request.getItemCode();
            
            // Auto-clean corrupt input
            if (rawInputCleaner.isCorrupt(itemCode)) {
                log.warn("Detected corrupt input, cleaning...");
                List<String> extracted = rawInputCleaner.extractItemCodes(itemCode);
                if (!extracted.isEmpty()) {
                    itemCode = extracted.get(0); // Use first extracted code
                    log.info("Cleaned itemCode: {} -> {}", request.getItemCode(), itemCode);
                } else {
                    log.error("Failed to extract valid item code from corrupt input");
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Không thể đọc mã sản phẩm. Vui lòng quét lại QR code.", 400));
                }
            }

            log.info("Pharmacy dispense request: itemCode={}, customer={}",
                    itemCode, request.getCustomerPhone());

            DispenseService.DispenseResult result =
                    dispenseService.dispenseItem(itemCode, request.getCustomerPhone(), request.getNotes());

            Map<String, Object> payload = new HashMap<>();
            payload.put("itemCode", result.getItemCode());
            payload.put("currentStatus", result.getCurrentStatus());
            payload.put("message", result.getStatusMessage());
            payload.put("movementId", result.getMovementId());
            payload.put("alreadyDispensed", result.isAlreadyDispensed());

            if (!result.isSuccess()) {
                // CẢNH BÁO: Thuốc đã bán!
                return ResponseEntity.status(409).body(ApiResponse.error("Thuốc đã bán!", 409));
            }

            return ResponseEntity.ok(ApiResponse.success(payload, "Dispense thành công"));

        } catch (Exception e) {
            log.error("Dispense error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Quick status check for UI (optional helper)
     * GET /api/pharmacy/dispense/check/{itemCode}
     */
    @GetMapping("/check/{itemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(@PathVariable String itemCode) {
        try {
            DispenseService.DispenseResult result =
                    dispenseService.checkItemStatus(itemCode);

            Map<String, Object> payload = new HashMap<>();
            payload.put("itemCode", result.getItemCode());
            payload.put("currentStatus", result.getCurrentStatus());
            payload.put("message", result.getStatusMessage());
            payload.put("movementId", result.getMovementId());
            payload.put("alreadyDispensed", result.isAlreadyDispensed());

            if (!result.isSuccess() && result.isAlreadyDispensed()) {
                return ResponseEntity.status(409).body(ApiResponse.error("Thuốc đã bán!", 409));
            }
            return ResponseEntity.ok(ApiResponse.success(payload, "Kiểm tra trạng thái thành công"));
        } catch (Exception e) {
            log.error("Check status error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    @Data
    public static class DispenseRequest {
        private String itemCode;
        private String customerPhone; // optional
        private String notes;         // optional
    }
}