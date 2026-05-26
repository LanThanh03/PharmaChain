package com.nckh.dia5.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nckh.dia5.model.BlockchainEvent;
import com.nckh.dia5.repository.BlockchainEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để index blockchain events - Aggressive Cleanup & Sync Version
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainIndexerService {

    private final Web3j web3j;
    private final BlockchainEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${pharmaledger.contract.address}")
    private String contractAddress;

    @Value("${pharmaledger.blockchain.safety-buffer:2}")
    private int safetyBuffer;

    private static final BigInteger MAX_BLOCK_RANGE = BigInteger.valueOf(100000);

    // Event signatures
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
        )
    );

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
        )
    );

    private static final Event SHIPMENT_RECEIVED_EVENT = new Event("ShipmentReceived",
        Arrays.asList(
            new TypeReference<Uint256>(true) {}, // shipmentId (indexed)
            new TypeReference<Uint256>(true) {}, // batchId (indexed)
            new TypeReference<Address>(true) {}, // receiver (indexed)
            new TypeReference<Uint256>(false) {}, 
            new TypeReference<Address>(false) {}
        )
    );

    @Scheduled(fixedDelay = 30000)
    @Async
    public void indexNewEvents() {
        try {
            BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
            BigInteger lastIndexedBlock = eventRepository.findMaxBlockNumber().orElse(BigInteger.ZERO);
            
            // 🧹 CLEANUP: Xóa rác cũ nếu block quá thấp (< 1000)
            if (lastIndexedBlock.compareTo(BigInteger.ZERO) > 0 && lastIndexedBlock.compareTo(BigInteger.valueOf(1000)) < 0) {
                log.info("🧹 Dọn dẹp rác từ local test...");
                eventRepository.deleteAll();
                lastIndexedBlock = BigInteger.ZERO;
            }

            // 🚀 AGGRESSIVE JUMP: Nhảy tới sát hiện tại trên Sepolia
            if (currentBlock.subtract(lastIndexedBlock).compareTo(BigInteger.valueOf(50000)) > 0) {
                lastIndexedBlock = currentBlock.subtract(BigInteger.valueOf(5000));
                log.info("🚀 Aggressive jump to block {}...", lastIndexedBlock);
            }

            BigInteger safeCurrentBlock = currentBlock.subtract(BigInteger.valueOf(safetyBuffer));
            if (safeCurrentBlock.compareTo(lastIndexedBlock) <= 0) return;

            BigInteger fromBlock = lastIndexedBlock.add(BigInteger.ONE);
            BigInteger toBlock = safeCurrentBlock;

            if (toBlock.subtract(fromBlock).compareTo(MAX_BLOCK_RANGE) > 0) {
                toBlock = fromBlock.add(MAX_BLOCK_RANGE);
            }

            indexEvents(BATCH_CREATED_EVENT, "BatchCreated", fromBlock, toBlock);
            indexEvents(SHIPMENT_CREATED_EVENT, "ShipmentCreated", fromBlock, toBlock);
            indexEvents(SHIPMENT_RECEIVED_EVENT, "ShipmentReceived", fromBlock, toBlock);

        } catch (Exception e) {
            log.error("❌ Indexing failed: {}", e.getMessage());
        }
    }

    private void indexEvents(Event event, String eventType, BigInteger fromBlock, BigInteger toBlock) {
        try {
            String eventSignature = EventEncoder.encode(event);
            EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(fromBlock),
                new DefaultBlockParameterNumber(toBlock),
                contractAddress
            ).addSingleTopic(eventSignature);

            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<EthLog.LogResult> logs = ethLog.getLogs();

            if (logs != null && !logs.isEmpty()) {
                log.info("🏷️ Found {} {} events in block range {}-{}", logs.size(), eventType, fromBlock, toBlock);
                for (EthLog.LogResult logResult : logs) {
                    Log eventLog = (Log) logResult.get();
                    saveDecodedEvent(eventLog, event, eventType);
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to index {}: {}", eventType, e.getMessage());
        }
    }

    private void saveDecodedEvent(Log eventLog, Event event, String eventType) throws Exception {
        if (eventRepository.existsByTransactionHashAndLogIndex(
                eventLog.getTransactionHash(), eventLog.getLogIndex())) return;

        EventValues values = Contract.staticExtractEventParameters(event, eventLog);
        
        BlockchainEvent.BlockchainEventBuilder builder = BlockchainEvent.builder()
                .eventType(eventType)
                .contractAddress(contractAddress)
                .transactionHash(eventLog.getTransactionHash())
                .blockNumber(eventLog.getBlockNumber())
                .logIndex(eventLog.getLogIndex())
                .eventData("{}")
                .processed(false);

        if (values != null) {
            if (eventType.equals("BatchCreated")) {
                builder.batchId((BigInteger) values.getIndexedValues().get(0).getValue());
                builder.fromAddress((String) values.getIndexedValues().get(1).getValue());
            } else if (eventType.equals("ShipmentCreated")) {
                builder.shipmentId((BigInteger) values.getIndexedValues().get(0).getValue());
                builder.batchId((BigInteger) values.getIndexedValues().get(1).getValue());
                builder.fromAddress((String) values.getIndexedValues().get(2).getValue());
            } else if (eventType.equals("ShipmentReceived")) {
                builder.shipmentId((BigInteger) values.getIndexedValues().get(0).getValue());
                builder.batchId((BigInteger) values.getIndexedValues().get(1).getValue());
                builder.toAddress((String) values.getIndexedValues().get(2).getValue());
            }
        }

        eventRepository.save(builder.build());
        log.info("💾 Indexed {} - TX: {} (Block: {})", eventType, eventLog.getTransactionHash(), eventLog.getBlockNumber());
    }

    public Map<String, Object> getIndexingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastIndexed", eventRepository.findMaxBlockNumber().orElse(BigInteger.ZERO));
        return status;
    }
}
