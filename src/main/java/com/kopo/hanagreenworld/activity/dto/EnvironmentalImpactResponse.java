package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentalImpactResponse {
    
    // 총 탄소 절약량 (kg)
    private BigDecimal totalCarbonSaved;
    
    // 월간 탄소 절약량 (kg)
    private BigDecimal monthlyCarbonSaved;
    
    // 총 절약된 물 (L)
    private BigDecimal totalWaterSaved;
    
    // 월간 절약된 물 (L)
    private BigDecimal monthlyWaterSaved;
    
    // 총 절약된 전력 (kWh)
    private BigDecimal totalEnergySaved;
    
    // 월간 절약된 전력 (kWh)
    private BigDecimal monthlyEnergySaved;
    
    // 총 재활용량 (kg)
    private BigDecimal totalRecycled;
    
    // 월간 재활용량 (kg)
    private BigDecimal monthlyRecycled;
    
    // 환경 등급
    private String environmentalGrade;
    
    // 환경 점수
    private Integer environmentalScore;
    
    // 환경 임팩트 레벨
    private String impactLevel;
    
    // 환경 임팩트 설명
    private String impactDescription;
    
    // 환경 임팩트 아이콘
    private String impactIcon;
    
    // 환경 임팩트 색상
    private String impactColor;
    
    // 카테고리별 환경 임팩트
    private List<Map<String, Object>> categoryImpacts;
    
    // 환경 임팩트 트렌드
    private List<Map<String, Object>> impactTrends;
    
    // 환경 임팩트 랭킹
    private Integer ranking;
    
    // 환경 임팩트 랭킹 설명
    private String rankingDescription;
    
    // 환경 임팩트 성과
    private List<String> achievements;
    
    // 환경 임팩트 목표
    private Map<String, Object> goals;
    
    // 환경 임팩트 추천
    private List<String> recommendations;
    
    // 분석 날짜
    private String analysisDate;
}
