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
    
    // 탄소절감 평균 대비
    private double monthlyCarbonComparison;    // 이번달 탄소절감 상위 퍼센트
    private double totalCarbonComparison;      // 전체 탄소절감 상위 퍼센트
    
    // 원큐씨앗 평균 대비
    private double monthlyPointsComparison;    // 이번달 원큐씨앗 상위 퍼센트
    private double totalPointsComparison;      // 전체 원큐씨앗 상위 퍼센트
    
    // 증감률
    private Double monthlyGrowthRate;          // 월간 탄소절약량 증감률 (%) - null이면 지난달 데이터 없음
    private Double ecoSeedsGrowthRate;         // 월간 원큐씨앗 증감률 (%) - null이면 지난달 데이터 없음
    
    // 기존 호환성을 위한 필드 (deprecated)
    @Deprecated
    private double averageComparison;          // 평균 대비 비율 (%)
    
    private String comparisonDescription;     // 비교 설명
    private int userRanking;                  // 사용자 순위
    private int totalUsers;                    // 전체 사용자 수
}
