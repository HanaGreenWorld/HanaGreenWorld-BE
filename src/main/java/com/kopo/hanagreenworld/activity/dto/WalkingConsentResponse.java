package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkingConsentResponse {
    private Boolean isConsented;
    private LocalDateTime consentedAt;
    private LocalDateTime lastSyncAt;
    private Integer dailyGoalSteps;
    private String message;
}












