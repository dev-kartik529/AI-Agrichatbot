package com.agrichat.repository;

import com.agrichat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    // Find recently active sessions (last 24 hours)
    @Query("SELECT s FROM ChatSession s WHERE s.lastActive >= :since ORDER BY s.lastActive DESC")
    List<ChatSession> findRecentSessions(LocalDateTime since);

    // Count total sessions today
    @Query("SELECT COUNT(s) FROM ChatSession s WHERE s.createdAt >= :today")
    long countSessionsToday(LocalDateTime today);
}
