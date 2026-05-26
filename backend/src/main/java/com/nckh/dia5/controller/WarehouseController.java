package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

/**
 * Controller quản lý kho hàng (Warehouse)
 * Dùng cho Nhà Sản Xuất, Nhà Phân Phối, Hiệu Thuốc
 */
@Slf4j
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@Tag(name = "Warehouse", description = "APIs quản lý kho hàng tích hợp blockchain")
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * ✅ LẤY DANH SÁCH HÀNG TRONG KHO
     * GET /api/warehouse/inventory?wallet=0x...
     * GET /api/warehouse/inventory (không cần wallet - lấy tất cả)
     * 
     * Dùng để hiển thị màn hình "Danh sách hàng trong kho"
     */
    @GetMapping("/inventory")
    @Operation(
        summary = "Lấy danh sách hàng trong kho",
        description = "Lấy tất cả lô hàng đang sở hữu theo địa chỉ ví blockchain. Wallet là optional."
    )
    public ResponseEntity<ApiResponse<List<DrugBatch>>> getInventory(
            @RequestParam(required = false) String wallet) {
        
        log.info("📦 API: Get inventory for wallet: {}", wallet);
        
        try {
            List<DrugBatch> inventory = warehouseService.getWarehouseInventory(wallet);
            
            log.info("✅ Found {} batches", inventory.size());
            
            return ResponseEntity.ok(
                ApiResponse.success(
                    inventory,
                    String.format("Tìm thấy %d lô hàng trong kho", inventory.size())
                )
            );
        } catch (Exception e) {
            log.error("❌ Failed to get inventory", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi lấy danh sách kho: " + e.getMessage())
            );
        }
    }

    /**
     * ✅ LẤY DANH SÁCH HÀNG CÓ THỂ XUẤT KHO
     * GET /api/warehouse/exportable?wallet=0x...
     * GET /api/warehouse/exportable (không cần wallet - lấy tất cả)
     * 
     * Dùng cho màn hình "Xuất kho" - chỉ hiển thị những lô có thể xuất
     */
    @GetMapping("/exportable")
    @Operation(
        summary = "Lấy danh sách hàng có thể xuất kho",
        description = "Lấy các lô hàng đủ điều kiện xuất kho (có số lượng, chưa hết hạn). Wallet là optional."
    )
    public ResponseEntity<ApiResponse<List<DrugBatch>>> getExportableBatches(
            @RequestParam(required = false) String wallet) {
        
        log.info("📤 API: Get exportable batches for wallet: {}", wallet);
        
        try {
            List<DrugBatch> exportable = warehouseService.getExportableBatches(wallet);
            
            log.info("✅ Found {} exportable batches", exportable.size());
            
            return ResponseEntity.ok(
                ApiResponse.success(
                    exportable,
                    String.format("Tìm thấy %d lô hàng có thể xuất kho", exportable.size())
                )
            );
        } catch (Exception e) {
            log.error("❌ Failed to get exportable batches", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi lấy danh sách hàng xuất kho: " + e.getMessage())
            );
        }
    }

    /**
     * ✅ KIỂM TRA CÓ THỂ XUẤT KHO KHÔNG
     * POST /api/warehouse/can-export
     * 
     * Gọi trước khi tạo shipment để validate
     */
    @PostMapping("/can-export")
    @Operation(
        summary = "Kiểm tra có thể xuất kho không",
        description = "Verify với blockchain xem lô hàng có thể xuất kho không"
    )
    public ResponseEntity<ApiResponse<WarehouseService.CanExportResult>> canExportBatch(
            @RequestParam BigInteger batchId,
            @RequestParam String wallet) {
        
        log.info("🔍 API: Check can export - Batch: {}, Wallet: {}", batchId, wallet);
        
        try {
            WarehouseService.CanExportResult result = 
                warehouseService.canExportBatch(batchId, wallet);
            
            if (result.isCanExport()) {
                log.info("✅ Batch {} can be exported", batchId);
                return ResponseEntity.ok(
                    ApiResponse.success(result, "Lô hàng có thể xuất kho")
                );
            } else {
                log.warn("⚠️ Batch {} cannot be exported: {}", batchId, result.getReason());
                return ResponseEntity.ok(
                    ApiResponse.success(result, "Không thể xuất kho: " + result.getReason())
                );
            }
        } catch (Exception e) {
            log.error("❌ Failed to check export permission", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Lỗi khi kiểm tra: " + e.getMessage())
            );
        }
    }

    /**
     * ✅ ĐỒNG BỘ TỪ BLOCKCHAIN
     * POST /api/warehouse/sync?wallet=0x...
     * 
     * Gọi khi cần sync lại dữ liệu từ blockchain về database
     * (Manual trigger nếu phát hiện data không khớp)
     */
    @PostMapping("/sync")
    @Operation(
        summary = "Đồng bộ dữ liệu từ blockchain",
        description = "Sync ownership của batches từ blockchain về database"
    )
    public ResponseEntity<ApiResponse<String>> syncFromBlockchain(
            @RequestParam String wallet) {
        
        log.info("🔄 API: Sync from blockchain for wallet: {}", wallet);
        
        try {
            warehouseService.syncFromBlockchain(wallet);
            
            log.info("✅ Sync completed");
            
            return ResponseEntity.ok(
                ApiResponse.success("SYNC_SUCCESS", "Đồng bộ thành công từ blockchain")
            );
        } catch (Exception e) {
            log.error("❌ Sync failed", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Đồng bộ thất bại: " + e.getMessage())
            );
        }
    }
}

