package com.nckh.dia5.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility để clean raw input bị corrupt và extract item codes
 */
@Component
@Slf4j
public class RawInputCleaner {
    
    // Pattern để tìm item codes hợp lệ: XX000-0000000
    private static final Pattern ITEM_CODE_PATTERN = Pattern.compile("([A-Z]{2}\\d{3}-\\d{7})");

    /**
     * Extract item codes từ raw input bị corrupt
     * Ví dụ input: "�M�>��PL500-7017456PL500-4607284���"
     * Output: ["PL500-7017456", "PL500-4607284"]
     */
    public List<String> extractItemCodes(String rawInput) {
        List<String> itemCodes = new ArrayList<>();
        
        if (rawInput == null || rawInput.isEmpty()) {
            log.warn("Raw input is null or empty");
            return itemCodes;
        }
        
        try {
            log.debug("Processing raw input (length: {})", rawInput.length());
            
            // Log bytes để debug
            byte[] bytes = rawInput.getBytes(StandardCharsets.UTF_8);
            log.debug("Raw bytes (first 100): {}", bytesToHex(bytes, 100));
            
            // Remove all control characters and non-printable characters
            String cleaned = removeNonPrintableCharacters(rawInput);
            log.debug("After removing non-printable: '{}'", cleaned);
            
            // Extract all valid item codes
            Matcher matcher = ITEM_CODE_PATTERN.matcher(cleaned);
            while (matcher.find()) {
                String itemCode = matcher.group(1);
                if (!itemCodes.contains(itemCode)) { // Avoid duplicates
                    itemCodes.add(itemCode);
                    log.info("Found valid item code: {}", itemCode);
                }
            }
            
            log.info("Extracted {} valid item codes from corrupt input", itemCodes.size());
            
        } catch (Exception e) {
            log.error("Error extracting item codes from raw input", e);
        }
        
        return itemCodes;
    }
    
    /**
     * Remove all non-printable characters from string
     * Keep only ASCII printable characters (32-126)
     */
    private String removeNonPrintableCharacters(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        
        for (char c : input.toCharArray()) {
            // Keep only printable ASCII characters
            if (c >= 32 && c <= 126) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Clean corrupt input và return single string
     */
    public String cleanRawInput(String rawInput) {
        List<String> itemCodes = extractItemCodes(rawInput);
        return String.join(",", itemCodes);
    }
    
    /**
     * Validate if string contains corrupt data
     */
    public boolean isCorrupt(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // Check for non-printable characters
        for (char c : input.toCharArray()) {
            if (c < 32 || c > 126) {
                if (c != '\n' && c != '\r' && c != '\t') {
                    return true; // Found corrupt character
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get corruption statistics
     */
    public CorruptionStats analyzeInput(String input) {
        if (input == null) {
            return new CorruptionStats(0, 0, 0, 0);
        }
        
        int totalChars = input.length();
        int corruptChars = 0;
        int printableChars = 0;
        int controlChars = 0;
        
        for (char c : input.toCharArray()) {
            if (c < 32 || c > 126) {
                if (c == '\n' || c == '\r' || c == '\t') {
                    controlChars++;
                } else {
                    corruptChars++;
                }
            } else {
                printableChars++;
            }
        }
        
        return new CorruptionStats(totalChars, printableChars, corruptChars, controlChars);
    }
    
    /**
     * Convert bytes to hex string for debugging (limit to first N bytes)
     */
    private String bytesToHex(byte[] bytes, int limit) {
        StringBuilder result = new StringBuilder();
        int len = Math.min(bytes.length, limit);
        
        for (int i = 0; i < len; i++) {
            result.append(String.format("%02x ", bytes[i]));
        }
        
        if (bytes.length > limit) {
            result.append("...");
        }
        
        return result.toString();
    }
    
    /**
     * Statistics about corruption in input
     */
    public static class CorruptionStats {
        public final int totalCharacters;
        public final int printableCharacters;
        public final int corruptCharacters;
        public final int controlCharacters;
        
        public CorruptionStats(int total, int printable, int corrupt, int control) {
            this.totalCharacters = total;
            this.printableCharacters = printable;
            this.corruptCharacters = corrupt;
            this.controlCharacters = control;
        }
        
        public double getCorruptionPercentage() {
            if (totalCharacters == 0) return 0.0;
            return (corruptCharacters * 100.0) / totalCharacters;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Total: %d, Printable: %d, Corrupt: %d (%.1f%%), Control: %d",
                totalCharacters, printableCharacters, corruptCharacters,
                getCorruptionPercentage(), controlCharacters
            );
        }
    }
}

