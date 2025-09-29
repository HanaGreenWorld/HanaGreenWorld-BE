package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkingResponse {
    private Long walkingId;
    private Integer steps;
    private BigDecimal distanceKm;
    private BigDecimal carbonSaved;
    private Integer pointsAwarded;
    private LocalDateTime activityDate;
    private String message;
}












