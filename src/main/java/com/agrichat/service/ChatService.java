package com.agrichat.service;

import com.agrichat.dto.ChatRequestDto;
import com.agrichat.dto.ChatResponseDto;
import com.agrichat.entity.ChatMessage;
import com.agrichat.entity.ChatMessage.MessageRole;
import com.agrichat.entity.ChatSession;
import com.agrichat.repository.ChatMessageRepository;
import com.agrichat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final GeminiService geminiService;

    @Value("${agrichat.max-history:10}")
    private int maxHistory;

    /**
     * Main method: receives a user message, calls Gemini, saves everything, returns response.
     */
    @Transactional
    public ChatResponseDto processMessage(ChatRequestDto request) {
        try {
            // 1. Get or create session
            ChatSession session = getOrCreateSession(request.getSessionId(), request.getUserName());

            // 2. Detect category for analytics
            String category = geminiService.detectCategory(request.getMessage());

            // 3. Save the user's message to DB
            ChatMessage userMessage = new ChatMessage();
            userMessage.setSession(session);
            userMessage.setRole(MessageRole.USER);
            userMessage.setContent(request.getMessage());
            userMessage.setCategory(category);
            messageRepository.save(userMessage);

            // 4. Get recent conversation history for Gemini context
            List<ChatMessage> history = getRecentHistory(session.getSessionId());

            // 5. Call Gemini API
            String aiReply = geminiService.generateReply(request.getMessage(), history);

            // 6. Save AI response to DB
            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setSession(session);
            assistantMessage.setRole(MessageRole.ASSISTANT);
            assistantMessage.setContent(aiReply);
            assistantMessage.setCategory(category);
            messageRepository.save(assistantMessage);

            // 7. Update session last-active time
            sessionRepository.save(session);

            log.info("Processed message for session: {} | Category: {}", session.getSessionId(), category);

            return ChatResponseDto.builder()
                    .sessionId(session.getSessionId())
                    .reply(aiReply)
                    .category(category)
                    .timestamp(assistantMessage.getTimestamp())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            return ChatResponseDto.error("An error occurred. Please try again.");
        }
    }

    /**
     * Retrieve full chat history for a session (for display on frontend).
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        if (!sessionRepository.existsBySessionId(sessionId)) {
            return Collections.emptyList();
        }
        return messageRepository.findBySessionSessionIdOrderByTimestampAsc(sessionId);
    }

    /**
     * Get or create a session. Creates a new UUID-based session ID if needed.
     */
    private ChatSession getOrCreateSession(String sessionId, String userName) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createNewSession(sessionId, userName));
        }
        return createNewSession(UUID.randomUUID().toString(), userName);
    }

    private ChatSession createNewSession(String sessionId, String userName) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserName(userName != null ? userName : "Farmer");
        return sessionRepository.save(session);
    }

    /**
     * Gets the last N messages (excluding the one just saved) for Gemini context.
     * We reverse the result since the query returns DESC order.
     */
    private List<ChatMessage> getRecentHistory(String sessionId) {
        // Get last (maxHistory * 2) messages = maxHistory exchanges (user + assistant pairs)
        List<ChatMessage> recent = messageRepository.findLastNMessages(sessionId, maxHistory * 2);
        Collections.reverse(recent);
        // Remove the very last message (the current user message we just saved)
        if (!recent.isEmpty()) {
            recent.remove(recent.size() - 1);
        }
        return recent;
    }
}
