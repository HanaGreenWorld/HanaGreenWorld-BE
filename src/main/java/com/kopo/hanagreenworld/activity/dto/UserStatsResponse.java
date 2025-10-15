package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private String registrationDate;           // 가입일
    private int practiceDays;                  // 실천일 (가입일 기준)
    private double averageComparison;          // 평균 대비 비율 (%)
    private double monthlyGrowthRate;         // 월간 탄소절약량 증감률 (%)
    private double ecoSeedsGrowthRate;        // 월간 원큐씨앗 증감률 (%)
    private String comparisonDescription;     // 비교 설명
    private int userRanking;                  // 사용자 순위
    private int totalUsers;                    // 전체 사용자 수
}
