package com.nckh.dia5.dto.blockchain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyDrugRequest {

    @NotNull(message = "Mã QR không được để trống")
    @Size(min = 1, max = 1000, message = "Mã QR phải từ 1 đến 1000 ký tự")
    private String qrCode;
}
