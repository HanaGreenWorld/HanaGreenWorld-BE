package com.kopo.hanagreenworld.member.dto;

import com.kopo.hanagreenworld.member.domain.Team;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.repository.ChallengeRepository;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String slogan;
    private Integer completedChallenges;
    private Integer rank;
    private Integer members;
    private String owner;
    private Boolean isLeader;
    private String createdAt;
    private String inviteCode;
    private String currentChallenge;
    private Long totalSeeds;
    private Double carbonSavedKg;
    private TeamStatsResponse stats;

    @Getter
    @Builder
    public static class EmblemResponse {
        private String id;
        private String name;
        private String description;
        private String iconUrl;
        private Boolean isEarned;
        private LocalDateTime earnedAt;
    }

    @Getter
    @Builder
    public static class TeamStatsResponse {
        private Long monthlyPoints;
        private Long totalPoints;
        private Integer monthlyRank;
        private Integer totalRank;
        private Double carbonSavedKg;
        private Double monthlyCarbonSaved;
        private Integer activeMembers;
        private Integer completedChallengesThisMonth;
    }

    public static TeamResponse from(Team team, TeamStatsResponse stats,
                                   Member leader, Challenge currentChallenge, Integer completedChallenges) {
        TeamResponse result = TeamResponse.builder()
                .id(team.getId())
                .name(team.getTeamName())
                .slogan(team.getDescription())
                .completedChallenges(completedChallenges != null ? completedChallenges : 0)
                .rank(stats.getMonthlyRank())
                .members(stats.getActiveMembers())
                .owner(leader != null ? leader.getName() : "알 수 없음")
                .isLeader(false)
                .createdAt(team.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")))
                .inviteCode(generateInviteCode(team.getId()))
                .currentChallenge(currentChallenge != null ? currentChallenge.getTitle() : "진행 중인 챌린지 없음")
                .totalSeeds(stats.getTotalPoints())
                .carbonSavedKg(stats.getCarbonSavedKg())
                .stats(stats)
                .build();

        return result;
    }
    
    private static String generateInviteCode(Long teamId) {
        return "GG-" + String.format("%04d", teamId).toUpperCase();
    }
}