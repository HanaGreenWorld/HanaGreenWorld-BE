package com.kopo.hanagreenworld.member.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor
public class Team extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name", length = 100, nullable = false, unique = true)
    private String teamName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_team_points")
    private Long totalTeamPoints = 0L;
    
    @Column(name = "current_team_points")
    private Long currentTeamPoints = 0L;

    // 팀장 ID만 참조 (순환 참조 방지)
    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Team(String teamName, String description, Long leaderId, 
                Integer maxMembers, Boolean isActive) {
        this.teamName = teamName;
        this.description = description;
        this.leaderId = leaderId;
        this.maxMembers = maxMembers;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }

    public void changeLeader(Long newLeaderId) {
        this.leaderId = newLeaderId;
    }

    public boolean isLeader(Long memberId) {
        return this.leaderId != null && this.leaderId.equals(memberId);
    }
}