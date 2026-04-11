package com.farmchainX.farmchainX.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.farmchainX.farmchainX.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "groq.api.key", matchIfMissing = false)
public class GroqAIService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> generateFarmPrediction(Product product) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt = buildFarmPredictionPrompt(product);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3); // Lower for factual predictions
            requestBody.put("max_tokens", 1000);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "You are an expert agricultural advisor AI. Analyze farm product data and provide actionable insights including quality prediction, market readiness, storage recommendations, and optimal selling timeframe. Return response ONLY in valid JSON format without any markdown code blocks or additional text."));
            messages.add(Map.of("role", "user", "content", prompt));

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, request, Map.class);

            return parsePredictionResponse(response.getBody(), product);

        } catch (Exception e) {
            System.err.println("[GroqAI Error] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return createFallbackPrediction(product);
        }
    }

    private String buildFarmPredictionPrompt(Product product) {
        return String.format("""
                Analyze this agricultural product and provide comprehensive insights:

                Product Details:
                - Crop Name: %s
                - Soil Type: %s
                - Pesticides Used: %s
                - Harvest Date: %s
                - Location (GPS): %s

                Provide analysis in this exact JSON structure (return ONLY valid JSON, no markdown):
                {
                  "qualityGrade": "A or B or C",
                  "qualityScore": 85,
                  "confidence": 92,
                  "marketReadiness": "Ready/Wait 3-5 days/Immediate sale recommended",
                  "storageRecommendation": "specific storage advice",
                  "optimalSellingWindow": "timeframe",
                  "priceEstimate": "price range in local currency",
                  "insights": ["insight1", "insight2", "insight3"],
                  "warnings": ["warning if any"],
                  "certificationEligibility": "Organic/Conventional/Premium"
                }

                Consider soil quality, pesticide usage, freshness based on harvest date, and location climate.
                """,
                product.getCropName() != null ? product.getCropName() : "Unknown",
                product.getSoilType() != null ? product.getSoilType() : "Unknown",
                product.getPesticides() != null ? product.getPesticides() : "None",
                product.getHarvestDate() != null ? product.getHarvestDate().toString() : "Unknown",
                product.getGpsLocation() != null ? product.getGpsLocation() : "Unknown");
    }

    private Map<String, Object> parsePredictionResponse(Map<String, Object> apiResponse, Product product) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
            if (choices == null || choices.isEmpty()) {
                return createFallbackPrediction(product);
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Extract JSON from content (remove markdown code blocks if present)
            String jsonContent = extractJsonFromResponse(content);

            // Parse JSON
            Map<String, Object> prediction = objectMapper.readValue(jsonContent, Map.class);

            // Validate and set defaults if needed
            if (!prediction.containsKey("qualityGrade")) {
                prediction.put("qualityGrade", product.getQualityGrade() != null ? product.getQualityGrade() : "B");
            }
            if (!prediction.containsKey("qualityScore")) {
                prediction.put("qualityScore", product.getConfidenceScore() != null
                        ? (int) (product.getConfidenceScore() * 100)
                        : 75);
            }
            if (!prediction.containsKey("confidence")) {
                prediction.put("confidence", product.getConfidenceScore() != null
                        ? (int) (product.getConfidenceScore() * 100)
                        : 85);
            }

            return prediction;

        } catch (Exception e) {
            System.err.println("[GroqAI Parse Error] " + e.getMessage());
            return createFallbackPrediction(product);
        }
    }

    private String extractJsonFromResponse(String content) {
        if (content == null) {
            return "{}";
        }

        // Remove markdown code blocks if present
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Find JSON object boundaries
        int startIdx = cleaned.indexOf("{");
        int endIdx = cleaned.lastIndexOf("}");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return cleaned.substring(startIdx, endIdx + 1);
        }

        return cleaned.trim();
    }

    private Map<String, Object> createFallbackPrediction(Product product) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("qualityGrade", product.getQualityGrade() != null ? product.getQualityGrade() : "B");
        fallback.put("qualityScore", product.getConfidenceScore() != null
                ? (int) (product.getConfidenceScore() * 100)
                : 75);
        fallback.put("confidence", product.getConfidenceScore() != null
                ? (int) (product.getConfidenceScore() * 100)
                : 85);
        fallback.put("marketReadiness", "Ready for market");
        fallback.put("storageRecommendation", "Store in cool, dry place. Maintain proper ventilation.");
        fallback.put("optimalSellingWindow", "1-3 days after harvest");
        fallback.put("priceEstimate", "Market price varies");
        fallback.put("insights", Arrays.asList(
                "Product appears to be in good condition",
                "Monitor storage conditions regularly",
                "Consider local market demand"));
        fallback.put("warnings", new ArrayList<>());
        fallback.put("certificationEligibility", "Conventional");
        return fallback;
    }
}
