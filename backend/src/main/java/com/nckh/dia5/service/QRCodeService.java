package com.nckh.dia5.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Service để generate QR codes cho product items
 * Hỗ trợ cả file và byte array output
 */
@Service
@Slf4j
public class QRCodeService {

    @Value("${qr.code.base-url:https://yourapp.com/verify}")
    private String baseUrl;

    @Value("${qr.code.size:300}")
    private int qrCodeSize;

    @Value("${qr.code.storage-path:./qr-codes}")
    private String qrStoragePath;

    private static final String QR_CODE_IMAGE_FORMAT = "PNG";

    /**
     * Generate QR code data (URL) cho item
     * CHỈ CHỨA ITEM CODE - KHÔNG CHỨA THÔNG TIN CÓ DẤU
     */
    public String generateQRCodeData(String itemCode) {
        // Chỉ trả về item code thuần túy, không thêm thông tin khác
        return itemCode;
    }

    /**
     * Generate QR code image và lưu vào file
     * @param itemCode Mã sản phẩm
     * @param filePath Đường dẫn file để lưu
     * @return Đường dẫn file đã lưu
     */
    public String generateQRCodeImage(String itemCode, String filePath) throws WriterException, IOException {
        String qrCodeData = generateQRCodeData(itemCode);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize,
                hints
        );

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, QR_CODE_IMAGE_FORMAT, path);

        log.info("Generated QR code for item: {} at path: {}", itemCode, filePath);
        return filePath;
    }

    /**
     * Generate QR code dạng byte array (để upload lên S3/MinIO)
     * @param itemCode Mã sản phẩm
     * @return Byte array của QR code image
     */
    public byte[] generateQRCodeBytes(String itemCode) throws WriterException, IOException {
        String qrCodeData = generateQRCodeData(itemCode);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize,
                hints
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_IMAGE_FORMAT, outputStream);
        
        log.debug("Generated QR code bytes for item: {}, size: {} bytes", itemCode, outputStream.size());
        return outputStream.toByteArray();
    }

    /**
     * Generate QR code với custom size
     */
    public byte[] generateQRCodeBytes(String itemCode, int width, int height) 
            throws WriterException, IOException {
        String qrCodeData = generateQRCodeData(itemCode);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_IMAGE_FORMAT, outputStream);
        
        return outputStream.toByteArray();
    }

    /**
     * Validate QR code data format
     */
    public boolean isValidQRCodeData(String qrCodeData) {
        return qrCodeData != null && qrCodeData.startsWith(baseUrl);
    }

    /**
     * Extract item code từ QR code data
     */
    public String extractItemCodeFromQRData(String qrCodeData) {
        if (!isValidQRCodeData(qrCodeData)) {
            throw new IllegalArgumentException("Invalid QR code data format");
        }
        return qrCodeData.substring(baseUrl.length() + 1);
    }

    /**
     * Generate filename cho QR code image
     */
    public String generateQRCodeFileName(String itemCode) {
        return String.format("qr_%s.png", itemCode.replace("/", "_"));
    }

    /**
     * Get full file path cho QR code
     */
    public String getQRCodeFilePath(String itemCode) {
        return String.format("%s/%s", qrStoragePath, generateQRCodeFileName(itemCode));
    }
}

