package com.nckh.dia5.service;

import com.nckh.dia5.dto.blockchain.DrugBatch;
import com.nckh.dia5.util.BlockchainEncodingFixer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Java wrapper cho PharmaLedger Smart Contract
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PharmaLedgerContract {

    private final Web3j web3j;
    private final Credentials credentials;
    private final StaticGasProvider gasProvider;
    private final BlockchainEncodingFixer encodingFixer;

    @Value("${pharmaledger.contract.address:0xc6e7DF5E7b4f2A278906862b61205850344D4e7d}")
    private String contractAddress;

    /**
     * Tạo lô thuốc mới
     */
    public CompletableFuture<String> issueBatch(
            DrugBatch.DrugInfo drugInfo,
            BigInteger quantity,
            LocalDateTime manufactureDate,
            LocalDateTime expiryDate,
            String qrCode) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🏭 Creating new drug batch: {}", drugInfo.getName());
                
                // Sanitize all Vietnamese text before sending to blockchain
                String cleanName = sanitizeForBlockchain(drugInfo.getName(), "UNKNOWN");
                String cleanActiveIngredient = sanitizeForBlockchain(drugInfo.getActiveIngredient(), "");
                String cleanDosage = sanitizeForBlockchain(drugInfo.getDosage(), "");
                String cleanManufacturer = sanitizeForBlockchain(drugInfo.getManufacturer(), "UNKNOWN");
                String cleanRegistrationNumber = sanitizeForBlockchain(drugInfo.getRegistrationNumber(), "");
                String cleanQrCode = sanitizeForBlockchain(qrCode, "UNKNOWN");
                
                // Log encoding fixes
                encodingFixer.logEncodingIssues(drugInfo.getName(), cleanName, "drug_name");
                encodingFixer.logEncodingIssues(drugInfo.getManufacturer(), cleanManufacturer, "manufacturer");
                encodingFixer.logEncodingIssues(qrCode, cleanQrCode, "qr_code");
                
                // Tạo struct DrugInfo
                DynamicStruct drugInfoStruct = new DynamicStruct(
                    new Utf8String(cleanName),
                    new Utf8String(cleanActiveIngredient),
                    new Utf8String(cleanDosage),
                    new Utf8String(cleanManufacturer),
                    new Utf8String(cleanRegistrationNumber)
                );
                
                // Convert DateTime to Unix timestamp
                BigInteger manufactureTimestamp = BigInteger.valueOf(
                    manufactureDate.atZone(ZoneId.systemDefault()).toEpochSecond());
                BigInteger expiryTimestamp = BigInteger.valueOf(
                    expiryDate.atZone(ZoneId.systemDefault()).toEpochSecond());
                
                Function function = new Function(
                    "issueBatch",
                    Arrays.asList(
                        drugInfoStruct,
                        new Uint256(quantity),
                        new Uint256(manufactureTimestamp),
                        new Uint256(expiryTimestamp),
                        new Utf8String(cleanQrCode)
                    ),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                EthSendTransaction response = web3j.ethSendTransaction(
                    Transaction.createFunctionCallTransaction(
                        credentials.getAddress(),
                        null,
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        contractAddress,
                        encodedFunction
                    )
                ).send();
                
                if (response.hasError()) {
                    throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
                }
                
                log.info("✅ Batch created successfully. Transaction: {}", response.getTransactionHash());
                return response.getTransactionHash();
                
            } catch (Exception e) {
                log.error("❌ Failed to create batch: {}", e.getMessage());
                throw new RuntimeException("Failed to create batch", e);
            }
        });
    }

    /**
     * Tạo shipment mới
     */
    public CompletableFuture<String> createShipment(
            BigInteger batchId,
            String toAddress,
            BigInteger quantity,
            String trackingNumber) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🚚 Creating shipment for batch: {}", batchId);
                
                // Sanitize tracking number
                String cleanTrackingNumber = sanitizeForBlockchain(trackingNumber, "TRK-0000000");
                encodingFixer.logEncodingIssues(trackingNumber, cleanTrackingNumber, "tracking_number");
                
                Function function = new Function(
                    "createShipment",
                    Arrays.asList(
                        new Uint256(batchId),
                        new Address(toAddress),
                        new Uint256(quantity),
                        new Utf8String(cleanTrackingNumber)
                    ),
                    Arrays.asList(new TypeReference<Uint256>() {})
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                EthSendTransaction response = web3j.ethSendTransaction(
                    Transaction.createFunctionCallTransaction(
                        credentials.getAddress(),
                        null,
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        contractAddress,
                        encodedFunction
                    )
                ).send();
                
                if (response.hasError()) {
                    throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
                }
                
                log.info("✅ Shipment created successfully. Transaction: {}", response.getTransactionHash());
                return response.getTransactionHash();
                
            } catch (Exception e) {
                log.error("❌ Failed to create shipment: {}", e.getMessage());
                throw new RuntimeException("Failed to create shipment", e);
            }
        });
    }

    /**
     * Nhận shipment
     */
    public CompletableFuture<String> receiveShipment(BigInteger shipmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("📦 Receiving shipment: {}", shipmentId);
                
                Function function = new Function(
                    "receiveShipment",
                    Arrays.asList(new Uint256(shipmentId)),
                    Arrays.asList()
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                EthSendTransaction response = web3j.ethSendTransaction(
                    Transaction.createFunctionCallTransaction(
                        credentials.getAddress(),
                        null,
                        gasProvider.getGasPrice(),
                        gasProvider.getGasLimit(),
                        contractAddress,
                        encodedFunction
                    )
                ).send();
                
                if (response.hasError()) {
                    throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
                }
                
                log.info("✅ Shipment received successfully. Transaction: {}", response.getTransactionHash());
                return response.getTransactionHash();
                
            } catch (Exception e) {
                log.error("❌ Failed to receive shipment: {}", e.getMessage());
                throw new RuntimeException("Failed to receive shipment", e);
            }
        });
    }

    /**
     * Verify QR Code
     */
    public CompletableFuture<DrugBatch> verifyQRCode(String qrCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔍 Verifying QR code: {}", qrCode);
                
                // Sanitize QR code for verification
                String cleanQrCode = sanitizeForBlockchain(qrCode, "UNKNOWN");
                encodingFixer.logEncodingIssues(qrCode, cleanQrCode, "qr_code_verify");
                
                Function function = new Function(
                    "verifyDrug",
                    Arrays.asList(new Utf8String(cleanQrCode)),
                    Arrays.asList(
                        new TypeReference<Bool>() {},
                        new TypeReference<DynamicStruct>() {},
                        new TypeReference<Utf8String>() {}
                    )
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).send();
                
                List<Type> result = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
                
                if (result.isEmpty() || !((Bool) result.get(0)).getValue()) {
                    throw new RuntimeException("Invalid QR code or verification failed");
                }
                
                // Parse batch info từ result
                // TODO: Implement proper parsing logic
                
                log.info("✅ QR code verified successfully");
                return DrugBatch.builder()
                    .qrCode(cleanQrCode)
                    .build();
                
            } catch (Exception e) {
                log.error("❌ Failed to verify QR code: {}", e.getMessage());
                throw new RuntimeException("Failed to verify QR code", e);
            }
        });
    }

    /**
     * Get batch information
     */
    public CompletableFuture<DrugBatch> getBatchInfo(BigInteger batchId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("📋 Getting batch info: {}", batchId);
                
                Function function = new Function(
                    "batches",
                    Arrays.asList(new Uint256(batchId)),
                    Arrays.asList(
                        new TypeReference<Uint256>() {}, // batchId
                        new TypeReference<DynamicStruct>() {}, // drugInfo
                        new TypeReference<Uint256>() {}, // quantity
                        new TypeReference<Address>() {}, // manufacturerAddress
                        new TypeReference<Uint256>() {}, // manufactureTimestamp
                        new TypeReference<Uint256>() {}, // expiryDate
                        new TypeReference<Uint8>() {}, // status
                        new TypeReference<Utf8String>() {}, // qrCode
                        new TypeReference<Address>() {}, // currentOwner
                        new TypeReference<Bool>() {} // isActive
                    )
                );
                
                String encodedFunction = FunctionEncoder.encode(function);
                
                EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).send();
                
                List<Type> result = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
                
                if (result.isEmpty()) {
                    throw new RuntimeException("Batch not found");
                }
                
                // Parse result to DrugBatch
                // TODO: Implement proper parsing logic
                
                log.info("✅ Batch info retrieved successfully");
                return DrugBatch.builder()
                    .batchId(batchId)
                    .build();
                
            } catch (Exception e) {
                log.error("❌ Failed to get batch info: {}", e.getMessage());
                throw new RuntimeException("Failed to get batch info", e);
            }
        });
    }

    /**
     * Sanitize Vietnamese text for blockchain
     */
    private String sanitizeForBlockchain(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String cleaned = encodingFixer.cleanForBlockchain(input);
        if (cleaned == null || cleaned.isBlank()) {
            return fallback;
        }
        return cleaned;
    }
}
