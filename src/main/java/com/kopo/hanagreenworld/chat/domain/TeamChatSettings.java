package com.kopo.hanagreenworld.chat.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Team;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_chat_settings")
@Getter
@NoArgsConstructor
public class TeamChatSettings extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "is_chat_active", nullable = false)
    private Boolean isChatActive = true;

    @Column(name = "max_message_retention_days", nullable = false)
    private Integer maxMessageRetentionDays = 30; // 메시지 보관 기간

    @Column(name = "last_message_id")
    private String lastMessageId; // 마지막 메시지 ID (Redis/MongoDB에서)

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "total_message_count")
    private Long totalMessageCount = 0L;

    @Column(name = "daily_message_limit")
    private Integer dailyMessageLimit = 1000; // 하루 메시지 제한

    @Builder
    public TeamChatSettings(Team team, Boolean isChatActive, 
                           Integer maxMessageRetentionDays, 
                           Integer dailyMessageLimit) {
        this.team = team;
        this.isChatActive = isChatActive;
        this.maxMessageRetentionDays = maxMessageRetentionDays;
        this.dailyMessageLimit = dailyMessageLimit;
    }

    public void updateLastMessage(String messageId, LocalDateTime messageAt) {
        this.lastMessageId = messageId;
        this.lastMessageAt = messageAt;
        this.totalMessageCount++;
    }

    public void deactivateChat() {
        this.isChatActive = false;
    }
}