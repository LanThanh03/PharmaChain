package com.nckh.dia5.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utility để fix encoding issues trong blockchain transactions
 * Giải quyết vấn đề raw input bị corrupt trong Blockscout
 */
@Component
@Slf4j
public class BlockchainEncodingFixer {
    
    private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\x00-\\x7F]");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^A-Za-z0-9\\-_\\s]");
    
    /**
     * Clean và validate string trước khi gửi lên blockchain
     * @param input Raw string
     * @return Cleaned string safe for blockchain
     */
    public String cleanForBlockchain(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        try {
            // Step 1: Remove Vietnamese diacritics - enhanced version
            String cleaned = VietnameseUtils.removeVietnameseDiacritics(input);
            
            // Step 2: Remove non-ASCII characters completely
            cleaned = NON_ASCII_PATTERN.matcher(cleaned).replaceAll("");
            
            // Step 3: Remove special characters except allowed ones (A-Z, a-z, 0-9, -, _, space)
            cleaned = SPECIAL_CHARS_PATTERN.matcher(cleaned).replaceAll("");
            
            // Step 4: Normalize whitespace and replace with underscore for blockchain safety
            cleaned = cleaned.trim().replaceAll("\\s+", "_");
            
            // Step 5: Remove multiple underscores
            cleaned = cleaned.replaceAll("_+", "_");
            
            // Step 6: Remove leading/trailing underscores
            cleaned = cleaned.replaceAll("^_+|_+$", "");
            
            // Step 7: Ensure not empty
            if (cleaned.isEmpty()) {
                cleaned = "UNKNOWN";
            }
            
            // Step 8: Convert to uppercase for consistency
            cleaned = cleaned.toUpperCase();
            
            // Step 9: Limit length to prevent gas issues
            if (cleaned.length() > 100) {
                cleaned = cleaned.substring(0, 100);
            }
            
            log.debug("Cleaned string: '{}' -> '{}'", input, cleaned);
            return cleaned;
            
        } catch (Exception e) {
            log.error("Error cleaning string for blockchain: '{}'", input, e);
            return "UNKNOWN";
        }
    }
    
    /**
     * Validate hex string trước khi convert to bytes
     * @param hex Hex string
     * @return Valid hex string
     */
    public String validateHexString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "0x0";
        }
        
        try {
            // Remove 0x prefix if present
            String cleanHex = hex.startsWith("0x") ? hex.substring(2) : hex;
            
            // Validate hex characters
            if (!cleanHex.matches("[0-9a-fA-F]*")) {
                log.warn("Invalid hex string detected: '{}', replacing with zeros", hex);
                return "0x" + "0".repeat(Math.max(2, cleanHex.length()));
            }
            
            // Ensure even length
            if (cleanHex.length() % 2 != 0) {
                cleanHex = "0" + cleanHex;
            }
            
            // Ensure minimum length for bytes32
            if (cleanHex.length() < 64) {
                cleanHex = String.format("%-64s", cleanHex).replace(' ', '0');
            }
            
            return "0x" + cleanHex;
            
        } catch (Exception e) {
            log.error("Error validating hex string: '{}'", hex, e);
            return "0x" + "0".repeat(64); // Return valid bytes32 zero
        }
    }
    
    /**
     * Safe hex to bytes conversion
     * @param hex Hex string
     * @return Byte array
     */
    public byte[] safeHexToBytes(String hex) {
        try {
            String validHex = validateHexString(hex);
            return Numeric.hexStringToByteArray(validHex);
        } catch (Exception e) {
            log.error("Error converting hex to bytes: '{}'", hex, e);
            return new byte[32]; // Return 32 zero bytes for bytes32
        }
    }
    
    /**
     * Validate item code format
     * @param itemCode Item code
     * @return Valid item code
     */
    public String validateItemCode(String itemCode) {
        if (itemCode == null || itemCode.isEmpty()) {
            return generateFallbackItemCode();
        }
        
        try {
            // Clean the item code
            String cleaned = cleanForBlockchain(itemCode);
            
            // Ensure proper format: XX000-0000000
            if (!cleaned.matches("[A-Z]{2}\\d{3}-\\d{7}")) {
                log.warn("Invalid item code format: '{}', generating fallback", itemCode);
                return generateFallbackItemCode();
            }
            
            return cleaned;
            
        } catch (Exception e) {
            log.error("Error validating item code: '{}'", itemCode, e);
            return generateFallbackItemCode();
        }
    }
    
    /**
     * Generate fallback item code when validation fails
     * @return Valid fallback item code
     */
    private String generateFallbackItemCode() {
        long timestamp = System.currentTimeMillis() % 10000000; // Last 7 digits
        return String.format("XX000-%07d", timestamp);
    }
    
    /**
     * Validate batch number
     * @param batchNumber Batch number
     * @return Valid batch number
     */
    public String validateBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.isEmpty()) {
            return "BATCH-" + System.currentTimeMillis() % 100000;
        }
        
        String cleaned = cleanForBlockchain(batchNumber);
        
        // Ensure reasonable length
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        
        if (cleaned.isEmpty()) {
            return "BATCH-" + System.currentTimeMillis() % 100000;
        }
        
        return cleaned;
    }
    
    /**
     * Check if string contains problematic characters
     * @param input String to check
     * @return true if contains problematic characters
     */
    public boolean hasProblematicCharacters(String input) {
        if (input == null) {
            return false;
        }
        
        // Check for non-ASCII characters
        if (NON_ASCII_PATTERN.matcher(input).find()) {
            return true;
        }
        
        // Check for null bytes
        if (input.contains("\0")) {
            return true;
        }
        
        // Check for control characters
        for (char c : input.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Log encoding issues for debugging
     * @param original Original string
     * @param cleaned Cleaned string
     * @param context Context (e.g., "drug_name", "item_code")
     */
    public void logEncodingIssues(String original, String cleaned, String context) {
        if (original == null || cleaned == null) {
            return;
        }
        
        if (!original.equals(cleaned)) {
            log.info("Encoding fix applied in {}: '{}' -> '{}'", context, original, cleaned);
            
            if (hasProblematicCharacters(original)) {
                log.warn("Problematic characters detected in {}: '{}'", context, original);
                
                // Log byte representation for debugging
                byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
                byte[] cleanedBytes = cleaned.getBytes(StandardCharsets.UTF_8);
                
                log.debug("Original bytes ({}): {}", context, bytesToHex(originalBytes));
                log.debug("Cleaned bytes ({}): {}", context, bytesToHex(cleanedBytes));
            }
        }
    }
    
    /**
     * Convert bytes to hex string for debugging
     * @param bytes Byte array
     * @return Hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
