package com.nckh.dia5.controller;

import com.nckh.dia5.model.RawMaterialBatch;
import com.nckh.dia5.service.RawMaterialBatchService;
import com.nckh.dia5.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/raw-materials")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RawMaterialBatchController {

    private final RawMaterialBatchService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllBatches(), "Lấy danh sách lô nguyên liệu thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createBatch(@Valid @RequestBody RawMaterialBatch batch) {
        try {
            RawMaterialBatch created = service.createBatch(batch);
            return ResponseEntity.ok(ApiResponse.success(created, "Tạo lô nguyên liệu thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            String notes = payload.get("notes");
            RawMaterialBatch updated = service.updateStatus(id, status, notes);
            return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật trạng thái thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBatch(@PathVariable Long id) {
        try {
            service.deleteBatch(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa lô nguyên liệu thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

