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
    
    // AI 검증 관련 정보
    private Double aiConfidence;
    private String aiExplanation;
    private String aiDetectedItems;

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
        private LocalDateTime startDate;
        private LocalDateTime endDate;
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
                .aiConfidence(record.getAiConfidence())
                .aiExplanation(record.getAiExplanation())
                .aiDetectedItems(record.getAiDetectedItems())
                .build();
    }
    
    /**
     * 사용자 친화적인 상태 메시지 생성
     */
    public String getStatusMessage() {
        switch (verificationStatus) {
            case "PENDING":
                return "📸 사진이 업로드되었습니다. '인증 완료' 버튼을 눌러 AI 검증을 시작하세요.";
            case "VERIFYING":
                return "🤖 AI가 사진을 검증하고 있습니다...";
            case "APPROVED":
                if (aiConfidence != null) {
                    return String.format("✅ 챌린지 인증 성공! (신뢰도: %.1f%%) %d개의 원큐씨앗을 획득했습니다.", 
                            aiConfidence * 100, pointsAwarded != null ? pointsAwarded : 0);
                }
                return "✅ 챌린지 인증이 완료되었습니다!";
            case "NEEDS_REVIEW":
                if (aiConfidence != null) {
                    return String.format("🟡 관리자 검토 필요 (신뢰도: %.1f%%) - 검토 후 포인트가 적립됩니다.", 
                            aiConfidence * 100);
                }
                return "🟡 관리자 검토가 필요합니다.";
            case "REJECTED":
                if (aiConfidence != null) {
                    return String.format("❌ 챌린지 인증 실패 (신뢰도: %.1f%%) - 적절한 인증 사진을 다시 제출해주세요.", 
                            aiConfidence * 100);
                }
                return "❌ 챌린지 인증이 거부되었습니다. 다시 시도해주세요.";
            default:
                return "알 수 없는 상태입니다.";
        }
    }
    
    /**
     * 상태별 버튼 텍스트
     */
    public String getButtonText() {
        switch (verificationStatus) {
            case "PENDING":
                return "인증 완료";
            case "VERIFYING":
                return "검증 중...";
            case "APPROVED":
                return "완료";
            case "NEEDS_REVIEW":
                return "검토 대기";
            case "REJECTED":
                return "다시 시도";
            default:
                return "확인";
        }
    }
    
    /**
     * 버튼 활성화 여부
     */
    public boolean isButtonEnabled() {
        return "PENDING".equals(verificationStatus) || "REJECTED".equals(verificationStatus);
    }
}
