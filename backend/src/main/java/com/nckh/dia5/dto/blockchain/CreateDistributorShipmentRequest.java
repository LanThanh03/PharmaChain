package com.nckh.dia5.dto.blockchain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * Request DTO for creating shipment from Distributor to Pharmacy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDistributorShipmentRequest {

    @NotNull(message = "Batch ID không được để trống")
    @JsonDeserialize(using = CreateShipmentRequest.StringToBigIntegerDeserializer.class)
    private BigInteger batchId;

    @NotNull(message = "Pharmacy ID không được để trống")
    private Long pharmacyId;  // ID của hiệu thuốc trong bảng pharma_companies

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    private String trackingNumber;
    
    private String notes;
    
    private String driverName;
    
    private String driverPhone;
    
    private String transportMethod;  // Xe tải, Xe van, Xe máy, etc.
}
