package com.kopo.hanagreenworld.chat.dto;

import com.kopo.hanagreenworld.chat.domain.TeamChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private String messageId;
    private Long teamId;
    private Long senderId;
    private String senderName;
    private String messageText;
    private String messageType;
    private LocalDateTime createdAt;
    private Boolean isDeleted;

    public static ChatMessageResponse from(TeamChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId().toString())
                .teamId(message.getTeam().getId())
                .senderId(message.getSender().getMemberId())
                .senderName(message.getSender().getName())
                .messageText(message.getMessageText())
                .messageType(message.getMessageType().name())
                .createdAt(message.getCreatedAt())
                .isDeleted(message.getIsDeleted())
                .build();
    }

    public static ChatMessageResponse create(String messageId, Long teamId, Long senderId, 
                                           String senderName, String messageText, String messageType) {
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .teamId(teamId)
                .senderId(senderId)
                .senderName(senderName)
                .messageText(messageText)
                .messageType(messageType)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }
}
