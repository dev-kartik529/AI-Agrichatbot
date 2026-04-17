package com.agrichat.repository;

import com.agrichat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionSessionIdOrderByTimestampAsc(String sessionId);

    // Get the last N messages for a session (for Gemini context window)
    @Query(value = "SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findLastNMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    // Count messages by category for analytics
    @Query("SELECT m.category, COUNT(m) FROM ChatMessage m GROUP BY m.category")
    List<Object[]> countByCategory();

    // Total messages in a session
    long countBySessionSessionId(String sessionId);
}
