package com.kopo.hanagreenworld.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    
    @NotNull(message = "팀 ID는 필수입니다.")
    private Long teamId;
    
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다.")
    private String messageText;
    
    private String messageType = "TEXT";
    
    public ChatMessageRequest(Long teamId, String messageText, String messageType) {
        this.teamId = teamId;
        this.messageText = messageText;
        this.messageType = messageType;
    }
}

