package com.kopo.hanagreenworld.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoConsumptionAnalysisResponse {
    
    // 전체 소비 금액
    private Long totalAmount;
    
    // 친환경 소비 금액
    private Long ecoAmount;
    
    // 친환경 소비 비율
    private Double ecoRatio;
    
    // 카테고리별 분석
    private List<Map<String, Object>> categories;
    
    // 친환경 등급
    private String grade;
    
    // 절약된 CO2 (kg)
    private Double savedCO2;
    
    // 친환경 카테고리별 금액
    private Map<String, Long> ecoCategoryAmounts;
    
    // 분석 날짜
    private String analysisDate;
    
    // 월간 트렌드
    private Map<String, Object> monthlyTrend;
    
    // 추천 개선사항
    private List<String> recommendations;
    
    // 예상 절약 금액
    private Long expectedSavings;
    
    // 예상 추가 포인트
    private Long expectedPoints;
}

