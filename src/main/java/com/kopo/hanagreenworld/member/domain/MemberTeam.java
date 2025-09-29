package com.kopo.hanagreenworld.member.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "member_teams",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "team_id"})
    },
    indexes = {
        @Index(name = "idx_member_team_member", columnList = "member_id"),
        @Index(name = "idx_member_team_team", columnList = "team_id")
    }
)
@Getter
@NoArgsConstructor
public class MemberTeam extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "joined_at")
    private java.time.LocalDateTime joinedAt;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private TeamRole role = TeamRole.MEMBER;

    @Builder
    public MemberTeam(Member member, Team team, TeamRole role) {
        this.member = member;
        this.team = team;
        this.role = role != null ? role : TeamRole.MEMBER;
        this.joinedAt = java.time.LocalDateTime.now();
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void changeRole(TeamRole newRole) {
        this.role = newRole;
    }

    public boolean isLeader() {
        return this.role == TeamRole.LEADER;
    }

    public void promoteToLeader() {
        this.role = TeamRole.LEADER;
    }

    public void demoteToMember() {
        this.role = TeamRole.MEMBER;
    }

    public enum TeamRole {
        LEADER, MEMBER
    }
}

