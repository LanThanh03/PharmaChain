package com.nckh.dia5.dto.blockchain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchRequest {

    @NotNull(message = "Tên thuốc không được để trống")
    @Size(min = 1, max = 255, message = "Tên thuốc phải từ 1 đến 255 ký tự")
    private String drugName;

    @NotNull(message = "Tên nhà sản xuất không được để trống")
    @Size(min = 1, max = 255, message = "Tên nhà sản xuất phải từ 1 đến 255 ký tự")
    private String manufacturer;

    @NotNull(message = "Số lô không được để trống")
    @Size(min = 1, max = 100, message = "Số lô phải từ 1 đến 100 ký tự")
    private String batchNumber;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    @NotNull(message = "Ngày hết hạn không được để trống")
    // Removed @FutureOrPresent to allow creating batches with past expiry dates (for testing/historical records)
    private LocalDateTime expiryDate;

    @Size(max = 500, message = "Điều kiện bảo quản không được vượt quá 500 ký tự")
    private String storageConditions;

    private String rawMaterialBatchNumber;

    private Double rawMaterialAmountUsed;
}
