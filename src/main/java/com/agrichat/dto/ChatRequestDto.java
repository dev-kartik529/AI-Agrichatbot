package com.agrichat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// -----------------------------------------------
// Request DTO from frontend to backend
// -----------------------------------------------
@Data
public class ChatRequestDto {

    @NotBlank(message = "Message cannot be empty")
    @Size(min = 2, max = 2000, message = "Message must be between 2 and 2000 characters")
    private String message;

    // Session ID to maintain conversation history. If null, backend creates a new session.
    private String sessionId;

    private String userName; // optional display name
}
