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
    private Long challengeRecordId;
    private String challengeTitle;
    private String verificationStatus;
    private String message;
    private Integer pointsAwarded;
    private Integer teamScoreAwarded;
}