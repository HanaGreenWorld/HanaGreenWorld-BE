package com.kopo.hanagreenworld.point.service;

import com.kopo.hanagreenworld.merchant.service.EcoMerchantMatchingService;
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

    private final EcoMerchantMatchingService ecoMerchantMatchingService;

    public EcoConsumptionAnalysisResponse getEcoConsumptionAnalysis(Long userId) {

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

        try {
            List<Map<String, Object>> ecoMerchantHistory = ecoMerchantMatchingService.getUserEcoMerchantHistory(userId);

            Map<String, Object> stats = ecoMerchantMatchingService.getEcoMerchantStats(userId);

            List<Map<String, Object>> benefitsList = new ArrayList<>();
            
            for (Map<String, Object> history : ecoMerchantHistory) {
                Map<String, Object> benefit = new HashMap<>();
                benefit.put("storeName", history.get("merchantName"));
                benefit.put("benefitName", getBenefitNameByCategory((String) history.get("category")));
                benefit.put("amount", String.format("%,d원", (Long) history.get("amount"))); // 실제 거래 금액 사용
                benefit.put("date", formatDate((String) history.get("transactionDate")));
                benefit.put("icon", getIconByCategory((String) history.get("category")));
                benefit.put("businessNumber", history.get("businessNumber"));
                benefit.put("originalAmount", history.get("amount"));
                benefit.put("additionalSeeds", history.get("additionalSeeds"));
                benefitsList.add(benefit);
            }
            
            Map<String, Object> benefits = new HashMap<>();
            benefits.put("totalBenefits", stats.get("totalAdditionalSeeds"));
            benefits.put("benefits", benefitsList);
            benefits.put("stats", stats);

            return benefits;
            
        } catch (Exception e) {
            log.error("친환경 가맹점 혜택 조회 실패: userId = {}, 에러: {}", userId, e.getMessage(), e);
            
            // 에러 시 기본 데이터 반환
            Map<String, Object> benefits = new HashMap<>();
            benefits.put("totalBenefits", 0L);
            benefits.put("benefits", new ArrayList<>());
            benefits.put("error", e.getMessage());
            return benefits;
        }
    }

    private String getBenefitNameByCategory(String category) {
        return switch (category) {
            case "친환경 식품/매장" -> "유기농 식품 구매";
            case "전기차 충전" -> "전기차 충전";
            case "재활용/제로웨이스트" -> "친환경 제품 구매";
            case "친환경 뷰티" -> "천연 화장품 구매";
            case "친환경 쇼핑" -> "친환경 의류 구매";
            case "유기농 카페" -> "유기농 음료 주문";
            default -> "친환경 가맹점 이용";
        };
    }

    private String getIconByCategory(String category) {
        return switch (category) {
            case "친환경 식품/매장" -> "eco-store.png";
            case "전기차 충전" -> "ev-charging.png";
            case "재활용/제로웨이스트" -> "zero-waste.png";
            case "친환경 뷰티" -> "green-beauty.png";
            case "친환경 쇼핑" -> "eco-shopping.png";
            case "유기농 카페" -> "organic-cafe.png";
            default -> "eco-default.png";
        };
    }

    private String formatDate(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            if (parts.length >= 3) {
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return String.format("%d월 %d일", month, day);
            }
        } catch (Exception e) {
            log.warn("날짜 포맷팅 실패: {}", dateStr);
        }
        return dateStr;
    }
}