package com.nckh.dia5.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * DTO cho Drug Batch từ Smart Contract
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugBatch {
    
    private BigInteger batchId;
    private DrugInfo drugInfo;
    private BigInteger quantity;
    private String manufacturerAddress;
    private LocalDateTime manufactureTimestamp;
    private LocalDateTime expiryDate;
    private BatchStatus status;
    private String qrCode;
    private String currentOwner;
    private boolean isActive;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugInfo {
        private String name;
        private String activeIngredient;
        private String dosage;
        private String manufacturer;
        private String registrationNumber;
    }
    
    public enum BatchStatus {
        MANUFACTURED(0),
        IN_TRANSIT(1),
        DELIVERED(2),
        SOLD(3),
        EXPIRED(4);
        
        private final int value;
        
        BatchStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static BatchStatus fromValue(int value) {
            for (BatchStatus status : BatchStatus.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown BatchStatus value: " + value);
        }
    }
}
