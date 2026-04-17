package com.agrichat.controller;

import com.agrichat.dto.ChatRequestDto;
import com.agrichat.dto.ChatResponseDto;
import com.agrichat.entity.ChatMessage;
import com.agrichat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow frontend requests (restrict in production)
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat/message
     * Main endpoint: send a farming question, get AI advice.
     *
     * Request body:
     * {
     *   "message": "My wheat crop has yellow leaves, what disease is this?",
     *   "sessionId": "abc-123",   // optional, creates new session if missing
     *   "userName": "Rahul"       // optional
     * }
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponseDto> sendMessage(@Valid @RequestBody ChatRequestDto request) {
        log.info("Received message: {}", request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
        ChatResponseDto response = chatService.processMessage(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.internalServerError().body(response);
    }

    /**
     * GET /api/chat/history/{sessionId}
     * Returns the full conversation history for a session.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = chatService.getSessionHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/chat/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AgriChat Bot",
                "message", "Farming AI is ready to help!"
        ));
    }
}
