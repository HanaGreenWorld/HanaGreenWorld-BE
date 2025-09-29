package com.kopo.hanagreenworld.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresenceEvent {
    private Long teamId;
    private Long userId;
    private String userName;
    private String action; // "join", "leave"
    private Long timestamp;

    public static PresenceEvent join(Long teamId, Long userId, String userName) {
        return PresenceEvent.builder()
                .teamId(teamId)
                .userId(userId)
                .userName(userName)
                .action("join")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static PresenceEvent leave(Long teamId, Long userId, String userName) {
        return PresenceEvent.builder()
                .teamId(teamId)
                .userId(userId)
                .userName(userName)
                .action("leave")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

