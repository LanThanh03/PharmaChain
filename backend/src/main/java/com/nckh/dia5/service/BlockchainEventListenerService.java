package com.nckh.dia5.service;

import com.nckh.dia5.model.DrugBatch;
import com.nckh.dia5.repository.DrugBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * Service lắng nghe events từ blockchain và tự động sync database
 * 
 * Events quan trọng:
 * - ShipmentReceived: Khi NPP/Pharmacy nhận hàng
 * - OwnershipTransferred: Khi ownership thay đổi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainEventListenerService {

    private final Web3j web3j;
    private final DrugBatchRepository drugBatchRepository;
    private final String contractAddress = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"; // Updated after re-deploy

    /**
     * Khởi động event listeners khi application ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("🎧 Starting blockchain event listeners...");
        
        listenToShipmentReceived();
        listenToOwnershipTransferred();
        
        log.info("✅ Blockchain event listeners started");
    }

    /**
     * ✅ LISTEN EVENT: ShipmentReceived
     * 
     * Event signature:
     * ShipmentReceived(uint256 indexed shipmentId, uint256 indexed batchId, address indexed receiver, uint256 receiveDate, address previousOwner)
     */
    private void listenToShipmentReceived() {
        Event shipmentReceivedEvent = new Event(
            "ShipmentReceived",
            Arrays.asList(
                new TypeReference<Uint256>(true) {},    // shipmentId (indexed)
                new TypeReference<Uint256>(true) {},    // batchId (indexed)
                new TypeReference<Address>(true) {},    // receiver (indexed)
                new TypeReference<Uint256>() {},        // receiveDate
                new TypeReference<Address>() {}         // previousOwner
            )
        );

        String eventSignature = EventEncoder.encode(shipmentReceivedEvent);

        EthFilter filter = new EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            contractAddress
        ).addSingleTopic(eventSignature);

        web3j.ethLogFlowable(filter).subscribe(
            log -> handleShipmentReceived(log),
            error -> log.error("❌ Error listening to ShipmentReceived event", error)
        );

        log.info("✅ Listening to ShipmentReceived events");
    }

    /**
     * ✅ LISTEN EVENT: OwnershipTransferred
     * 
     * Event signature:
     * OwnershipTransferred(uint256 indexed batchId, address indexed from, address indexed to, uint256 timestamp)
     */
    private void listenToOwnershipTransferred() {
        Event ownershipEvent = new Event(
            "OwnershipTransferred",
            Arrays.asList(
                new TypeReference<Uint256>(true) {},    // batchId (indexed)
                new TypeReference<Address>(true) {},    // from (indexed)
                new TypeReference<Address>(true) {},    // to (indexed)
                new TypeReference<Uint256>() {}         // timestamp
            )
        );

        String eventSignature = EventEncoder.encode(ownershipEvent);

        EthFilter filter = new EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            contractAddress
        ).addSingleTopic(eventSignature);

        web3j.ethLogFlowable(filter).subscribe(
            log -> handleOwnershipTransferred(log),
            error -> log.error("❌ Error listening to OwnershipTransferred event", error)
        );

        log.info("✅ Listening to OwnershipTransferred events");
    }

    // ============================================================
    // EVENT HANDLERS
    // ============================================================

    /**
     * Xử lý event ShipmentReceived
     */
    private void handleShipmentReceived(Log eventLog) {
        try {
            // Parse event data
            String shipmentIdHex = eventLog.getTopics().get(1);
            String batchIdHex = eventLog.getTopics().get(2);
            String receiverHex = eventLog.getTopics().get(3);

            BigInteger batchId = new BigInteger(shipmentIdHex.substring(2), 16);
            String receiverAddress = "0x" + receiverHex.substring(26); // Extract address từ topic

            log.info("📥 ShipmentReceived Event: Batch {} received by {}", 
                batchId, receiverAddress);

            // Update database
            updateBatchOwner(batchId, receiverAddress);

        } catch (Exception e) {
            log.error("❌ Failed to handle ShipmentReceived event", e);
        }
    }

    /**
     * Xử lý event OwnershipTransferred
     */
    private void handleOwnershipTransferred(Log eventLog) {
        try {
            // Parse event data
            String batchIdHex = eventLog.getTopics().get(1);
            String fromHex = eventLog.getTopics().get(2);
            String toHex = eventLog.getTopics().get(3);

            BigInteger batchId = new BigInteger(batchIdHex.substring(2), 16);
            String fromAddress = "0x" + fromHex.substring(26);
            String toAddress = "0x" + toHex.substring(26);

            log.info("🔄 OwnershipTransferred Event: Batch {} from {} to {}", 
                batchId, fromAddress, toAddress);

            // Update database
            updateBatchOwner(batchId, toAddress);

        } catch (Exception e) {
            log.error("❌ Failed to handle OwnershipTransferred event", e);
        }
    }

    /**
     * Cập nhật owner của batch trong database
     */
    private void updateBatchOwner(BigInteger batchId, String newOwner) {
        try {
            Optional<DrugBatch> batchOpt = drugBatchRepository.findByBatchId(batchId);

            if (batchOpt.isEmpty()) {
                log.warn("⚠️ Batch {} not found in database", batchId);
                return;
            }

            DrugBatch batch = batchOpt.get();
            String oldOwner = batch.getCurrentOwner();

            if (oldOwner != null && oldOwner.equalsIgnoreCase(newOwner)) {
                log.debug("ℹ️ Batch {} already owned by {}, skipping", batchId, newOwner);
                return;
            }

            // Update ownership
            batch.setCurrentOwner(newOwner.toLowerCase());
            batch.setUpdatedAt(java.time.LocalDateTime.now());
            batch.setIsSynced(true);

            drugBatchRepository.save(batch);

            log.info("✅ Database updated: Batch {} ownership changed from {} to {}", 
                batchId, oldOwner, newOwner);

        } catch (Exception e) {
            log.error("❌ Failed to update batch owner in database", e);
        }
    }
}

