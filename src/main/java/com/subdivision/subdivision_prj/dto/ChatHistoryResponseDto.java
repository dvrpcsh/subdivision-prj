package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.ChatMessage;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ChatHistoryResponseDto {
    private final String sender;
    private final String message;
    private final LocalDateTime sendAt;

    public ChatHistoryResponseDto(ChatMessage chatMessage) {
        this.sender = chatMessage.getSender().getNickname();
        this.message = chatMessage.getMessage();
        this.sendAt = chatMessage.getSendAt();
    }
}
