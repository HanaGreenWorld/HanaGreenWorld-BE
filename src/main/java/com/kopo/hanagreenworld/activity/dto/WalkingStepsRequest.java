package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkingStepsRequest {
    private Integer steps;
    private String date; // YYYY-MM-DD 형식 (선택사항, 없으면 오늘 날짜)
}












