import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Debug script để kiểm tra encoding issues trong blockchain transactions
 * Chạy script này để test Vietnamese diacritics và hex encoding
 */
public class DebugBlockchainEncoding {
    
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    
    public static void main(String[] args) {
        System.out.println("=== BLOCKCHAIN ENCODING DEBUG ===\n");
        
        // Test Vietnamese strings
        String[] testStrings = {
            "Paracetamol 500mg",
            "Thuốc đau đầu",
            "Công ty Dược ABC",
            "Amoxicillin 250mg",
            "Vitamin C 1000mg"
        };
        
        System.out.println("1. VIETNAMESE DIACRITICS REMOVAL TEST:");
        for (String str : testStrings) {
            String cleaned = removeVietnameseDiacritics(str);
            String safe = toBlockchainSafeString(str);
            
            System.out.printf("Original: '%s'\n", str);
            System.out.printf("No diacritics: '%s'\n", cleaned);
            System.out.printf("Blockchain safe: '%s'\n", safe);
            System.out.printf("UTF-8 bytes: %s\n", bytesToHex(str.getBytes(StandardCharsets.UTF_8)));
            System.out.printf("Safe UTF-8 bytes: %s\n", bytesToHex(safe.getBytes(StandardCharsets.UTF_8)));
            System.out.println("---");
        }
        
        System.out.println("\n2. ITEM CODE GENERATION TEST:");
        for (String drugName : testStrings) {
            String itemCode = generateItemCodeFromDrugName(drugName);
            System.out.printf("Drug: '%s' -> Item Code: '%s'\n", drugName, itemCode);
            System.out.printf("Item Code bytes: %s\n", bytesToHex(itemCode.getBytes(StandardCharsets.UTF_8)));
            System.out.println("---");
        }
        
        System.out.println("\n3. HEX ENCODING TEST:");
        String[] hexStrings = {
            "0x1234567890abcdef",
            "1234567890abcdef",
            "0x" + bytesToHex("Test String".getBytes(StandardCharsets.UTF_8))
        };
        
        for (String hex : hexStrings) {
            try {
                byte[] bytes = hexStringToBytes(hex);
                String reconstructed = new String(bytes, StandardCharsets.UTF_8);
                System.out.printf("Hex: '%s'\n", hex);
                System.out.printf("Bytes: %s\n", java.util.Arrays.toString(bytes));
                System.out.printf("Reconstructed: '%s'\n", reconstructed);
                System.out.println("---");
            } catch (Exception e) {
                System.out.printf("ERROR with hex '%s': %s\n", hex, e.getMessage());
            }
        }
        
        System.out.println("\n4. POTENTIAL ISSUES FOUND:");
        checkPotentialIssues();
    }
    
    private static String removeVietnameseDiacritics(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        
        return withoutDiacritics
                .replace("Đ", "D")
                .replace("đ", "d")
                .replace("Ð", "D")
                .replace("ð", "d");
    }
    
    private static String toBlockchainSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String withoutDiacritics = removeVietnameseDiacritics(input);
        
        return withoutDiacritics
                .toUpperCase()
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^A-Z0-9\\-_]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
    }
    
    private static String generateItemCodeFromDrugName(String drugName) {
        String normalizedName = removeVietnameseDiacritics(drugName);
        
        String cleanName = normalizedName.trim().split("\\s+")[0];
        char firstChar = Character.toUpperCase(cleanName.charAt(0));
        char lastChar = Character.toUpperCase(cleanName.charAt(cleanName.length() - 1));
        
        String dosage = "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*mg");
        java.util.regex.Matcher matcher = pattern.matcher(normalizedName);
        if (matcher.find()) {
            dosage = matcher.group(1);
        }
        
        String randomDigits = String.format("%07d", new java.util.Random().nextInt(10000000));
        
        return String.format("%c%c%s-%s", firstChar, lastChar, dosage, randomDigits);
    }
    
    private static byte[] hexStringToBytes(String hex) {
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
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private static void checkPotentialIssues() {
        System.out.println("- Check if Vietnamese characters are properly removed");
        System.out.println("- Verify hex encoding/decoding works correctly");
        System.out.println("- Ensure UTF-8 encoding is consistent");
        System.out.println("- Look for null bytes or invalid characters");
        System.out.println("- Check if Merkle proof generation creates valid hex strings");
        
        // Test edge cases
        String[] edgeCases = {
            "",
            null,
            "Special chars: @#$%^&*()",
            "Very long string: " + "A".repeat(1000),
            "Unicode: 🚀💊🏥",
            "Mixed: Thuốc ABC 500mg @#$"
        };
        
        System.out.println("\nEDGE CASE TESTS:");
        for (String test : edgeCases) {
            try {
                if (test != null) {
                    String safe = toBlockchainSafeString(test);
                    System.out.printf("Input: '%s' -> Safe: '%s'\n", 
                        test.length() > 50 ? test.substring(0, 50) + "..." : test, safe);
                }
            } catch (Exception e) {
                System.out.printf("ERROR with input '%s': %s\n", test, e.getMessage());
            }
        }
    }
}
