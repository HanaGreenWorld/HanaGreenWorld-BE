package com.kopo.hanagreenworld.point.service;

import com.kopo.hanagreenworld.integration.service.HanaCardBenefitService;
import com.kopo.hanagreenworld.point.dto.EcoConsumptionAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcoConsumptionService {

    private final HanaCardBenefitService hanaCardBenefitService;

    public EcoConsumptionAnalysisResponse getEcoConsumptionAnalysis(Long userId) {
        log.info("친환경 소비 현황 분석 시작: userId = {}", userId);
        
        // 실제 구현에서는 데이터베이스에서 사용자의 거래 내역을 조회하여 분석
        // 현재는 샘플 데이터 반환
        
        // 전체 소비 금액
        Long totalAmount = 1500000L;
        
        // 친환경 소비 금액
        Long ecoAmount = 450000L;
        
        // 친환경 소비 비율
        double ecoRatio = (double) ecoAmount / totalAmount * 100;
        
        // 카테고리별 분석
        List<Map<String, Object>> categories = new ArrayList<>();
        
        Map<String, Object> mobility = new HashMap<>();
        mobility.put("category", "모빌리티");
        mobility.put("amount", 200000L);
        mobility.put("ratio", 13.3);
        mobility.put("ecoAmount", 180000L);
        mobility.put("ecoRatio", 90.0);
        categories.add(mobility);
        
        Map<String, Object> food = new HashMap<>();
        food.put("category", "식품");
        food.put("amount", 800000L);
        food.put("ratio", 53.3);
        food.put("ecoAmount", 150000L);
        food.put("ecoRatio", 18.8);
        categories.add(food);
        
        Map<String, Object> shopping = new HashMap<>();
        shopping.put("category", "쇼핑");
        shopping.put("amount", 300000L);
        shopping.put("ratio", 20.0);
        shopping.put("ecoAmount", 80000L);
        shopping.put("ecoRatio", 26.7);
        categories.add(shopping);
        
        Map<String, Object> etc = new HashMap<>();
        etc.put("category", "기타");
        etc.put("amount", 200000L);
        etc.put("ratio", 13.3);
        etc.put("ecoAmount", 40000L);
        etc.put("ecoRatio", 20.0);
        categories.add(etc);
        
        // 친환경 등급
        String grade;
        if (ecoRatio >= 70) {
            grade = "친환경 마스터";
        } else if (ecoRatio >= 50) {
            grade = "친환경 전문가";
        } else if (ecoRatio >= 30) {
            grade = "친환경 초보자";
        } else {
            grade = "친환경 도전자";
        }
        
        // 절약된 CO2 (kg)
        double savedCO2 = ecoAmount * 0.0001; // 1원당 0.0001kg CO2 절약
        
        // 친환경 카테고리별 금액
        Map<String, Long> ecoCategoryAmounts = new HashMap<>();
        ecoCategoryAmounts.put("친환경 식품", 150000L);
        ecoCategoryAmounts.put("대중교통", 120000L);
        ecoCategoryAmounts.put("재활용품", 80000L);
        ecoCategoryAmounts.put("친환경 에너지", 113000L);
        
        // 추천 개선사항
        List<String> recommendations = new ArrayList<>();
        recommendations.add("대중교통 이용을 늘려보세요");
        recommendations.add("친환경 식품 구매를 늘려보세요");
        recommendations.add("재활용품 사용을 늘려보세요");
        
        log.info("친환경 소비 현황 분석 완료: userId = {}, ecoRatio = {}%", userId, ecoRatio);
        
        return EcoConsumptionAnalysisResponse.builder()
                .totalAmount(totalAmount)
                .ecoAmount(ecoAmount)
                .ecoRatio(Math.round(ecoRatio * 10) / 10.0)
                .categories(categories)
                .grade(grade)
                .savedCO2(Math.round(savedCO2 * 10) / 10.0)
                .ecoCategoryAmounts(ecoCategoryAmounts)
                .analysisDate(java.time.LocalDate.now().toString())
                .recommendations(recommendations)
                .expectedSavings(50000L)
                .expectedPoints(1000L)
                .build();
    }

    public Map<String, Object> getEcoMerchantBenefits(Long userId) {
        log.info("친환경 가맹점 혜택 조회 시작: userId = {}", userId);
        
        // 실제 구현에서는 데이터베이스에서 사용자의 가맹점 방문 내역을 조회
        // 현재는 샘플 데이터 반환
        
        Map<String, Object> benefits = new HashMap<>();
        
        // 총 혜택 금액
        Long totalBenefits = 45000L;
        benefits.put("totalBenefits", totalBenefits);
        
        // 월별 혜택 내역
        List<Map<String, Object>> monthlyBenefits = new ArrayList<>();
        
        Map<String, Object> currentMonth = new HashMap<>();
        currentMonth.put("month", "2024-01");
        currentMonth.put("amount", 15000L);
        currentMonth.put("count", 8);
        monthlyBenefits.add(currentMonth);
        
        Map<String, Object> lastMonth = new HashMap<>();
        lastMonth.put("month", "2023-12");
        lastMonth.put("amount", 18000L);
        lastMonth.put("count", 12);
        monthlyBenefits.add(lastMonth);
        
        Map<String, Object> twoMonthsAgo = new HashMap<>();
        twoMonthsAgo.put("month", "2023-11");
        twoMonthsAgo.put("amount", 12000L);
        twoMonthsAgo.put("count", 6);
        monthlyBenefits.add(twoMonthsAgo);
        
        benefits.put("monthlyBenefits", monthlyBenefits);
        
        // 카테고리별 혜택
        List<Map<String, Object>> categoryBenefits = new ArrayList<>();
        
        Map<String, Object> mobilityBenefit = new HashMap<>();
        mobilityBenefit.put("category", "모빌리티");
        mobilityBenefit.put("amount", 20000L);
        mobilityBenefit.put("count", 15);
        categoryBenefits.add(mobilityBenefit);
        
        Map<String, Object> foodBenefit = new HashMap<>();
        foodBenefit.put("category", "식품");
        foodBenefit.put("amount", 15000L);
        foodBenefit.put("count", 8);
        categoryBenefits.add(foodBenefit);
        
        Map<String, Object> shoppingBenefit = new HashMap<>();
        shoppingBenefit.put("category", "쇼핑");
        shoppingBenefit.put("amount", 10000L);
        shoppingBenefit.put("count", 3);
        categoryBenefits.add(shoppingBenefit);
        
        benefits.put("categoryBenefits", categoryBenefits);
        
        log.info("친환경 가맹점 혜택 조회 완료: userId = {}, totalBenefits = {}", userId, totalBenefits);
        
        return benefits;
    }

    public Map<String, Object> getCardBenefitPackages(Long userId) {
        log.info("카드 혜택 패키지 조회 시작: userId = {}", userId);
        
        // 하나카드 API 호출
        return hanaCardBenefitService.getBenefitPackages(userId);
    }

    public Map<String, Object> changeBenefitPackage(Long userId, Long cardProductId, String packageCode, String changeReason) {
        log.info("혜택 패키지 변경 시작: userId = {}, cardProductId = {}, packageCode = {}", userId, cardProductId, packageCode);
        
        // 하나카드 API 호출
        return hanaCardBenefitService.changeBenefitPackage(userId, cardProductId, packageCode, changeReason);
    }
}