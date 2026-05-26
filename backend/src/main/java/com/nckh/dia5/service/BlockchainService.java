package com.nckh.dia5.service;

import com.nckh.dia5.config.BlockchainConfig;
import com.nckh.dia5.util.BlockchainEncodingFixer;
import com.nckh.dia5.util.SafeFunctionEncoder;
import com.nckh.dia5.model.BlockchainTransaction;
import com.nckh.dia5.repository.BlockchainTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.Contract;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service tương tác với PharmaLedgerOptimized Smart Contract
 * Đã được tối ưu hóa để giảm số lượng transaction và gas fee.
 * 
 * QUY TRÌNH CHUẨN:
 * 1. Tạo lô (Manufacturer): 1 Tx (createBatchWithItems)
 * 2. Chuyển hàng (Sender): 1 Tx (createAndDispatchShipment)
 * 3. Nhận hàng (Receiver): 1 Tx (receiveShipment)
 * 4. Bán lẻ (Pharmacy): 1 Tx (sellItem)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final BlockchainConfig blockchainConfig;
    private final BlockchainEncodingFixer encodingFixer;
    private final SafeFunctionEncoder safeFunctionEncoder;
    private final BlockchainTransactionRepository blockchainTransactionRepository;

    // Hardhat default accounts for development
    private static final Map<String, String> WALLET_KEYS = new HashMap<>() {{
        put("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266".toLowerCase(), "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
        put("0x70997970C51812dc3A010C7d01b50e0d17dc79C8".toLowerCase(), "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d");
        put("0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC".toLowerCase(), "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a");
        put("0x90F79bf6EB2c4f870365E785982E1f101E93b906".toLowerCase(), "0x7c852118294e51e653712a81e05800f419141751be58f605c371e15141b007a6");
        put("0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65".toLowerCase(), "0x47e170ecddcab933da33a4cc4e9483d6861399695a43007c093a11a6a1e5d9fe");
        put("0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc".toLowerCase(), "0x8b3a350cf5c34c9194ca85829a2df0ec3153be0318b5e2d3348e872092ed28ee");
    }};

    private Credentials getCredentialsForAddress(String address) {
        if (address == null) return this.credentials;
        String key = WALLET_KEYS.get(address.toLowerCase());
        if (key != null) {
            log.debug("Using specific credentials for address: {}", address);
            return Credentials.create(key);
        }
        return this.credentials;
    }

    // Event definitions for decoding
    private static final Event BATCH_CREATED_EVENT = new Event("BatchCreated", 
            Arrays.asList(
                new TypeReference<Uint256>(true) {}, // batchId (indexed)
                new TypeReference<Address>(true) {}, // manufacturer (indexed)
                new TypeReference<Utf8String>(false) {},
                new TypeReference<Uint256>(false) {},
                new TypeReference<Uint256>(false) {},
                new TypeReference<Uint256>(false) {},
                new TypeReference<Bytes32>(false) {},
                new TypeReference<Uint256>(false) {}
            ));

    private static final Event SHIPMENT_CREATED_EVENT = new Event("ShipmentCreatedAndDispatched", 
            Arrays.asList(
                new TypeReference<Uint256>(true) {}, // shipmentId (indexed)
                new TypeReference<Uint256>(true) {}, // batchId (indexed)
                new TypeReference<Address>(true) {}, // from (indexed)
                new TypeReference<Address>(false) {},
                new TypeReference<Utf8String>(false) {},
                new TypeReference<Utf8String>(false) {},
                new TypeReference<Uint256>(false) {},
                new TypeReference<Uint256>(false) {},
                new TypeReference<Utf8String>(false) {}
            ));

    // ============================================================
    // 1. MANUFACTURER: TẠO LÔ THUỐC (1 Tx)
    // ============================================================
    
    public CompletableFuture<TransactionReceipt> createBatchWithItems(
            String drugName,
            String manufacturerName,
            BigInteger quantity,
            BigInteger manufactureDate,
            BigInteger expiryDate,
            String itemsMerkleRoot) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating batch on blockchain: drugName={}, quantity={}, merkleRoot={}", 
                         drugName, quantity, itemsMerkleRoot);

                // 1. Prepare DrugInfo struct
                List<Type> drugInfoParams = Arrays.asList(
                    new Utf8String(encodingFixer.cleanForBlockchain(drugName)),
                    new Utf8String(""), // activeIngredient
                    new Utf8String(""), // dosage
                    new Utf8String(encodingFixer.cleanForBlockchain(manufacturerName)),
                    new Utf8String("")  // registrationNumber
                );
                DynamicStruct drugInfo = new DynamicStruct(drugInfoParams);

                // 2. Prepare function params
                List<Type> inputParameters = Arrays.asList(
                    drugInfo,
                    new Uint256(quantity),
                    new Uint256(manufactureDate),
                    new Uint256(expiryDate),
                    new Bytes32(encodingFixer.safeHexToBytes(itemsMerkleRoot)),
                    new Utf8String(encodingFixer.cleanForBlockchain(manufacturerName))
                );

                Function function = new Function(
                    "createBatchWithItems",
                    inputParameters,
                    Arrays.asList(new TypeReference<Uint256>() {})
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to create batch on blockchain", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Alias for legacy code
    public CompletableFuture<TransactionReceipt> issueBatch(
            String drugName,
            String manufacturerName,
            String batchNumber,
            BigInteger quantity,
            BigInteger expiryDate,
            String storageConditions) {
        
        // Use current time as manufacture date
        BigInteger manufactureDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        
        // Use a dummy merkle root (valid 32 bytes hex)
        // In production, this should be calculated from actual items
        String dummyRoot = "0x0000000000000000000000000000000000000000000000000000000000000001";
        
        return createBatchWithItems(
            drugName,
            manufacturerName,
            quantity,
            manufactureDate,
            expiryDate,
            dummyRoot
        );
    }

    // ============================================================
    // 2. SENDER: TẠO VÀ GỬI SHIPMENT (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> createAndDispatchShipment(
            BigInteger batchId,
            String toAddress,
            String fromLocation,
            String toLocation,
            BigInteger quantity,
            String trackingNumber,
            String notes) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Dispatching shipment: batchId={}, to={}, quantity={}", batchId, toAddress, quantity);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Address(toAddress),
                    new Utf8String(encodingFixer.cleanForBlockchain(fromLocation)),
                    new Utf8String(encodingFixer.cleanForBlockchain(toLocation)),
                    new Utf8String("UNKNOWN"), // toLocationType (deprecated but kept for signature)
                    new Uint256(quantity),
                    new Utf8String(encodingFixer.cleanForBlockchain(trackingNumber)),
                    new Utf8String(encodingFixer.cleanForBlockchain(notes))
                );

                Function function = new Function(
                    "createAndDispatchShipment",
                    inputParameters,
                    Arrays.asList(new TypeReference<Uint256>() {})
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to dispatch shipment", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Alias for createAndDispatchShipment to support legacy calls
    public CompletableFuture<TransactionReceipt> createShipment(
            BigInteger batchId,
            String toAddress,
            BigInteger quantity,
            String trackingNumber) {
        // Use default values for missing parameters
        return createAndDispatchShipment(
            batchId, 
            toAddress, 
            "Unknown Location", // fromLocation
            "Unknown Location", // toLocation
            quantity, 
            trackingNumber, 
            "" // notes
        );
    }

    // ============================================================
    // 3. RECEIVER: NHẬN HÀNG (1 Tx)
    // ============================================================

    /**
     * Receive shipment on blockchain.
     * IMPORTANT: Always uses the SERVICE wallet (credentials) to sign the TX.
     * This is intentional — the backend acts as an authorized relayer.
     * The receiverAddress is passed only for logging/context, NOT for key lookup.
     */
    public CompletableFuture<TransactionReceipt> receiveShipment(
            BigInteger shipmentId,
            String receiverLocationName,
            String receiverAddress) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🚚 Receiving shipment on blockchain: shipmentId={}, location={}, receiver={}", 
                         shipmentId, receiverLocationName, receiverAddress);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(shipmentId),
                    new Utf8String(encodingFixer.cleanForBlockchain(receiverLocationName))
                );

                Function function = new Function(
                    "receiveShipment",
                    inputParameters,
                    Arrays.asList()
                );

                // ✅ FIX: Always use SERVICE credentials (not receiver key lookup which fails on Sepolia).
                // The service wallet is the authorized relayer for all on-chain operations.
                // Pass null as fromAddress so getCredentialsForAddress returns this.credentials.
                TransactionReceipt receipt = executeTransaction(function, null, null);
                log.info("✅ receiveShipment TX success: hash={}, block={}", 
                         receipt.getTransactionHash(), receipt.getBlockNumber());
                return receipt;

            } catch (Exception e) {
                log.error("Failed to receive shipment", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    /**
     * Overload: also accepts a Shipment object to persist the linkage to Admin Log.
     */
    public CompletableFuture<TransactionReceipt> receiveShipmentWithLink(
            BigInteger shipmentId,
            String receiverLocationName,
            com.nckh.dia5.model.Shipment shipment,
            String fromAddress) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🚚 Receiving shipment on blockchain (with DB link): shipmentId={}", shipmentId);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(shipmentId),
                    new Utf8String(encodingFixer.cleanForBlockchain(receiverLocationName))
                );

                Function function = new Function(
                    "receiveShipment",
                    inputParameters,
                    Arrays.asList()
                );

                // Use specific fromAddress (receiver) to sign the transaction as required by contract
                return executeTransactionWithShipment(function, shipment, fromAddress);

            } catch (Exception e) {
                log.error("Failed to receive shipment", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Overload for legacy calls
    public CompletableFuture<TransactionReceipt> receiveShipment(BigInteger shipmentId, String location) {
        return receiveShipment(shipmentId, location, null);
    }

    // Overload for legacy calls
    public CompletableFuture<TransactionReceipt> receiveShipment(BigInteger shipmentId) {
        return receiveShipment(shipmentId, "Unknown Location");
    }

    // ============================================================
    // BATCH STATUS: THU HỒI LÔ (1 Tx)
    // ============================================================
    
    public CompletableFuture<TransactionReceipt> recallBatch(BigInteger batchId, com.nckh.dia5.model.DrugBatch batch) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Recalling batch on blockchain: batchId={}", batchId);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId)
                );

                Function function = new Function(
                    "recallBatch",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function, batch);

            } catch (Exception e) {
                log.error("Failed to recall batch on blockchain", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // ============================================================
    // 4. PHARMACY: BÁN LẺ (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> sellItem(
            BigInteger batchId,
            String itemCode,
            List<String> merkleProof) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Selling item: batchId={}, itemCode={}", batchId, itemCode);

                // Convert proof to Bytes32 list
                List<Bytes32> proofBytes = merkleProof.stream()
                        .map(p -> new Bytes32(encodingFixer.safeHexToBytes(p)))
                        .toList();

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Utf8String(itemCode),
                    new DynamicArray<>(Bytes32.class, proofBytes)
                );

                Function function = new Function(
                    "sellItem",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to sell item", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // ============================================================
    // 5. UPDATE ITEM STATUS (DAMAGED/RECALL) (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> updateItemStatus(
            BigInteger batchId,
            String itemCode,
            BigInteger newStatus,
            String reason,
            List<String> merkleProof) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Updating item status: batchId={}, itemCode={}, newStatus={}, reason={}", 
                         batchId, itemCode, newStatus, reason);

                // Convert proof to Bytes32 list
                List<Bytes32> proofBytes = merkleProof.stream()
                        .map(p -> new Bytes32(encodingFixer.safeHexToBytes(p)))
                        .toList();

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Utf8String(itemCode),
                    new org.web3j.abi.datatypes.generated.Uint8(newStatus), // ItemStatus is an enum (uint8)
                    new Utf8String(encodingFixer.cleanForBlockchain(reason)),
                    new DynamicArray<>(Bytes32.class, proofBytes)
                );

                Function function = new Function(
                    "updateItemStatus",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to update item status", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // ============================================================
    // READ-ONLY & HELPER METHODS
    // ============================================================

    public CompletableFuture<Boolean> verifyOwnership(BigInteger batchId, String address) {
        return CompletableFuture.completedFuture(true);
    }

    public List<Map<String, Object>> getShipmentHistory(BigInteger shipmentId) {
        return new ArrayList<>();
    }

    public Map<String, Object> getShipmentDetails(BigInteger shipmentId) {
        return new HashMap<>();
    }

    public CompletableFuture<TransactionReceipt> dispatchShipment(BigInteger shipmentId, String location, String notes) {
         // Placeholder: In real impl, this might call createAndDispatchShipment if arguments were available
         // or a specific dispatch function if the contract supported it.
         return CompletableFuture.completedFuture(new TransactionReceipt());
    }

    public CompletableFuture<TransactionReceipt> updateShipmentStatus(
            BigInteger shipmentId, 
            BigInteger status, 
            String location, 
            String notes) {
        return CompletableFuture.completedFuture(new TransactionReceipt());
    }

    public String getItemStatus(String itemCode) {
        return "UNKNOWN";
    }

    public String getItemStatus(BigInteger batchId, String itemCode) {
        return "UNKNOWN";
    }

    public String getContractAddress() {
        return blockchainConfig.getContractAddress();
    }

    public BigInteger getLatestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            log.error("Failed to get latest block number", e);
            return BigInteger.ZERO;
        }
    }

    public Optional<TransactionReceipt> getTransactionReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
        } catch (Exception e) {
            log.error("Failed to get transaction receipt", e);
            return Optional.empty();
        }
    }

    public boolean isTransactionSuccessful(String txHash) {
        return getTransactionReceipt(txHash)
                .map(r -> "0x1".equals(r.getStatus()))
                .orElse(false);
    }

    /**
     * Extract batchId from BatchCreated event
     */
    public Optional<BigInteger> extractBatchId(TransactionReceipt receipt) {
        return extractIndexedValue(receipt, BATCH_CREATED_EVENT, 0);
    }

    /**
     * Extract shipmentId from ShipmentCreated event
     */
    public Optional<BigInteger> extractShipmentId(TransactionReceipt receipt) {
        return extractIndexedValue(receipt, SHIPMENT_CREATED_EVENT, 0);
    }

    private Optional<BigInteger> extractIndexedValue(TransactionReceipt receipt, Event event, int index) {
        try {
            String encodedEventSignature = EventEncoder.encode(event);
            for (Log log : receipt.getLogs()) {
                if (log.getTopics().contains(encodedEventSignature)) {
                    EventValues eventValues = Contract.staticExtractEventParameters(event, log);
                    if (eventValues != null && index < eventValues.getIndexedValues().size()) {
                        Type value = eventValues.getIndexedValues().get(index);
                        return Optional.of((BigInteger) value.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract event value", e);
        }
        return Optional.empty();
    }

    private TransactionReceipt executeTransaction(Function function) throws Exception {
        return executeTransaction(function, (com.nckh.dia5.model.DrugBatch) null, null);
    }

    private TransactionReceipt executeTransaction(Function function, com.nckh.dia5.model.DrugBatch batch) throws Exception {
        return executeTransaction(function, batch, null);
    }

    private TransactionReceipt executeTransaction(Function function, com.nckh.dia5.model.DrugBatch batch, String fromAddress) throws Exception {
        return executeTransactionInternal(function, batch, null, fromAddress);
    }

    /**
     * Overload that links to a Shipment entity in the Admin Log.
     * Used by receiveShipmentWithLink() to ensure Admin Log shows receiveShipment with shipment linkage.
     */
    private TransactionReceipt executeTransactionWithShipment(
            Function function,
            com.nckh.dia5.model.Shipment shipment,
            String fromAddress) throws Exception {
        return executeTransactionInternal(function, null, shipment, fromAddress);
    }

    /**
     * Core transaction execution method.
     * Always uses service credentials (fromAddress only used as hint - if not in WALLET_KEYS, falls back to service key).
     * Saves to blockchain_transactions (Admin Log) — idempotent via hash check.
     */
    private TransactionReceipt executeTransactionInternal(
            Function function,
            com.nckh.dia5.model.DrugBatch batch,
            com.nckh.dia5.model.Shipment shipment,
            String fromAddress) throws Exception {

        String contractAddress = blockchainConfig.getContractAddress();
        String encodedFunction = safeFunctionEncoder.safeEncode(function);

        Credentials txCredentials = getCredentialsForAddress(fromAddress);
        TransactionManager txManager = new RawTransactionManager(web3j, txCredentials, blockchainConfig.getChainId());
        
        org.web3j.protocol.core.methods.response.EthSendTransaction response = 
            txManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
            );

        if (response.hasError()) {
            throw new RuntimeException("Tx failed: " + response.getError().getMessage());
        }

        // Wait for receipt
        String txHash = response.getTransactionHash();
        log.info("📡 TX sent: hash={}, function={}", txHash, function.getName());
        
        TransactionReceipt receipt = null;
        for (int i = 0; i < 60; i++) { // wait up to 60s
            Optional<TransactionReceipt> receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receiptOpt.isPresent()) {
                receipt = receiptOpt.get();
                break;
            }
            Thread.sleep(1000);
        }

        if (receipt == null) {
            throw new RuntimeException("Tx receipt timeout for hash: " + txHash);
        }

        log.info("✅ TX mined: hash={}, status={}, block={}, function={}",
                 receipt.getTransactionHash(), receipt.getStatus(),
                 receipt.getBlockNumber(), function.getName());

        // ✅ SAVE TO DATABASE FOR ADMIN LEDGER (idempotent)
        try {
            String finalHash = receipt.getTransactionHash();
            // Check duplicate before insert
            boolean exists = blockchainTransactionRepository.existsByTransactionHash(finalHash);
            if (!exists) {
                BlockchainTransaction txRecord = new BlockchainTransaction();
                txRecord.setTransactionHash(finalHash);
                txRecord.setBlockNumber(receipt.getBlockNumber());
                txRecord.setFromAddress(receipt.getFrom());
                txRecord.setToAddress(receipt.getTo());
                txRecord.setFunctionName(function.getName());
                txRecord.setGasUsed(receipt.getGasUsed());
                txRecord.setGasPrice(gasProvider.getGasPrice());
                txRecord.setStatus(receipt.isStatusOK() ? 
                        BlockchainTransaction.TransactionStatus.SUCCESS : 
                        BlockchainTransaction.TransactionStatus.FAILED);
                txRecord.setTimestamp(java.time.LocalDateTime.now());
                if (batch != null) {
                    txRecord.setDrugBatch(batch);
                }
                if (shipment != null) {
                    txRecord.setShipment(shipment);
                }
                blockchainTransactionRepository.save(txRecord);
                log.info("💾 Admin Log: saved {} TX: {}", function.getName(), finalHash);
            } else {
                // Already exists — update shipment/batch linkage if provided
                blockchainTransactionRepository.findByTransactionHash(finalHash).ifPresent(existing -> {
                    boolean updated = false;
                    if (existing.getShipment() == null && shipment != null) {
                        existing.setShipment(shipment);
                        updated = true;
                    }
                    if (existing.getDrugBatch() == null && batch != null) {
                        existing.setDrugBatch(batch);
                        updated = true;
                    }
                    if (updated) {
                        blockchainTransactionRepository.save(existing);
                        log.info("🔄 Admin Log: updated linkage for TX: {}", finalHash);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to save Admin Log transaction record: {}", e.getMessage());
        }

        return receipt;
    }

    private String callFunction(Function function) throws Exception {
        String contractAddress = blockchainConfig.getContractAddress();
        String encodedFunction = safeFunctionEncoder.safeEncode(function);
        
        org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.getAddress(), contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        return response.getValue();
    }
}