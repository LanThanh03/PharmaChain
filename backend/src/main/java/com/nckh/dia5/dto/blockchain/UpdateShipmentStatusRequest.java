package com.nckh.dia5.dto.blockchain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShipmentStatusRequest {

    @NotNull(message = "Shipment ID không được để trống")
    private BigInteger shipmentId;

    @NotNull(message = "Trạng thái mới không được để trống")
    private String newStatus;

    @Size(max = 1000, message = "Thông tin theo dõi không được vượt quá 1000 ký tự")
    private String trackingInfo;
}
