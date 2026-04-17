package com.agrichat.controller;

import com.agrichat.dto.ChatRequestDto;
import com.agrichat.dto.ChatResponseDto;
import com.agrichat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void testSendMessage_Success() throws Exception {
        ChatResponseDto mockResponse = ChatResponseDto.builder()
                .sessionId("test-session-123")
                .reply("Wheat rust is a fungal disease. Apply propiconazole fungicide.")
                .category("CROP_DISEASE")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();

        when(chatService.processMessage(any())).thenReturn(mockResponse);

        ChatRequestDto request = new ChatRequestDto();
        request.setMessage("My wheat has rust disease, what to do?");

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").value("test-session-123"))
                .andExpect(jsonPath("$.category").value("CROP_DISEASE"));
    }

    @Test
    void testSendMessage_EmptyMessage_ShouldFail() throws Exception {
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage("");  // empty - should fail @NotBlank validation

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
