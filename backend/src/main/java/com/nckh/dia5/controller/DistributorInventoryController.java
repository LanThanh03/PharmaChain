package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DistributorInventory;
import com.nckh.dia5.service.DistributorInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/distributor/inventory")
@RequiredArgsConstructor
@Tag(name = "Distributor Inventory", description = "APIs quản lý kho nhà phân phối")
public class DistributorInventoryController {

    private final DistributorInventoryService inventoryService;

    @GetMapping("/company/{distributorId}")
    @Operation(summary = "Lấy kho hàng theo ID công ty", description = "Lấy tất cả sản phẩm trong kho của nhà phân phối")
    public ResponseEntity<ApiResponse<List<DistributorInventory>>> getInventoryByCompanyId(
            @PathVariable Long distributorId) {
        log.info("Getting inventory for distributor: {}", distributorId);
        List<DistributorInventory> inventory = inventoryService.getInventoryByDistributorId(distributorId);
        return ResponseEntity.ok(ApiResponse.success(inventory, "Lấy danh sách kho thành công"));
    }

    @GetMapping("/wallet/{walletAddress}")
    @Operation(summary = "Lấy kho hàng theo wallet address", description = "Lấy tất cả sản phẩm trong kho theo địa chỉ ví blockchain")
    public ResponseEntity<ApiResponse<List<DistributorInventory>>> getInventoryByWalletAddress(
            @PathVariable String walletAddress) {
        log.info("Getting inventory for wallet: {}", walletAddress);
        
        // ✅ FIX: Query từ drug_batches thay vì distributor_inventory
        // Vì khi nhận hàng từ blockchain chỉ update drug_batches.current_owner
        List<DistributorInventory> inventory = inventoryService.getInventoryByWalletAddressFromBatches(walletAddress);
        
        log.info("✅ Found {} items in inventory", inventory.size());
        return ResponseEntity.ok(ApiResponse.success(inventory, "Lấy danh sách kho thành công"));
    }

    @GetMapping("/company/{distributorId}/low-stock")
    @Operation(summary = "Lấy sản phẩm sắp hết hàng", description = "Lấy danh sách sản phẩm có số lượng <= ngưỡng tối thiểu")
    public ResponseEntity<ApiResponse<List<DistributorInventory>>> getLowStockItems(
            @PathVariable Long distributorId) {
        log.info("Getting low stock items for distributor: {}", distributorId);
        List<DistributorInventory> items = inventoryService.getLowStockItems(distributorId);
        return ResponseEntity.ok(ApiResponse.success(items, "Lấy danh sách sản phẩm sắp hết hàng thành công"));
    }

    @GetMapping("/company/{distributorId}/expiring-soon")
    @Operation(summary = "Lấy sản phẩm sắp hết hạn", description = "Lấy danh sách sản phẩm sắp hết hạn trong 30 ngày")
    public ResponseEntity<ApiResponse<List<DistributorInventory>>> getExpiringSoonItems(
            @PathVariable Long distributorId) {
        log.info("Getting expiring soon items for distributor: {}", distributorId);
        List<DistributorInventory> items = inventoryService.getExpiringSoonItems(distributorId);
        return ResponseEntity.ok(ApiResponse.success(items, "Lấy danh sách sản phẩm sắp hết hạn thành công"));
    }

    @GetMapping("/company/{distributorId}/search")
    @Operation(summary = "Tìm kiếm sản phẩm trong kho", description = "Tìm kiếm sản phẩm theo tên thuốc")
    public ResponseEntity<ApiResponse<List<DistributorInventory>>> searchByDrugName(
            @PathVariable Long distributorId,
            @RequestParam String searchTerm) {
        log.info("Searching inventory for distributor: {}, term: {}", distributorId, searchTerm);
        List<DistributorInventory> items = inventoryService.searchByDrugName(distributorId, searchTerm);
        return ResponseEntity.ok(ApiResponse.success(items, "Tìm kiếm thành công"));
    }

    @GetMapping("/company/{distributorId}/total-value")
    @Operation(summary = "Lấy tổng giá trị kho", description = "Tính tổng giá trị toàn bộ kho hàng")
    public ResponseEntity<ApiResponse<Double>> getTotalInventoryValue(
            @PathVariable Long distributorId) {
        log.info("Getting total inventory value for distributor: {}", distributorId);
        Double totalValue = inventoryService.getTotalInventoryValue(distributorId);
        return ResponseEntity.ok(ApiResponse.success(totalValue, "Lấy tổng giá trị kho thành công"));
    }
}
