package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.model.ProductItem;
import com.nckh.dia5.repository.DrugBatchRepository;
import com.nckh.dia5.service.ProductItemService;
import com.nckh.dia5.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller để quản lý items của batch
 * Dùng cho Web Nhà Sản Xuất
 */
@Slf4j
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BatchItemsController {

    private final DrugBatchRepository drugBatchRepository;
    private final ProductItemService productItemService;
    private final QRCodeService qrCodeService;

    /**
     * Get danh sách items của một batch
     * GET /api/batches/{batchId}/items
     */
    @GetMapping("/{batchId}/items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItemsByBatch(@PathVariable Long batchId) {
        try {
            log.info("Getting items for batch ID: {}", batchId);

            // Get batch info
            Optional<DrugBatch> batchOpt = drugBatchRepository.findById(batchId);
            if (batchOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Không tìm thấy lô hàng", 404));
            }

            DrugBatch batch = batchOpt.get();

            // Get all items for this batch
            List<ProductItem> items = productItemService.findByBatchId(batchId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            
            // Batch info
            Map<String, Object> batchInfo = new HashMap<>();
            batchInfo.put("id", batch.getId());
            batchInfo.put("batchId", batch.getBatchId());
            batchInfo.put("batchNumber", batch.getBatchNumber());
            batchInfo.put("drugName", batch.getDrugName());
            batchInfo.put("manufacturer", batch.getManufacturer());
            batchInfo.put("quantity", batch.getQuantity());
            batchInfo.put("manufactureDate", batch.getManufactureTimestamp());
            batchInfo.put("expiryDate", batch.getExpiryDate());
            batchInfo.put("status", batch.getStatus());
            
            response.put("batch", batchInfo);

            // Items info with QR data
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (ProductItem item : items) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("itemCode", item.getItemCode());
                itemData.put("status", item.getCurrentStatus());
                itemData.put("ownerType", item.getCurrentOwnerType());
                itemData.put("qrCodeData", item.getQrCodeData());
                itemData.put("manufactureDate", item.getManufactureDate());
                itemData.put("expiryDate", item.getExpiryDate());
                itemData.put("createdAt", item.getCreatedAt());
                
                itemsList.add(itemData);
            }
            
            response.put("items", itemsList);
            response.put("totalItems", items.size());

            return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách items thành công"));

        } catch (Exception e) {
            log.error("Error getting items for batch: {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Download tất cả QR codes của batch dưới dạng ZIP
     * GET /api/batches/{batchId}/items/qr-codes
     */
    @GetMapping("/{batchId}/items/qr-codes")
    public ResponseEntity<byte[]> downloadAllQRCodes(@PathVariable Long batchId) {
        try {
            log.info("Downloading all QR codes for batch ID: {}", batchId);

            // Get batch
            Optional<DrugBatch> batchOpt = drugBatchRepository.findById(batchId);
            if (batchOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            DrugBatch batch = batchOpt.get();
            List<ProductItem> items = productItemService.findByBatchId(batchId);

            if (items.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Create ZIP file in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (int i = 0; i < items.size(); i++) {
                ProductItem item = items.get(i);
                
                try {
                    // Generate QR code image
                    byte[] qrImage = qrCodeService.generateQRCodeBytes(item.getItemCode(), 300, 300);

                    // Add to ZIP
                    String fileName = String.format("%s_Item_%d_%s.png", 
                            batch.getBatchNumber(), (i + 1), item.getItemCode());
                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(qrImage);
                    zos.closeEntry();

                } catch (Exception e) {
                    log.error("Error generating QR for item: {}", item.getItemCode(), e);
                }
            }

            zos.close();

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                    String.format("QR_Codes_%s.zip", batch.getBatchNumber()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            log.error("Error downloading QR codes for batch: {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get danh sách batches với thông tin items count
     * GET /api/batches/with-items
     */
    @GetMapping("/with-items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBatchesWithItemsInfo() {
        try {
            log.info("Getting all batches with items info");

            List<DrugBatch> batches = drugBatchRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (DrugBatch batch : batches) {
                List<ProductItem> items = productItemService.findByBatchId(batch.getId());
                
                Map<String, Object> batchData = new HashMap<>();
                batchData.put("id", batch.getId());
                batchData.put("batchId", batch.getBatchId());
                batchData.put("batchNumber", batch.getBatchNumber());
                batchData.put("drugName", batch.getDrugName());
                batchData.put("manufacturer", batch.getManufacturer());
                batchData.put("quantity", batch.getQuantity());
                batchData.put("manufactureDate", batch.getManufactureTimestamp());
                batchData.put("expiryDate", batch.getExpiryDate());
                batchData.put("status", batch.getStatus());
                batchData.put("itemsCount", items.size());
                
                // Count by status
                Map<String, Long> statusCounts = new HashMap<>();
                for (ProductItem item : items) {
                    String status = item.getCurrentStatus().toString();
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
                }
                batchData.put("itemsStatusCounts", statusCounts);
                
                result.add(batchData);
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách batches thành công"));

        } catch (Exception e) {
            log.error("Error getting batches with items info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage(), 500));
        }
    }

    /**
     * Delete batch và tất cả items của batch đó
     * DELETE /api/batches/{batchId}
     */
    @DeleteMapping("/{batchId}")
    public ResponseEntity<ApiResponse<String>> deleteBatch(@PathVariable Long batchId) {
        try {
            log.info("Deleting batch ID: {}", batchId);

            // Check if batch exists
            Optional<DrugBatch> batchOpt = drugBatchRepository.findById(batchId);
            if (batchOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Không tìm thấy lô hàng", 404));
            }

            DrugBatch batch = batchOpt.get();
            String batchNumber = batch.getBatchNumber();

            // Delete all items of this batch first
            List<ProductItem> items = productItemService.findByBatchId(batchId);
            log.info("Deleting {} items for batch {}", items.size(), batchNumber);
            
            for (ProductItem item : items) {
                productItemService.deleteItem(item.getId());
            }

            // Delete the batch
            drugBatchRepository.delete(batch);
            log.info("Successfully deleted batch {} with {} items", batchNumber, items.size());

            return ResponseEntity.ok(ApiResponse.success(
                    batchNumber, 
                    String.format("Đã xóa lô %s và %d sản phẩm thành công", batchNumber, items.size())
            ));

        } catch (Exception e) {
            log.error("Error deleting batch: {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa lô: " + e.getMessage(), 500));
        }
    }
}

