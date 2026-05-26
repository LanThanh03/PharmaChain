package com.nckh.dia5.dto.blockchain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugBatchDto {

    private Long id;

    @NotNull(message = "Batch ID không được để trống")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger batchId;

    @NotNull(message = "Tên thuốc không được để trống")
    @Size(max = 255, message = "Tên thuốc không được vượt quá 255 ký tự")
    private String drugName;

    @NotNull(message = "Nhà sản xuất không được để trống")
    @Size(max = 255, message = "Tên nhà sản xuất không được vượt quá 255 ký tự")
    private String manufacturer;

    @NotNull(message = "Số lô không được để trống")
    @Size(max = 100, message = "Số lô không được vượt quá 100 ký tự")
    private String batchNumber;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    @NotNull(message = "Địa chỉ nhà sản xuất không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ nhà sản xuất phải có đúng 42 ký tự")
    private String manufacturerAddress;

    @NotNull(message = "Chủ sở hữu hiện tại không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ chủ sở hữu phải có đúng 42 ký tự")
    private String currentOwner;

    @NotNull(message = "Thời gian sản xuất không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime manufactureTimestamp;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryDate;

    @Size(max = 500, message = "Điều kiện bảo quản không được vượt quá 500 ký tự")
    private String storageConditions;

    @NotNull(message = "Trạng thái không được để trống")
    private String status;

    @Size(max = 1000, message = "Mã QR không được vượt quá 1000 ký tự")
    private String qrCode;

    @Size(max = 66, message = "Hash giao dịch không được vượt quá 66 ký tự")
    private String transactionHash;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger blockNumber;

    private Boolean isSynced;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<ShipmentDto> shipments;
    private List<BlockchainTransactionDto> transactions;

    // Helper method for backward compatibility
    public String getMintTransactionHash() {
        return transactionHash;
    }
}
