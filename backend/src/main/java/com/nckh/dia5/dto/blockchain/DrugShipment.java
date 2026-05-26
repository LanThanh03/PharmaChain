package com.nckh.dia5.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * DTO cho Drug Shipment từ Smart Contract
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugShipment {
    
    private BigInteger shipmentId;
    private BigInteger batchId;
    private String from;
    private String to;
    private BigInteger quantity;
    private LocalDateTime shipmentTimestamp;
    private ShipmentStatus status;
    private String trackingNumber;
    
    public enum ShipmentStatus {
        PENDING(0),
        IN_TRANSIT(1),
        DELIVERED(2),
        CANCELED(3);
        
        private final int value;
        
        ShipmentStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static ShipmentStatus fromValue(int value) {
            for (ShipmentStatus status : ShipmentStatus.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown ShipmentStatus value: " + value);
        }
    }
}
