package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipationRequest {
    private String imageUrl; // 사진 인증용 이미지 URL
    private Long stepCount; // 걸음수 인증용 (주간 걸음수 챌린지)
    private Long teamId; // 팀 챌린지일 때 팀 ID
}