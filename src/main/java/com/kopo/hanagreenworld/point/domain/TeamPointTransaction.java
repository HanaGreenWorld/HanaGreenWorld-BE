package com.kopo.hanagreenworld.point.domain;

import java.time.LocalDateTime;

import com.kopo.hanagreenworld.member.domain.Team;
import jakarta.persistence.*;

@Entity
@Table(name = "team_point_transactions")
public class TeamPointTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Enumerated(EnumType.STRING)
    private PointTransactionType pointTransactionType;
    
    @Enumerated(EnumType.STRING)
    private TeamPointCategory category;

    public enum TeamPointCategory {
        TEAM_CHALLENGE("팀 챌린지", "/static/assets/green_team.png"),
        TEAM_WALKING("팀 걷기", "/static/assets/hana3dIcon/hanaIcon3d_123.png"),
        TEAM_ECO_ACTIVITY("팀 환경활동", "/static/assets/sprout.png");

        private final String displayName;
        private final String imageUrl;

        TeamPointCategory(String displayName, String imageUrl) {
            this.displayName = displayName;
            this.imageUrl = imageUrl;
        }
    }
    
    private String description;
    private Integer pointsAmount;
    private Long teamBalanceAfter;
    private LocalDateTime occurredAt;
}