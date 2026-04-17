package com.agrichat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatResponseDto {

    private String sessionId;
    private String reply;
    private String category;         // e.g., "CROP_DISEASE", "WEATHER", "FERTILIZER", "MARKET_PRICE"
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;     // null on success

    public static ChatResponseDto error(String errorMessage) {
        return ChatResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
