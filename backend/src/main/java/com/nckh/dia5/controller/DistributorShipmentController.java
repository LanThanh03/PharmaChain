package com.nckh.dia5.controller;

import com.nckh.dia5.dto.blockchain.CreateDistributorShipmentRequest;
import com.nckh.dia5.dto.blockchain.ShipmentDto;
import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.service.DistributorShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/distributor/shipments")
@RequiredArgsConstructor
@Tag(name = "Distributor Shipments", description = "APIs quản lý xuất kho từ nhà phân phối")
public class DistributorShipmentController {

    private final DistributorShipmentService distributorShipmentService;

    @PostMapping
    @Operation(summary = "Tạo shipment mới", description = "Xuất hàng từ nhà phân phối đến hiệu thuốc")
    public ResponseEntity<ApiResponse<ShipmentDto>> createShipment(
            @Valid @RequestBody CreateDistributorShipmentRequest request) {
        
        log.info("Creating distributor shipment: batchId={}, pharmacyId={}, quantity={}", 
                 request.getBatchId(), request.getPharmacyId(), request.getQuantity());
        
        ShipmentDto shipment = distributorShipmentService.createShipmentToPharmacy(request);
        
        return ResponseEntity.ok(ApiResponse.success(shipment, "Tạo phiếu xuất kho thành công"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách shipments", description = "Lấy tất cả shipments của distributor")
    public ResponseEntity<ApiResponse<List<ShipmentDto>>> getAllShipments() {
        log.info("Getting all distributor shipments");
        List<ShipmentDto> shipments = distributorShipmentService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success(shipments, "Lấy danh sách shipments thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết shipment", description = "Lấy thông tin chi tiết một shipment")
    public ResponseEntity<ApiResponse<ShipmentDto>> getShipmentById(@PathVariable Long id) {
        log.info("Getting shipment by id: {}", id);
        ShipmentDto shipment = distributorShipmentService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Lấy thông tin shipment thành công"));
    }
}
