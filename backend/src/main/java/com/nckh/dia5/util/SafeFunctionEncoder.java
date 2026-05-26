package com.nckh.dia5.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;

import java.util.regex.Pattern;

/**
 * Safe Function Encoder để tránh raw input corruption
 * Kiểm tra và làm sạch dữ liệu trước khi encode
 */
@Component
@Slf4j
public class SafeFunctionEncoder {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("^0x[0-9a-fA-F]*$");
    
    /**
     * Encode function với kiểm tra an toàn
     * @param function Function to encode
     * @return Safe encoded function string
     */
    public String safeEncode(Function function) {
        try {
            // Validate function name
            String functionName = function.getName();
            if (!isValidFunctionName(functionName)) {
                throw new IllegalArgumentException("Invalid function name: " + functionName);
            }
            
            // Log function details for debugging
            log.debug("Encoding function: {}", functionName);
            log.debug("Input parameters count: {}", function.getInputParameters().size());
            log.debug("Output parameters count: {}", function.getOutputParameters().size());
            
            // Validate parameters BEFORE encoding
            // NOTE: Do NOT clean/alter parameters here - cleaning should happen at input layer
            // before Function object is created
            validateParameters(function);
            
            // Encode function - this creates the final calldata with function selector
            String encoded = FunctionEncoder.encode(function);
            
            // Validate encoded result format (but DO NOT ALTER IT)
            if (!isValidHexString(encoded)) {
                throw new IllegalArgumentException("Encoded function is not valid hex: " + encoded);
            }
            
            // Log the function selector for debugging
            String selector = encoded.substring(0, 10);
            log.debug("Function encoded successfully: {} -> selector={}, length={}", 
                    functionName, selector, encoded.length());
            
            // CRITICAL: Return the encoded string AS-IS
            // Do NOT apply any hex validation/cleaning that might alter the selector
            return encoded;
            
        } catch (Exception e) {
            log.error("Failed to encode function safely: {}", e.getMessage(), e);
            throw new RuntimeException("Function encoding failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate function name
     */
    private boolean isValidFunctionName(String functionName) {
        if (functionName == null || functionName.isEmpty()) {
            return false;
        }
        
        // Check for valid function name pattern
        return functionName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
    
    /**
     * Validate function parameters
     */
    private void validateParameters(Function function) {
        if (function.getInputParameters() == null) {
            return;
        }
        
        for (int i = 0; i < function.getInputParameters().size(); i++) {
            var param = function.getInputParameters().get(i);
            try {
                validateParameter(param, i);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid parameter at index " + i + ": " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Validate individual parameter
     */
    private void validateParameter(Object param, int index) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter cannot be null");
        }
        
        // Check for common problematic types
        if (param instanceof String) {
            String str = (String) param;
            if (str.contains("\0")) {
                throw new IllegalArgumentException("String contains null bytes");
            }
            if (str.length() > 1000) {
                log.warn("String parameter at index {} is very long: {} characters", index, str.length());
            }
            
            // Check for Vietnamese characters and other problematic characters
            String cleaned = cleanString(str);
            if (!str.equals(cleaned)) {
                log.warn("String parameter at index {} contains problematic characters: '{}' -> '{}'", 
                        index, str, cleaned);
            }
        }
        
        // Add more validation as needed
    }
    
    /**
     * Check if string is valid hex
     */
    private boolean isValidHexString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return false;
        }
        
        return HEX_PATTERN.matcher(hex).matches();
    }
    
    /**
     * Clean string for safe encoding
     */
    public String cleanString(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove null bytes
        String cleaned = input.replace("\0", "");
        
        // Remove non-printable characters except whitespace
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // Remove Vietnamese diacritics
        cleaned = com.nckh.dia5.util.VietnameseUtils.removeVietnameseDiacritics(cleaned);
        
        // Remove non-ASCII characters
        cleaned = cleaned.replaceAll("[^\\x00-\\x7F]", "");
        
        // Remove special characters except A-Z, a-z, 0-9, -, _, space
        cleaned = cleaned.replaceAll("[^A-Za-z0-9\\-_\\s]", "");
        
        // Normalize whitespace
        cleaned = cleaned.trim().replaceAll("\\s+", " ");
        
        // Limit length
        if (cleaned.length() > 1000) {
            cleaned = cleaned.substring(0, 1000);
            log.warn("String truncated to 1000 characters");
        }
        
        return cleaned;
    }
    
    /**
     * Validate hex string
     */
    public String validateHexString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return "0x0";
        }
        
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
    }
    
    /**
     * Get safe bytes from hex string
     */
    public byte[] safeHexToBytes(String hex) {
        try {
            String validHex = validateHexString(hex);
            return hexStringToByteArray(validHex);
        } catch (Exception e) {
            log.error("Error converting hex to bytes: '{}'", hex, e);
            return new byte[32]; // Return 32 zero bytes for bytes32
        }
    }
    
    /**
     * Convert hex string to byte array
     */
    private byte[] hexStringToByteArray(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
