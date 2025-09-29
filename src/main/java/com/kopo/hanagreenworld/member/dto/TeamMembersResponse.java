package com.kopo.hanagreenworld.member.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TeamMembersResponse {
    private Long teamId;
    private List<TeamMemberResponse> members;
    private Integer totalCount;

    @Getter
    @Builder
    public static class TeamMemberResponse {
        private Long id;
        private String name;
        private String email;
        private String role; // LEADER, MEMBER
        private Long totalPoints;
        private Long monthlyPoints;
        private LocalDateTime joinedAt;
        private String profileImageUrl;
        private Boolean isOnline;
    }
}
