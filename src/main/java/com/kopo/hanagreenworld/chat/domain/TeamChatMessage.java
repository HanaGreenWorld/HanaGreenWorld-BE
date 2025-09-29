package com.kopo.hanagreenworld.chat.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.Team;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "team_chat_messages",
    indexes = {
        @Index(name = "idx_chat_team_created", columnList = "team_id, created_at"),
        @Index(name = "idx_chat_sender", columnList = "sender_id")
    }
)
@Getter
@NoArgsConstructor
public class TeamChatMessage extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    private String messageText;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "redis_message_id")
    private String redisMessageId; // Redis에서의 메시지 ID

    @Builder
    public TeamChatMessage(Team team, Member sender, String messageText, MessageType messageType) {
        this.team = team;
        this.sender = sender;
        this.messageText = messageText;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void setRedisMessageId(String redisMessageId) {
        this.redisMessageId = redisMessageId;
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}

