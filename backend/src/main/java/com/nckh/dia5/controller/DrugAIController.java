package com.nckh.dia5.controller;

import com.nckh.dia5.dto.common.ApiResponse;
import com.nckh.dia5.model.DrugProduct;
import com.nckh.dia5.repository.DrugProductRepository;
import com.nckh.dia5.service.DrugAIService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for AI-powered drug consultation
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/drug")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DrugAIController {

    private final DrugAIService drugAIService;
    private final DrugProductRepository drugProductRepository;

    /**
     * Consult about a specific drug - auto-fetches DB context
     * POST /api/ai/drug/consult
     */
    @PostMapping("/consult")
    public ResponseEntity<ApiResponse<Map<String, Object>>> consultDrug(
            @RequestBody DrugConsultRequest request) {

        try {
            log.info("AI drug consult request: {}", request.getDrugName());

            // Look up drug in database for context
            DrugProduct dbInfo = drugProductRepository
                    .findFirstByNameContainingIgnoreCase(request.getDrugName())
                    .orElse(null);

            if (dbInfo != null) {
                log.info("Found drug in DB: {} (id={})", dbInfo.getName(), dbInfo.getId());
            } else {
                log.info("Drug not found in DB, AI will use general knowledge");
            }

            var response = drugAIService.consultDrug(request.getDrugName(), dbInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("drugName", request.getDrugName());
            result.put("response", response.getMessage());
            result.put("model", response.getModel());
            result.put("hasDbData", dbInfo != null);
            result.put("tokensUsed", response.getTokensUsed());
            if (dbInfo != null && dbInfo.getArticleUrl() != null) {
                result.put("articleUrl", dbInfo.getArticleUrl());
            }

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Tư vấn thuốc thành công"));

        } catch (Exception e) {
            log.error("Drug consult error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }
    /**
     * Get drug info from DATABASE only (no AI call)
     * Returns stored drug details for instant display
     * GET /api/ai/drug/db-info?name=Paracetamol
     */
    @GetMapping("/db-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDrugDbInfo(
            @RequestParam String name) {

        try {
            log.info("Drug DB info request: {}", name);

            DrugProduct dbInfo = drugProductRepository
                    .findFirstByNameContainingIgnoreCase(name)
                    .orElse(null);

            if (dbInfo == null) {
                Map<String, Object> notFound = new HashMap<>();
                notFound.put("found", false);
                notFound.put("drugName", name);
                return ResponseEntity.ok(ApiResponse.success(
                        notFound,
                        "Không tìm thấy thông tin thuốc trong CSDL"
                ));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("found", true);
            result.put("drugName", dbInfo.getName());
            result.put("activeIngredient", dbInfo.getActiveIngredient());
            result.put("dosage", dbInfo.getDosage());
            result.put("unit", dbInfo.getUnit());
            result.put("category", dbInfo.getCategory());
            result.put("description", dbInfo.getDescription());
            result.put("indications", dbInfo.getIndications());
            result.put("contraindications", dbInfo.getContraindications());
            result.put("sideEffects", dbInfo.getSideEffects());
            result.put("precautions", dbInfo.getPrecautions());
            result.put("drugInteractions", dbInfo.getDrugInteractions());
            result.put("usageInstructions", dbInfo.getUsageInstructions());
            result.put("storageConditions", dbInfo.getStorageConditions());
            result.put("imageUrl", dbInfo.getImageUrl());
            result.put("articleUrl", dbInfo.getArticleUrl());

            return ResponseEntity.ok(ApiResponse.success(result, "Thông tin thuốc từ CSDL"));

        } catch (Exception e) {
            log.error("Drug DB info error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    /**
     * Get comprehensive information about a drug
     * GET /api/ai/drug/info?name=Paracetamol
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDrugInfo(
            @RequestParam String name) {

        try {
            log.info("AI drug info request: {}", name);

            var response = drugAIService.getDrugInfo(name);

            Map<String, Object> result = new HashMap<>();
            result.put("drugName", name);
            result.put("content", response.getMessage());
            result.put("model", response.getModel());
            result.put("tokensUsed", response.getTokensUsed());

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Thông tin thuốc"));

        } catch (Exception e) {
            log.error("Drug info error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    /**
     * Ask a question about a specific drug
     * POST /api/ai/drug/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<Map<String, Object>>> askAboutDrug(
            @RequestBody DrugQuestionRequest request) {

        try {
            log.info("AI drug question: {} - {}", request.getDrugName(), request.getQuestion());

            var response = drugAIService.askAboutDrug(request.getDrugName(), request.getQuestion());

            Map<String, Object> result = new HashMap<>();
            result.put("drugName", request.getDrugName());
            result.put("question", request.getQuestion());
            result.put("answer", response.getMessage());
            result.put("model", response.getModel());

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Trả lời câu hỏi"));

        } catch (Exception e) {
            log.error("Drug question error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    /**
     * Check interactions between multiple drugs
     * POST /api/ai/drug/interactions
     */
    @PostMapping("/interactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkInteractions(
            @RequestBody DrugInteractionRequest request) {

        try {
            log.info("AI drug interaction check: {}", request.getDrugNames());

            if (request.getDrugNames() == null || request.getDrugNames().size() < 2) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Cần ít nhất 2 loại thuốc để kiểm tra tương tác", 400));
            }

            var response = drugAIService.checkDrugInteraction(request.getDrugNames());

            Map<String, Object> result = new HashMap<>();
            result.put("drugs", request.getDrugNames());
            result.put("analysis", response.getMessage());
            result.put("model", response.getModel());

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Phân tích tương tác thuốc"));

        } catch (Exception e) {
            log.error("Drug interaction error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    /**
     * Get medication tips based on prescription
     * POST /api/ai/drug/tips
     */
    @PostMapping("/tips")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMedicationTips(
            @RequestBody MedicationTipsRequest request) {

        try {
            log.info("AI medication tips: {} - {} x {}/day", 
                    request.getDrugName(), request.getDosage(), request.getFrequency());

            var response = drugAIService.getMedicationTips(
                    request.getDrugName(),
                    request.getDosage(),
                    request.getFrequency()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("drugName", request.getDrugName());
            result.put("dosage", request.getDosage());
            result.put("frequency", request.getFrequency());
            result.put("tips", response.getMessage());
            result.put("model", response.getModel());

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Lời khuyên sử dụng thuốc"));

        } catch (Exception e) {
            log.error("Medication tips error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    /**
     * General chat about health/medication
     * POST /api/ai/drug/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(
            @RequestBody ChatRequest request) {

        try {
            log.info("AI chat: {}", request.getMessage());

            // Convert history if provided
            List<DrugAIService.ChatMessage> history = null;
            if (request.getHistory() != null) {
                history = request.getHistory().stream()
                        .map(h -> DrugAIService.ChatMessage.builder()
                                .role(h.getRole())
                                .content(h.getContent())
                                .build())
                        .toList();
            }

            var response = drugAIService.chat(request.getMessage(), history);

            Map<String, Object> result = new HashMap<>();
            result.put("userMessage", request.getMessage());
            result.put("response", response.getMessage());
            result.put("model", response.getModel());
            result.put("tokensUsed", response.getTokensUsed());

            if (!response.isSuccess()) {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error(response.getErrorMessage(), 500));
            }

            return ResponseEntity.ok(ApiResponse.success(result, "Phản hồi từ AI"));

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage(), 500));
        }
    }

    // ============== Request DTOs ==============

    @Data
    public static class DrugQuestionRequest {
        private String drugName;
        private String question;
    }

    @Data
    public static class DrugConsultRequest {
        private String drugName;
    }

    @Data
    public static class DrugInteractionRequest {
        private List<String> drugNames;
    }

    @Data
    public static class MedicationTipsRequest {
        private String drugName;
        private String dosage;
        private int frequency;
    }

    @Data
    public static class ChatRequest {
        private String message;
        private List<ChatHistoryItem> history;
    }

    @Data
    public static class ChatHistoryItem {
        private String role;
        private String content;
    }
}
