package com.kopo.hanagreenworld.member.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamRankingResponse {
    private Integer myTeamRank;
    private Integer totalTeams;
    private List<TopTeamResponse> topTeams;
    private TeamRankingInfo myTeam;

    @Getter
    @Builder
    public static class TopTeamResponse {
        private Long teamId;
        private String teamName;
        private String slogan;
        private Integer rank;
        private Long totalPoints;
        private Integer members;
        private String leaderName;
        private String emblemUrl;
    }

    @Getter
    @Builder
    public static class TeamRankingInfo {
        private Long teamId;
        private String teamName;
        private Integer currentRank;
        private Integer previousRank;
        private Long monthlyPoints;
        private Long totalPoints;
        private Integer members;
        private String trend; // "up", "down", "same"
        private Integer rankChange;
    }

    public static TeamRankingResponse create(List<TopTeamResponse> topTeams, TeamRankingInfo myTeam, Integer totalTeams) {
        return TeamRankingResponse.builder()
                .myTeamRank(myTeam.getCurrentRank())
                .totalTeams(totalTeams)
                .topTeams(topTeams)
                .myTeam(myTeam)
                .build();
    }
}

