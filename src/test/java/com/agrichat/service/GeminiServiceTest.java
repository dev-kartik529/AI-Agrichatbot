package com.agrichat.service;

import com.agrichat.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiService, "apiUrl", "https://test-gemini-url");
        ReflectionTestUtils.setField(geminiService, "maxTokens", 1024);
        ReflectionTestUtils.setField(geminiService, "temperature", 0.7);
    }

    @Test
    void testGenerateReply_Success() {
        String mockResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{"text": "Yellow leaves on wheat can indicate rust disease. Apply fungicide."}],
                  "role": "model"
                }
              }]
            }
            """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        String reply = geminiService.generateReply("My wheat has yellow leaves", Collections.emptyList());

        assertNotNull(reply);
        assertTrue(reply.contains("yellow") || reply.contains("rust") || reply.contains("fungicide"));
    }

    @Test
    void testGenerateReply_ApiError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        String reply = geminiService.generateReply("test message", Collections.emptyList());

        assertNotNull(reply);
        assertTrue(reply.contains("trouble") || reply.contains("try again") || reply.contains("KVK"));
    }

    @Test
    void testDetectCategory_CropDisease() {
        assertEquals("CROP_DISEASE", geminiService.detectCategory("My wheat has yellow spots and blight"));
        assertEquals("CROP_DISEASE", geminiService.detectCategory("pest attack on my cotton crop"));
    }

    @Test
    void testDetectCategory_Weather() {
        assertEquals("WEATHER", geminiService.detectCategory("What crops to grow in monsoon season?"));
        assertEquals("WEATHER", geminiService.detectCategory("irrigation schedule for drought conditions"));
    }

    @Test
    void testDetectCategory_Fertilizer() {
        assertEquals("FERTILIZER", geminiService.detectCategory("How much urea for wheat per acre?"));
        assertEquals("FERTILIZER", geminiService.detectCategory("organic manure for tomatoes"));
    }

    @Test
    void testDetectCategory_MarketPrice() {
        assertEquals("MARKET_PRICE", geminiService.detectCategory("what is current MSP for rice?"));
        assertEquals("MARKET_PRICE", geminiService.detectCategory("best mandi to sell my crop"));
    }

    @Test
    void testDetectCategory_General() {
        assertEquals("GENERAL", geminiService.detectCategory("Tell me about farming"));
    }
}
