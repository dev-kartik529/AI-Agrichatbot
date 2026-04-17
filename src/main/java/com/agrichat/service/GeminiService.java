package com.agrichat.service;

import com.agrichat.entity.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.max-tokens:1024}")
    private int maxTokens;

    @Value("${gemini.api.temperature:0.7}")
    private double temperature;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------
    // AGRICULTURAL SYSTEM PROMPT - The core of the chatbot
    // -------------------------------------------------------
    private static final String SYSTEM_PROMPT = """
            You are AgriBot, an expert AI agricultural advisor specialized for Indian farmers.
            You provide practical, accurate, and location-aware farming advice.
            
            Your expertise covers:
            1. CROP DISEASE DIAGNOSIS: Identify diseases from symptoms. Provide:
               - Disease name and cause (fungal/bacterial/viral/pest)
               - Visual symptoms to confirm
               - Organic and chemical treatment options
               - Prevention strategies
            
            2. WEATHER-BASED ADVICE: Based on season/weather conditions, recommend:
               - Ideal sowing/harvesting windows
               - Irrigation scheduling
               - Weather risk mitigation (frost, drought, flood)
               - Crop selection per season
            
            3. FERTILIZER & PESTICIDE RECOMMENDATIONS:
               - NPK ratios and application timing
               - Soil-type specific fertilizer advice
               - Safe pesticide use with dosage
               - Organic alternatives (neem, compost, biofertilizers)
               - Government-registered product names when possible
            
            4. MARKET PRICE QUERIES:
               - Current Minimum Support Price (MSP) for major crops
               - Tips for getting better prices (APMC, FPO, direct selling)
               - Crop storage to sell at better prices
               - e-NAM and government schemes
            
            Guidelines:
            - Always respond in simple, clear language that farmers understand
            - Use both technical and common local names for crops and diseases
            - Mention if the farmer should consult a local Krishi Vigyan Kendra (KVK) for serious issues
            - For pesticides, always mention safety precautions
            - If you're unsure, say so and suggest the farmer verify with a local expert
            - Keep responses concise and actionable
            - If asked about something outside agriculture, politely redirect to agricultural topics
            """;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to Gemini API with full conversation history for context.
     *
     * @param userMessage   The new message from the farmer
     * @param chatHistory   Previous messages in this session for context
     * @return AI-generated reply text
     */
    public String generateReply(String userMessage, List<ChatMessage> chatHistory) {
        try {
            String url = apiUrl + "?key=" + apiKey;

            ObjectNode requestBody = buildRequestBody(userMessage, chatHistory);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "I'm sorry, I'm having trouble connecting right now. Please try again in a moment. " +
                   "For urgent farming issues, contact your nearest Krishi Vigyan Kendra (KVK).";
        }
    }

    /**
     * Builds the Gemini API request body with system prompt + conversation history.
     */
    private ObjectNode buildRequestBody(String userMessage, List<ChatMessage> chatHistory) {
        ObjectNode root = objectMapper.createObjectNode();

        // System instruction (sets the agricultural expert persona)
        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ObjectNode systemPart = objectMapper.createObjectNode();
        systemPart.put("text", SYSTEM_PROMPT);
        ArrayNode systemParts = objectMapper.createArrayNode();
        systemParts.add(systemPart);
        systemInstruction.set("parts", systemParts);
        root.set("systemInstruction", systemInstruction);

        // Build conversation history as "contents"
        ArrayNode contents = objectMapper.createArrayNode();

        // Add prior conversation messages (for context)
        for (ChatMessage msg : chatHistory) {
            String role = msg.getRole() == ChatMessage.MessageRole.USER ? "user" : "model";
            contents.add(buildContentNode(role, msg.getContent()));
        }

        // Add the current user message
        contents.add(buildContentNode("user", userMessage));
        root.set("contents", contents);

        // Generation config
        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", temperature);
        generationConfig.put("topP", 0.9);
        root.set("generationConfig", generationConfig);

        // Safety settings - allow agricultural content
        ArrayNode safetySettings = objectMapper.createArrayNode();
        String[] categories = {"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
                               "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"};
        for (String category : categories) {
            ObjectNode setting = objectMapper.createObjectNode();
            setting.put("category", category);
            setting.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.add(setting);
        }
        root.set("safetySettings", safetySettings);

        return root;
    }

    private ObjectNode buildContentNode(String role, String text) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", role);
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", text);
        parts.add(part);
        content.set("parts", parts);
        return content;
    }

    /**
     * Parses Gemini API response JSON and extracts the text.
     */
    private String parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Check for API-level error
        if (root.has("error")) {
            String errorMsg = root.path("error").path("message").asText("Unknown API error");
            log.error("Gemini API error: {}", errorMsg);
            return "I encountered an issue. Please try again. (" + errorMsg + ")";
        }

        // Extract text from candidates[0].content.parts[0].text
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            return "I couldn't generate a response. Please rephrase your question.";
        }

        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (parts.isEmpty()) {
            return "I couldn't generate a response. Please try again.";
        }

        return parts.get(0).path("text").asText("No response generated.");
    }

    /**
     * Detects the category of a farming query for analytics/logging.
     */
    public String detectCategory(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("disease") || lower.contains("infection") || lower.contains("blight") ||
            lower.contains("pest") || lower.contains("insect") || lower.contains("fungus") ||
            lower.contains("yellow") || lower.contains("spots") || lower.contains("rot")) {
            return "CROP_DISEASE";
        } else if (lower.contains("weather") || lower.contains("rain") || lower.contains("temperature") ||
                   lower.contains("drought") || lower.contains("flood") || lower.contains("season") ||
                   lower.contains("monsoon") || lower.contains("irrigation")) {
            return "WEATHER";
        } else if (lower.contains("fertilizer") || lower.contains("urea") || lower.contains("npk") ||
                   lower.contains("pesticide") || lower.contains("spray") || lower.contains("chemical") ||
                   lower.contains("organic") || lower.contains("manure") || lower.contains("compost")) {
            return "FERTILIZER";
        } else if (lower.contains("price") || lower.contains("market") || lower.contains("msp") ||
                   lower.contains("sell") || lower.contains("mandi") || lower.contains("rate")) {
            return "MARKET_PRICE";
        }
        return "GENERAL";
    }
}
