package com.nckh.dia5.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class để xử lý tiếng Việt
 * Chuyển tiếng Việt có dấu thành không dấu cho blockchain
 */
public class VietnameseUtils {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Chuyển chuỗi tiếng Việt có dấu thành không dấu
     * VD: "Công ty Dược ABC" -> "Cong ty Duoc ABC"
     */
    public static String removeVietnameseDiacritics(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Normalize và loại bỏ dấu
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");

        // Xử lý các ký tự đặc biệt của tiếng Việt - mở rộng để bao gồm tất cả
        return withoutDiacritics
                .replace("Đ", "D")
                .replace("đ", "d")
                .replace("Ð", "D")  // Alternative Đ
                .replace("ð", "d")  // Alternative đ
                .replace("Ơ", "O")  // Ơ
                .replace("ơ", "o")  // ơ
                .replace("Ư", "U")  // Ư
                .replace("ư", "u")  // ư
                .replace("Ạ", "A")  // Ạ
                .replace("ạ", "a")  // ạ
                .replace("Ả", "A")  // Ả
                .replace("ả", "a")  // ả
                .replace("Ấ", "A")  // Ấ
                .replace("ấ", "a")  // ấ
                .replace("Ầ", "A")  // Ầ
                .replace("ầ", "a")  // ầ
                .replace("Ẩ", "A")  // Ẩ
                .replace("ẩ", "a")  // ẩ
                .replace("Ẫ", "A")  // Ẫ
                .replace("ẫ", "a")  // ẫ
                .replace("Ậ", "A")  // Ậ
                .replace("ậ", "a")  // ậ
                .replace("Ắ", "A")  // Ắ
                .replace("ắ", "a")  // ắ
                .replace("Ằ", "A")  // Ằ
                .replace("ằ", "a")  // ằ
                .replace("Ẳ", "A")  // Ẳ
                .replace("ẳ", "a")  // ẳ
                .replace("Ẵ", "A")  // Ẵ
                .replace("ẵ", "a")  // ẵ
                .replace("Ặ", "A")  // Ặ
                .replace("ặ", "a")  // ặ
                .replace("Ẹ", "E")  // Ẹ
                .replace("ẹ", "e")  // ẹ
                .replace("Ẻ", "E")  // Ẻ
                .replace("ẻ", "e")  // ẻ
                .replace("Ẽ", "E")  // Ẽ
                .replace("ẽ", "e")  // ẽ
                .replace("Ế", "E")  // Ế
                .replace("ế", "e")  // ế
                .replace("Ề", "E")  // Ề
                .replace("ề", "e")  // ề
                .replace("Ể", "E")  // Ể
                .replace("ể", "e")  // ể
                .replace("Ễ", "E")  // Ễ
                .replace("ễ", "e")  // ễ
                .replace("Ệ", "E")  // Ệ
                .replace("ệ", "e")  // ệ
                .replace("Ỉ", "I")  // Ỉ
                .replace("ỉ", "i")  // ỉ
                .replace("Ị", "I")  // Ị
                .replace("ị", "i")  // ị
                .replace("Ọ", "O")  // Ọ
                .replace("ọ", "o")  // ọ
                .replace("Ỏ", "O")  // Ỏ
                .replace("ỏ", "o")  // ỏ
                .replace("Ố", "O")  // Ố
                .replace("ố", "o")  // ố
                .replace("Ồ", "O")  // Ồ
                .replace("ồ", "o")  // ồ
                .replace("Ổ", "O")  // Ổ
                .replace("ổ", "o")  // ổ
                .replace("Ỗ", "O")  // Ỗ
                .replace("ỗ", "o")  // ỗ
                .replace("Ộ", "O")  // Ộ
                .replace("ộ", "o")  // ộ
                .replace("Ớ", "O")  // Ớ
                .replace("ớ", "o")  // ớ
                .replace("Ờ", "O")  // Ờ
                .replace("ờ", "o")  // ờ
                .replace("Ở", "O")  // Ở
                .replace("ở", "o")  // ở
                .replace("Ỡ", "O")  // Ỡ
                .replace("ỡ", "o")  // ỡ
                .replace("Ợ", "O")  // Ợ
                .replace("ợ", "o")  // ợ
                .replace("Ụ", "U")  // Ụ
                .replace("ụ", "u")  // ụ
                .replace("Ủ", "U")  // Ủ
                .replace("ủ", "u")  // ủ
                .replace("Ứ", "U")  // Ứ
                .replace("ứ", "u")  // ứ
                .replace("Ừ", "U")  // Ừ
                .replace("ừ", "u")  // ừ
                .replace("Ử", "U")  // Ử
                .replace("ử", "u")  // ử
                .replace("Ữ", "U")  // Ữ
                .replace("ữ", "u")  // ữ
                .replace("Ự", "U")  // Ự
                .replace("ự", "u")  // ự
                .replace("Ỳ", "Y")  // Ỳ
                .replace("ỳ", "y")  // ỳ
                .replace("Ỵ", "Y")  // Ỵ
                .replace("ỵ", "y")  // ỵ
                .replace("Ỷ", "Y")  // Ỷ
                .replace("ỷ", "y")  // ỷ
                .replace("Ỹ", "Y")  // Ỹ
                .replace("ỹ", "y");  // ỹ
    }

    /**
     * Chuyển chuỗi thành format an toàn cho blockchain (chỉ A-Z, 0-9, -, _)
     * VD: "Paracetamol 500mg" -> "PARACETAMOL-500MG"
     */
    public static String toBlockchainSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Bỏ dấu tiếng Việt
        String withoutDiacritics = removeVietnameseDiacritics(input);

        // Chuyển thành chữ hoa, thay khoảng trắng bằng dấu gạch ngang
        // Chỉ giữ lại A-Z, 0-9, -, _
        return withoutDiacritics
                .toUpperCase()
                .trim()
                .replaceAll("\\s+", "-")           // Khoảng trắng -> gạch ngang
                .replaceAll("[^A-Z0-9\\-_]", "")   // Loại bỏ ký tự đặc biệt
                .replaceAll("-+", "-")             // Nhiều gạch ngang -> 1 gạch
                .replaceAll("^-+|-+$", "");        // Bỏ gạch đầu/cuối
    }

    /**
     * Generate item code an toàn cho blockchain
     * VD: "Paracetamol 500mg", "Công ty ABC", "LOT001", 1 
     *     -> "PARACETAMOL-500MG-CONG-TY-ABC-LOT001-00001"
     */
    public static String generateBlockchainItemCode(String drugName, String manufacturer, 
                                                     String batchNumber, int sequence) {
        String safeDrugName = toBlockchainSafeString(drugName);
        String safeManufacturer = toBlockchainSafeString(manufacturer);
        String safeBatchNumber = toBlockchainSafeString(batchNumber);
        
        // Limit length để tránh item code quá dài
        if (safeDrugName.length() > 20) {
            safeDrugName = safeDrugName.substring(0, 20);
        }
        if (safeManufacturer.length() > 15) {
            safeManufacturer = safeManufacturer.substring(0, 15);
        }
        if (safeBatchNumber.length() > 15) {
            safeBatchNumber = safeBatchNumber.substring(0, 15);
        }

        return String.format("%s-%s-%s-%05d", 
                safeDrugName, 
                safeManufacturer, 
                safeBatchNumber, 
                sequence);
    }

    /**
     * Chuyển drug name thành format ngắn gọn cho item code
     * VD: "Paracetamol 500mg" -> "PARA500MG"
     */
    public static String toShortDrugCode(String drugName) {
        if (drugName == null || drugName.isEmpty()) {
            return "DRUG";
        }

        String safe = toBlockchainSafeString(drugName);
        
        // Nếu quá dài, lấy 4 ký tự đầu + số
        if (safe.length() > 12) {
            // Tách phần chữ và phần số
            String[] parts = safe.split("-");
            if (parts.length > 0) {
                String firstPart = parts[0];
                if (firstPart.length() > 4) {
                    firstPart = firstPart.substring(0, 4);
                }
                
                // Tìm phần số (VD: 500MG)
                String numberPart = safe.replaceAll("[A-Z\\-]", "");
                if (!numberPart.isEmpty()) {
                    return firstPart + numberPart;
                }
                return firstPart;
            }
        }
        
        return safe;
    }
}

