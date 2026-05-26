package com.nckh.dia5.dto.blockchain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainTransactionDto {

    private Long id;

    @NotNull(message = "Hash giao dịch không được để trống")
    @Size(min = 66, max = 66, message = "Hash giao dịch phải có đúng 66 ký tự")
    private String transactionHash;

    @NotNull(message = "Số block không được để trống")
    private BigInteger blockNumber;

    @NotNull(message = "Địa chỉ gửi không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ gửi phải có đúng 42 ký tự")
    private String fromAddress;

    @NotNull(message = "Địa chỉ nhận không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ nhận phải có đúng 42 ký tự")
    private String toAddress;

    @NotNull(message = "Tên function không được để trống")
    @Size(max = 100, message = "Tên function không được vượt quá 100 ký tự")
    private String functionName;

    private BigInteger gasUsed;
    private BigInteger gasPrice;

    @NotNull(message = "Trạng thái không được để trống")
    private String status;

    private String inputData;
    private String eventLogs;

    @Size(max = 1000, message = "Thông báo lỗi không được vượt quá 1000 ký tự")
    private String errorMessage;

    @NotNull(message = "Thời gian không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Long drugBatchId;
    private Long shipmentId;
}
