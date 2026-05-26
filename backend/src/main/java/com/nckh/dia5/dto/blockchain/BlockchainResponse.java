package com.nckh.dia5.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response cho blockchain operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String transactionHash;
    private String blockNumber;
    private String gasUsed;
    
    public static <T> BlockchainResponse<T> success(T data, String transactionHash) {
        return BlockchainResponse.<T>builder()
                .success(true)
                .message("Transaction successful")
                .data(data)
                .transactionHash(transactionHash)
                .build();
    }
    
    public static <T> BlockchainResponse<T> success(T data) {
        return BlockchainResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }
    
    public static <T> BlockchainResponse<T> error(String message) {
        return BlockchainResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
