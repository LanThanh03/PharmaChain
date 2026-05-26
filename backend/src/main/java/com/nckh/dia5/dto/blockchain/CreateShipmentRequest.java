package com.nckh.dia5.dto.blockchain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.core.JsonParser;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequest {

    @NotNull(message = "Batch ID không được để trống")
    @JsonDeserialize(using = StringToBigIntegerDeserializer.class)
    private BigInteger batchId;

    @NotNull(message = "Địa chỉ nhận không được để trống")
    @Size(min = 42, max = 42, message = "Địa chỉ nhận phải có đúng 42 ký tự")
    private String toAddress;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    @Size(max = 1000, message = "Thông tin theo dõi không được vượt quá 1000 ký tự")
    private String trackingInfo;

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String notes;

    // Custom deserializer to handle string to BigInteger conversion
    public static class StringToBigIntegerDeserializer extends JsonDeserializer<BigInteger> {
        @Override
        public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                return new BigInteger(value.trim());
            } catch (NumberFormatException e) {
                throw new IOException("Invalid BigInteger value: " + value, e);
            }
        }
    }
}
