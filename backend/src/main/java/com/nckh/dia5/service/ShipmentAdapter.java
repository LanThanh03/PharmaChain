package com.nckh.dia5.service;

import com.nckh.dia5.model.Shipment;
import com.nckh.dia5.model.PharmaCompany;
import com.nckh.dia5.repository.PharmaCompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter service to handle conversion between old shipments structure 
 * (blockchain-based) and new drug_shipments structure (company-based)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentAdapter {
    
    private final PharmaCompanyRepository pharmaCompanyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert blockchain-based parameters to new Shipment entity
     */
    public Shipment createShipmentFromBlockchain(
            BigInteger shipmentId,
            String fromAddress,
            String toAddress,
            Long quantity,
            String trackingInfo,
            String transactionHash,
            BigInteger blockNumber) {
        
        Shipment shipment = new Shipment();
        
        // Generate shipment code
        shipment.setShipmentCode("SHIP-" + shipmentId.toString());
        
        // Store blockchain data in transient fields
        shipment.setShipmentId(shipmentId);
        shipment.setFromAddress(fromAddress);
        shipment.setToAddress(toAddress);
        shipment.setTrackingInfo(trackingInfo);
        shipment.setBlockNumber(blockNumber);
        
        // Find or create companies based on addresses
        PharmaCompany fromCompany = findOrCreateCompanyByAddress(fromAddress, "Manufacturer");
        PharmaCompany toCompany = findOrCreateCompanyByAddress(toAddress, "Distributor");
        
        shipment.setFromCompany(fromCompany);
        shipment.setToCompany(toCompany);
        
        // Set quantity (convert Long to Integer)
        shipment.setQuantity(quantity != null ? quantity.intValue() : 0);
        
        // Set dates
        shipment.setShipmentDate(LocalDateTime.now());
        shipment.setExpectedDeliveryDate(LocalDateTime.now().plusDays(3));
        
        // Set transaction hash
        shipment.setCreateTxHash(transactionHash);
        
        // Store blockchain data in notes as JSON
        Map<String, Object> blockchainData = new HashMap<>();
        blockchainData.put("original_shipment_id", shipmentId.toString());
        blockchainData.put("from_address", fromAddress);
        blockchainData.put("to_address", toAddress);
        blockchainData.put("tracking_info", trackingInfo);
        blockchainData.put("block_number", blockNumber != null ? blockNumber.toString() : null);
        blockchainData.put("is_synced", true);
        
        try {
            shipment.setNotes(objectMapper.writeValueAsString(blockchainData));
        } catch (Exception e) {
            log.error("Failed to serialize blockchain data", e);
            shipment.setNotes(blockchainData.toString());
        }
        
        return shipment;
    }
    
    /**
     * Extract blockchain data from Shipment entity
     */
    public Map<String, Object> extractBlockchainData(Shipment shipment) {
        Map<String, Object> data = new HashMap<>();
        
        // Try to parse from notes
        if (shipment.getNotes() != null && !shipment.getNotes().isEmpty()) {
            try {
                ObjectNode jsonNode = objectMapper.readValue(shipment.getNotes(), ObjectNode.class);
                
                // Extract blockchain fields
                if (jsonNode.has("original_shipment_id")) {
                    data.put("shipmentId", new BigInteger(jsonNode.get("original_shipment_id").asText()));
                }
                if (jsonNode.has("from_address")) {
                    data.put("fromAddress", jsonNode.get("from_address").asText());
                }
                if (jsonNode.has("to_address")) {
                    data.put("toAddress", jsonNode.get("to_address").asText());
                }
                if (jsonNode.has("tracking_info")) {
                    data.put("trackingInfo", jsonNode.get("tracking_info").asText());
                }
                if (jsonNode.has("block_number")) {
                    String blockNum = jsonNode.get("block_number").asText();
                    if (blockNum != null && !blockNum.equals("null")) {
                        data.put("blockNumber", new BigInteger(blockNum));
                    }
                }
                if (jsonNode.has("is_synced")) {
                    data.put("isSynced", jsonNode.get("is_synced").asBoolean());
                }
            } catch (Exception e) {
                log.error("Failed to parse blockchain data from notes", e);
            }
        }
        
        // Fallback to extracting from shipment code
        if (!data.containsKey("shipmentId") && shipment.getShipmentCode() != null) {
            String code = shipment.getShipmentCode();
            if (code.startsWith("SHIP-")) {
                try {
                    data.put("shipmentId", new BigInteger(code.substring(5)));
                } catch (NumberFormatException e) {
                    log.warn("Failed to extract shipment ID from code: {}", code);
                }
            }
        }
        
        // Get addresses from companies if not in notes
        if (!data.containsKey("fromAddress") && shipment.getFromCompany() != null) {
            data.put("fromAddress", shipment.getFromCompany().getWalletAddress());
        }
        if (!data.containsKey("toAddress") && shipment.getToCompany() != null) {
            data.put("toAddress", shipment.getToCompany().getWalletAddress());
        }
        
        // Set default values
        data.putIfAbsent("trackingInfo", "");
        data.putIfAbsent("isSynced", false);
        
        return data;
    }
    
    /**
     * Find or create a pharma company by wallet address
     */
    private PharmaCompany findOrCreateCompanyByAddress(String walletAddress, String defaultType) {
        if (walletAddress == null || walletAddress.isEmpty()) {
            // Return a default company
            return pharmaCompanyRepository.findById(1L)
                .orElseGet(() -> createDefaultCompany(defaultType));
        }
        
        // Try to find by wallet address
        Optional<PharmaCompany> existing = pharmaCompanyRepository.findByWalletAddress(walletAddress);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new company without setting ID (let database auto-generate)
        PharmaCompany company = new PharmaCompany();
        company.setName("Company-" + walletAddress.substring(0, 8));
        company.setWalletAddress(walletAddress);
        company.setEmail("contact@" + walletAddress.substring(0, 8) + ".com");
        company.setStatus("ACTIVE");
        
        // Set required fields for the new model
        if (defaultType.equals("Manufacturer")) {
            company.setCompanyType(PharmaCompany.CompanyType.MANUFACTURER);
        } else if (defaultType.equals("Distributor")) {
            company.setCompanyType(PharmaCompany.CompanyType.DISTRIBUTOR);
        } else {
            company.setCompanyType(PharmaCompany.CompanyType.PHARMACY);
        }
        company.setIsActive(true);
        company.setBlockchainVerified(false);
        
        try {
            return pharmaCompanyRepository.save(company);
        } catch (Exception e) {
            log.error("Failed to create company for address: {}", walletAddress, e);
            return createDefaultCompany(defaultType);
        }
    }
    
    /**
     * Create a default company for fallback
     */
    private PharmaCompany createDefaultCompany(String type) {
        PharmaCompany company = new PharmaCompany();
        company.setName("Default " + type);
        company.setStatus("ACTIVE");
        
        // Set required fields
        if (type.equals("Manufacturer")) {
            company.setCompanyType(PharmaCompany.CompanyType.MANUFACTURER);
        } else if (type.equals("Distributor")) {
            company.setCompanyType(PharmaCompany.CompanyType.DISTRIBUTOR);
        } else {
            company.setCompanyType(PharmaCompany.CompanyType.PHARMACY);
        }
        company.setIsActive(true);
        company.setBlockchainVerified(false);
        
        try {
            return pharmaCompanyRepository.save(company);
        } catch (Exception e) {
            log.error("Failed to create default company", e);
            // Return the first company from database as ultimate fallback
            return pharmaCompanyRepository.findById(1L).orElse(null);
        }
    }
    
    /**
     * Update shipment blockchain data in notes
     */
    public void updateBlockchainData(Shipment shipment, String key, Object value) {
        try {
            ObjectNode jsonNode;
            if (shipment.getNotes() != null && !shipment.getNotes().isEmpty()) {
                jsonNode = objectMapper.readValue(shipment.getNotes(), ObjectNode.class);
            } else {
                jsonNode = objectMapper.createObjectNode();
            }
            
            if (value instanceof BigInteger) {
                jsonNode.put(key, value.toString());
            } else if (value instanceof Boolean) {
                jsonNode.put(key, (Boolean) value);
            } else {
                jsonNode.put(key, value != null ? value.toString() : null);
            }
            
            shipment.setNotes(objectMapper.writeValueAsString(jsonNode));
        } catch (Exception e) {
            log.error("Failed to update blockchain data", e);
        }
    }
}
