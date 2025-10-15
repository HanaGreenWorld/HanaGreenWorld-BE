package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipationResponse {
    private boolean success;
    private String message;
    private ChallengeParticipationData data;
    
    // AI 검증 상세 정보
    private Double confidence;
    private String explanation;
    private String detectedItems; // JSON 문자열로 저장
    private String verifiedAt;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeParticipationData {
        private Long challengeRecordId;
        private String challengeTitle;
        private String verificationStatus;
        private String message;
        private Integer pointsAwarded;
        private Integer teamScoreAwarded;
    }
}