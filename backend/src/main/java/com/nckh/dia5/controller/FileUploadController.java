package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /**
     * Base URL của backend server — được cấu hình trong application.properties.
     * Phải là IP thực của máy (VD: http://192.168.0.103:8080) để
     * các thiết bị khác (Android, web trên máy khác) truy cập được ảnh upload.
     */
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestPart(value = "file") MultipartFile file) {
        log.info("=== FILE UPLOAD REQUEST RECEIVED ===");
        try {
            if (file == null) {
                log.warn("File is null");
                return ResponseEntity.badRequest().body(ApiResponse.error("File is null", 400));
            }

            log.info("File name: {}, size: {} bytes, content-type: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn một tập tin", 400));
            }

            // Use absolute path for uploads directory
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");
            log.info("Upload directory: {}", uploadPath.toAbsolutePath());

            File uploadDir = uploadPath.toFile();
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                log.info("Created upload directory: {} -> {}", uploadPath.toAbsolutePath(), created);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + extension;
            File destFile = new File(uploadDir, fileName);

            // Transfer file
            file.transferTo(destFile);
            log.info("File saved successfully: {}", destFile.getAbsolutePath());

            // ✅ FIX: Dùng baseUrl từ config thay vì hardcode localhost
            // baseUrl = http://192.168.0.103:8080 (IP thực, cấu hình trong application.properties)
            String cleanBase = baseUrl.replaceAll("/+$", "");
            String fileUrl = cleanBase + "/uploads/" + fileName;
            log.info("File URL (with real IP): {}", fileUrl);

            return ResponseEntity.ok(ApiResponse.success(fileUrl, "Tải tệp tin lên thành công"));
        } catch (Exception e) {
            log.error("Error during file upload: ", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("Không thể tải tệp tin lên: " + e.getClass().getSimpleName() + " - " + e.getMessage(), 500)
            );
        }
    }
}
