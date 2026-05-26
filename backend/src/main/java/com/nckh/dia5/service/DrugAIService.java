package com.nckh.dia5.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Service using OpenRouter API for drug consultation
 * 
 * OpenRouter provides access to multiple AI models (GPT-4, Claude, Gemini, etc.)
 * with a unified API interface.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrugAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${openrouter.model:google/gemini-2.0-flash-001}")
    private String model;

    private static final String SYSTEM_PROMPT = """
        Bạn là một trợ lý dược sĩ AI thông minh, chuyên tư vấn về thuốc và sức khỏe.
        
        NHIỆM VỤ:
        - Cung cấp thông tin về thuốc: công dụng, cách dùng, liều lượng, tác dụng phụ
        - Cảnh báo về tương tác thuốc và chống chỉ định
        - Hướng dẫn sử dụng thuốc an toàn
        - Trả lời câu hỏi về sức khỏe cơ bản
        
        QUY TẮC:
        1. Luôn nhắc nhở người dùng tham khảo ý kiến bác sĩ/dược sĩ chuyên nghiệp
        2. KHÔNG đưa ra chẩn đoán bệnh
        3. KHÔNG thay thế lời khuyên y tế chuyên nghiệp
        4. Cung cấp thông tin chính xác, dễ hiểu
        5. Trả lời bằng tiếng Việt
        6. Sử dụng emoji phù hợp để dễ đọc
        
        FORMAT TRẢ LỜI:
        - Ngắn gọn, súc tích
        - Chia thành các mục rõ ràng khi cần
        - Đánh dấu thông tin quan trọng
        """;

    /**
     * Get AI response for a drug-related question
     */
    public AIResponse askAboutDrug(String drugName, String question) {
        String prompt = String.format("""
            Người dùng hỏi về thuốc: %s
            
            Câu hỏi: %s
            
            Hãy trả lời chi tiết và chính xác.
            """, drugName, question);

        return sendToAI(prompt);
    }

    /**
     * Get comprehensive drug information
     */
    public AIResponse getDrugInfo(String drugName) {
        String prompt = String.format("""
            Hãy cung cấp thông tin chi tiết về thuốc: %s
            
            Bao gồm:
            1. 💊 Công dụng chính
            2. 📋 Liều dùng khuyến nghị
            3. ⏰ Cách dùng (thời điểm, với bữa ăn...)
            4. ⚠️ Tác dụng phụ thường gặp
            5. 🚫 Chống chỉ định
            6. 🔄 Tương tác thuốc cần tránh
            7. 💡 Lưu ý đặc biệt
            """, drugName);

        return sendToAI(prompt);
    }

    /**
     * Check drug interactions
     */
    public AIResponse checkDrugInteraction(List<String> drugNames) {
        String drugsStr = String.join(", ", drugNames);
        String prompt = String.format("""
            Kiểm tra tương tác thuốc giữa các thuốc sau: %s
            
            Hãy phân tích:
            1. ⚠️ Có tương tác nghiêm trọng không?
            2. 🔄 Các tương tác nhẹ/trung bình
            3. 💡 Khuyến nghị sử dụng an toàn
            4. ⏰ Có nên uống cách nhau không?
            """, drugsStr);

        return sendToAI(prompt);
    }

    /**
     * Get medication reminders and tips
     */
    public AIResponse getMedicationTips(String drugName, String dosage, int frequency) {
        String prompt = String.format("""
            Người dùng đang uống thuốc: %s
            Liều lượng: %s
            Số lần/ngày: %d
            
            Hãy đưa ra:
            1. ⏰ Lời khuyên về thời điểm uống tối ưu
            2. 🍽️ Nên uống trước/sau bữa ăn?
            3. 💧 Uống với nước hay thức uống khác?
            4. ⚠️ Những điều cần tránh khi dùng thuốc này
            5. 💡 Mẹo giúp nhớ uống thuốc đúng giờ
            """, drugName, dosage, frequency);

        return sendToAI(prompt);
    }

    /**
     * Consult about a specific drug with DB context
     * Builds a comprehensive prompt with drug details from database
     */
    public AIResponse consultDrug(String drugName, com.nckh.dia5.model.DrugProduct dbInfo) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format("""
            Người dùng đang dùng thuốc: %s
            Họ muốn hiểu rõ hơn về thuốc này.
            
            """, drugName));

        // Inject DB context if available
        if (dbInfo != null) {
            promptBuilder.append("DỮ LIỆU TỪ HỆ THỐNG (dùng để tham khảo, bổ sung thêm kiến thức của bạn):\n");
            if (dbInfo.getActiveIngredient() != null)
                promptBuilder.append("- Hoạt chất: ").append(dbInfo.getActiveIngredient()).append("\n");
            if (dbInfo.getDosage() != null)
                promptBuilder.append("- Hàm lượng: ").append(dbInfo.getDosage()).append("\n");
            if (dbInfo.getCategory() != null)
                promptBuilder.append("- Danh mục: ").append(dbInfo.getCategory()).append("\n");
            if (dbInfo.getDescription() != null)
                promptBuilder.append("- Mô tả: ").append(dbInfo.getDescription()).append("\n");
            if (dbInfo.getIndications() != null)
                promptBuilder.append("- Chỉ định: ").append(dbInfo.getIndications()).append("\n");
            if (dbInfo.getContraindications() != null)
                promptBuilder.append("- Chống chỉ định: ").append(dbInfo.getContraindications()).append("\n");
            if (dbInfo.getSideEffects() != null)
                promptBuilder.append("- Tác dụng phụ: ").append(dbInfo.getSideEffects()).append("\n");
            if (dbInfo.getPrecautions() != null)
                promptBuilder.append("- Thận trọng: ").append(dbInfo.getPrecautions()).append("\n");
            if (dbInfo.getDrugInteractions() != null)
                promptBuilder.append("- Tương tác thuốc: ").append(dbInfo.getDrugInteractions()).append("\n");
            if (dbInfo.getUsageInstructions() != null)
                promptBuilder.append("- Hướng dẫn sử dụng: ").append(dbInfo.getUsageInstructions()).append("\n");
            promptBuilder.append("\n");
        }

        promptBuilder.append("""
            Hãy trả lời CHI TIẾT theo các mục sau (dùng emoji để dễ đọc):
            
            1. 💊 **Công dụng chính** - Vì sao bạn cần uống thuốc này?
            2. 🚫 **Chống chỉ định** - Ai KHÔNG nên dùng thuốc này?
            3. ⚠️ **Tác dụng phụ** - Những phản ứng có thể gặp
            4. 🔄 **Tương tác thuốc** - Không nên dùng chung với thuốc nào?
            5. 📋 **Cách dùng đúng** - Thời điểm, liều lượng, lưu ý
            6. 🛡️ **Thận trọng đặc biệt** - Đối tượng cần cẩn thận (phụ nữ mang thai, trẻ em, người già...)
            7. 💡 **Lời khuyên** - Mẹo sử dụng hiệu quả, khi nào cần gặp bác sĩ
            8. 🤗 **Trấn an** - Giải thích ngắn gọn giúp người dùng yên tâm khi sử dụng đúng cách
            
            Trả lời bằng tiếng Việt, dễ hiểu, thân thiện.
            """);

        return sendToAI(promptBuilder.toString());
    }

    /**
     * General chat about health/medication
     */
    public AIResponse chat(String userMessage, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        // Chat history
        if (history != null) {
            for (ChatMessage msg : history) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        // Current message
        messages.add(Map.of("role", "user", "content", userMessage));

        return sendToAI(messages);
    }

    /**
     * Send request to OpenRouter API
     */
    private AIResponse sendToAI(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", prompt));
        return sendToAI(messages);
    }

    private AIResponse sendToAI(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenRouter API key not configured");
            return AIResponse.error("AI service chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        try {
            // Build request
            OpenRouterRequest request = new OpenRouterRequest();
            request.setModel(model);
            request.setMessages(messages);
            request.setMaxTokens(1024);
            request.setTemperature(0.7);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://pharmaledger.vn");
            headers.set("X-Title", "PharmaLedger Drug Assistant");

            HttpEntity<OpenRouterRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending request to OpenRouter: model={}", model);

            // Call API
            ResponseEntity<OpenRouterResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    OpenRouterResponse.class
            );

            if (response.getBody() != null && response.getBody().getChoices() != null
                    && !response.getBody().getChoices().isEmpty()) {

                String content = response.getBody().getChoices().get(0).getMessage().getContent();
                
                return AIResponse.builder()
                        .success(true)
                        .message(content)
                        .model(model)
                        .tokensUsed(response.getBody().getUsage() != null ?
                                response.getBody().getUsage().getTotalTokens() : 0)
                        .build();
            }

            return AIResponse.error("Không nhận được phản hồi từ AI");

        } catch (Exception e) {
            log.error("OpenRouter API error: {}", e.getMessage(), e);
            return AIResponse.error("Lỗi kết nối AI: " + e.getMessage());
        }
    }

    // ============== DTOs ==============

    @Data
    public static class OpenRouterRequest {
        private String model;
        private List<Map<String, String>> messages;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private double temperature;
    }

    @Data
    public static class OpenRouterResponse {
        private String id;
        private String model;
        private List<Choice> choices;
        private Usage usage;

        @Data
        public static class Choice {
            private int index;
            private Message message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Data
        public static class Message {
            private String role;
            private String content;
        }

        @Data
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("completion_tokens")
            private int completionTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AIResponse {
        private boolean success;
        private String message;
        private String model;
        private int tokensUsed;
        private String errorMessage;

        public static AIResponse error(String message) {
            return AIResponse.builder()
                    .success(false)
                    .errorMessage(message)
                    .build();
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatMessage {
        private String role; // "user" or "assistant"
        private String content;
    }
}
