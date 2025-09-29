package com.kopo.hanagreenworld.activity.dto;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRecordResponse {
    private Long id;
    private ChallengeInfo challenge;
    private MemberInfo member;
    private Long teamId;
    private LocalDateTime activityDate;
    private String imageUrl;
    private Long stepCount;
    private String verificationStatus;
    private LocalDateTime verifiedAt;
    private Integer pointsAwarded;
    private Integer teamScoreAwarded;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeInfo {
        private Long id;
        private String code;
        private String title;
        private String description;
        private String rewardPolicy;
        private Integer points;
        private Integer teamScore;
        private Boolean isTeamChallenge;
        private Boolean isActive;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long memberId;
    }

    public static ChallengeRecordResponse from(ChallengeRecord record) {
        return ChallengeRecordResponse.builder()
                .id(record.getId())
                .challenge(ChallengeInfo.builder()
                        .id(record.getChallenge().getId())
                        .code(record.getChallenge().getCode().name())
                        .title(record.getChallenge().getTitle())
                        .description(record.getChallenge().getDescription())
                        .rewardPolicy(record.getChallenge().getRewardPolicy().name())
                        .points(record.getChallenge().getPoints())
                        .teamScore(record.getChallenge().getTeamScore())
                        .isTeamChallenge(record.getChallenge().getIsTeamChallenge())
                        .isActive(record.getChallenge().getIsActive())
                        .build())
                .member(MemberInfo.builder()
                        .memberId(record.getMember().getMemberId())
                        .build())
                .teamId(record.getTeamId())
                .activityDate(record.getActivityDate())
                .imageUrl(record.getImageUrl())
                .stepCount(record.getStepCount())
                .verificationStatus(record.getVerificationStatus())
                .verifiedAt(record.getVerifiedAt())
                .pointsAwarded(record.getPointsAwarded())
                .teamScoreAwarded(record.getTeamScoreAwarded())
                .build();
    }
}
