package com.kopo.hanagreenworld.ai.service;

import com.kopo.hanagreenworld.point.dto.EcoConsumptionAnalysisResponse;
import com.kopo.hanagreenworld.integration.service.HanaCardBenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * AI 기반 개인화 혜택 추천 서비스
 * 사용자의 소비 패턴을 분석하여 최적의 카드 혜택을 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitRecommendationService {

    private final RestTemplate restTemplate;
    private final HanaCardBenefitService hanaCardBenefitService;

    @Value("${ai.server.url}")
    private String aiServerBaseUrl;

    /**
     * 사용자 소비 패턴 기반 혜택 추천
     */
    public Map<String, Object> recommendBenefitPackage(Long userId, EcoConsumptionAnalysisResponse consumptionAnalysis) {
        log.info("🔍 사용자 {}의 혜택 추천 시작", userId);
        
        try {
            // 1. 사용자 소비 패턴 분석
            Map<String, Object> consumptionPattern = analyzeConsumptionPattern(consumptionAnalysis);
            
            // 2. AI 서버에 추천 요청
            Map<String, Object> aiRecommendation = getAIRecommendation(consumptionPattern);
            
            // 3. 추천 결과와 현재 혜택 비교
            Map<String, Object> recommendation = createRecommendationResponse(userId, aiRecommendation, consumptionAnalysis);
            
            log.info("✅ 사용자 {}의 혜택 추천 완료", userId);
            return recommendation;
            
        } catch (Exception e) {
            log.error("❌ 혜택 추천 실패: {}", e.getMessage(), e);
            return createFallbackRecommendation(consumptionAnalysis);
        }
    }

    /**
     * 소비 패턴 분석
     */
    private Map<String, Object> analyzeConsumptionPattern(EcoConsumptionAnalysisResponse analysis) {
        Map<String, Object> pattern = new HashMap<>();
        
        // 전체 소비 금액
        BigDecimal totalAmount = BigDecimal.valueOf(analysis.getTotalAmount());
        BigDecimal ecoAmount = BigDecimal.valueOf(analysis.getEcoAmount());
        
        // 친환경 소비 비율
        BigDecimal ecoRatio = totalAmount.compareTo(BigDecimal.ZERO) > 0 
            ? ecoAmount.divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        
        // 카테고리별 소비 분석 (categories에서 추출)
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        for (Map<String, Object> category : analysis.getCategories()) {
            String categoryName = (String) category.get("category");
            Long amount = (Long) category.get("amount");
            categoryAmounts.put(categoryName, BigDecimal.valueOf(amount));
        }
        
        // 친환경 카테고리별 금액을 BigDecimal로 변환
        Map<String, BigDecimal> ecoCategoryAmounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : analysis.getEcoCategoryAmounts().entrySet()) {
            ecoCategoryAmounts.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
        }
        
        // 주요 소비 카테고리 식별
        String topCategory = findTopCategory(categoryAmounts);
        String topEcoCategory = findTopCategory(ecoCategoryAmounts);
        
        // 소비 패턴 분류
        String consumptionType = classifyConsumptionType(ecoRatio, topCategory, topEcoCategory);
        
        pattern.put("totalAmount", totalAmount);
        pattern.put("ecoAmount", ecoAmount);
        pattern.put("ecoRatio", ecoRatio);
        pattern.put("topCategory", topCategory);
        pattern.put("topEcoCategory", topEcoCategory);
        pattern.put("consumptionType", consumptionType);
        pattern.put("categoryAmounts", categoryAmounts);
        pattern.put("ecoCategoryAmounts", ecoCategoryAmounts);
        
        log.info("📊 소비 패턴 분석 완료: 타입={}, 친환경비율={}%, 주요카테고리={}", 
                consumptionType, ecoRatio, topCategory);
        
        return pattern;
    }

    /**
     * AI 서버에 추천 요청 (일시적으로 비활성화)
     */
    private Map<String, Object> getAIRecommendation(Map<String, Object> consumptionPattern) {
        log.info("🚫 AI 서버 호출이 비활성화되었습니다. 로직 기반 추천을 사용합니다.");
        return createLogicBasedRecommendation(consumptionPattern);
    }

    /**
     * 로직 기반 추천 (AI 서버 실패 시 폴백)
     */
    private Map<String, Object> createLogicBasedRecommendation(Map<String, Object> consumptionPattern) {
        String consumptionType = (String) consumptionPattern.get("consumptionType");
        BigDecimal ecoRatio = (BigDecimal) consumptionPattern.get("ecoRatio");
        String topCategory = (String) consumptionPattern.get("topCategory");
        
        Map<String, Object> recommendation = new HashMap<>();
        
        // 소비 패턴에 따른 추천 로직
        if ("ECO_FOCUSED".equals(consumptionType)) {
            recommendation.put("recommendedPackage", "ALL_GREEN_LIFE");
            recommendation.put("confidence", 0.9);
            recommendation.put("reason", "친환경 소비 비율이 높아 종합 혜택 패키지를 추천합니다.");
        } else if ("MOBILITY_FOCUSED".equals(consumptionType)) {
            recommendation.put("recommendedPackage", "GREEN_MOBILITY");
            recommendation.put("confidence", 0.85);
            recommendation.put("reason", "교통 관련 소비가 많아 모빌리티 혜택 패키지를 추천합니다.");
        } else if ("ZERO_WASTE_FOCUSED".equals(consumptionType)) {
            recommendation.put("recommendedPackage", "ZERO_WASTE_LIFE");
            recommendation.put("confidence", 0.8);
            recommendation.put("reason", "제로웨이스트 관련 소비가 많아 제로웨이스트 혜택 패키지를 추천합니다.");
        } else {
            recommendation.put("recommendedPackage", "ALL_GREEN_LIFE");
            recommendation.put("confidence", 0.7);
            recommendation.put("reason", "균형잡힌 소비 패턴으로 종합 혜택 패키지를 추천합니다.");
        }
        
        return recommendation;
    }

    /**
     * 추천 결과 응답 생성
     */
    private Map<String, Object> createRecommendationResponse(Long userId, Map<String, Object> aiRecommendation, 
                                                           EcoConsumptionAnalysisResponse consumptionAnalysis) {
        Map<String, Object> response = new HashMap<>();
        
        // 현재 혜택 패키지 조회
        Map<String, Object> currentPackage = hanaCardBenefitService.getCurrentBenefitPackage(userId, 1L);
        
        // 추천 패키지 정보
        String recommendedPackageCode = (String) aiRecommendation.get("recommendedPackage");
        String currentPackageName = (String) currentPackage.get("packageName");
        
        // 추천 패키지가 현재 패키지와 다른지 확인
        boolean shouldChange = !isCurrentPackage(recommendedPackageCode, currentPackageName);
        
        // 예상 추가 혜택 계산
        Map<String, Object> expectedBenefits = calculateExpectedBenefits(
                recommendedPackageCode, consumptionAnalysis);
        
        response.put("recommendedPackage", recommendedPackageCode);
        response.put("currentPackage", currentPackageName);
        response.put("shouldChange", shouldChange);
        response.put("confidence", aiRecommendation.get("confidence"));
        response.put("reason", aiRecommendation.get("reason"));
        response.put("expectedBenefits", expectedBenefits);
        response.put("consumptionAnalysis", createConsumptionSummary(consumptionAnalysis));
        
        return response;
    }

    /**
     * 예상 혜택 계산
     */
    private Map<String, Object> calculateExpectedBenefits(String packageCode, 
                                                         EcoConsumptionAnalysisResponse analysis) {
        Map<String, Object> benefits = new HashMap<>();
        
        // 패키지별 캐시백 비율
        Map<String, BigDecimal> cashbackRates = getCashbackRates(packageCode);
        
        BigDecimal totalExpectedCashback = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryBenefits = new HashMap<>();
        
        // 카테고리별 예상 혜택 계산
        Map<String, BigDecimal> ecoCategoryAmounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : analysis.getEcoCategoryAmounts().entrySet()) {
            ecoCategoryAmounts.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
        }
        
        for (Map.Entry<String, BigDecimal> entry : ecoCategoryAmounts.entrySet()) {
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal rate = cashbackRates.getOrDefault(category, BigDecimal.ZERO);
            
            BigDecimal expectedCashback = amount.multiply(rate).divide(new BigDecimal("100"));
            categoryBenefits.put(category, expectedCashback);
            totalExpectedCashback = totalExpectedCashback.add(expectedCashback);
        }
        
        benefits.put("totalExpectedCashback", totalExpectedCashback);
        benefits.put("categoryBenefits", categoryBenefits);
        benefits.put("cashbackRates", cashbackRates);
        
        return benefits;
    }

    /**
     * 소비 요약 생성
     */
    private Map<String, Object> createConsumptionSummary(EcoConsumptionAnalysisResponse analysis) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalAmount", analysis.getTotalAmount());
        summary.put("ecoAmount", analysis.getEcoAmount());
        summary.put("ecoRatio", analysis.getEcoRatio());
        // 친환경 카테고리별 금액을 BigDecimal로 변환
        Map<String, BigDecimal> ecoCategoryAmounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : analysis.getEcoCategoryAmounts().entrySet()) {
            ecoCategoryAmounts.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
        }
        summary.put("topCategories", getTopCategories(ecoCategoryAmounts, 3));
        
        return summary;
    }

    /**
     * 폴백 추천 (에러 시)
     */
    private Map<String, Object> createFallbackRecommendation(EcoConsumptionAnalysisResponse analysis) {
        Map<String, Object> fallback = new HashMap<>();
        
        fallback.put("recommendedPackage", "ALL_GREEN_LIFE");
        fallback.put("currentPackage", "올인원 그린라이프 캐시백");
        fallback.put("shouldChange", false);
        fallback.put("confidence", 0.5);
        fallback.put("reason", "현재 패키지가 균형잡힌 혜택을 제공합니다.");
        fallback.put("expectedBenefits", new HashMap<>());
        fallback.put("consumptionAnalysis", createConsumptionSummary(analysis));
        
        return fallback;
    }

    // 유틸리티 메서드들
    private String findTopCategory(Map<String, BigDecimal> categoryAmounts) {
        return categoryAmounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("기타");
    }

    private String classifyConsumptionType(BigDecimal ecoRatio, String topCategory, String topEcoCategory) {
        if (ecoRatio.compareTo(new BigDecimal("70")) >= 0) {
            return "ECO_FOCUSED";
        } else if (topCategory.contains("교통") || topCategory.contains("모빌리티")) {
            return "MOBILITY_FOCUSED";
        } else if (topCategory.contains("리필") || topCategory.contains("제로웨이스트")) {
            return "ZERO_WASTE_FOCUSED";
        } else {
            return "BALANCED";
        }
    }

    private boolean isCurrentPackage(String recommendedCode, String currentPackageName) {
        if (currentPackageName == null) return false;
        
        return (recommendedCode.equals("ALL_GREEN_LIFE") && currentPackageName.contains("올인원")) ||
               (recommendedCode.equals("GREEN_MOBILITY") && currentPackageName.contains("모빌리티")) ||
               (recommendedCode.equals("ZERO_WASTE_LIFE") && currentPackageName.contains("제로웨이스트"));
    }

    private Map<String, BigDecimal> getCashbackRates(String packageCode) {
        Map<String, BigDecimal> rates = new HashMap<>();
        
        switch (packageCode) {
            case "ALL_GREEN_LIFE":
                rates.put("전기차충전", new BigDecimal("3.0"));
                rates.put("대중교통", new BigDecimal("2.0"));
                rates.put("공유모빌리티", new BigDecimal("4.0"));
                rates.put("리필스테이션", new BigDecimal("4.0"));
                rates.put("친환경브랜드", new BigDecimal("2.0"));
                break;
            case "GREEN_MOBILITY":
                rates.put("전기차충전", new BigDecimal("7.0"));
                rates.put("대중교통", new BigDecimal("5.0"));
                rates.put("공유모빌리티", new BigDecimal("10.0"));
                rates.put("친환경렌터카", new BigDecimal("3.0"));
                break;
            case "ZERO_WASTE_LIFE":
                rates.put("리필스테이션", new BigDecimal("10.0"));
                rates.put("친환경브랜드", new BigDecimal("5.0"));
                rates.put("중고거래", new BigDecimal("3.0"));
                rates.put("비건유기농", new BigDecimal("7.0"));
                break;
        }
        
        return rates;
    }

    private List<Map<String, Object>> getTopCategories(Map<String, BigDecimal> categoryAmounts, int limit) {
        return categoryAmounts.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> category = new HashMap<>();
                    category.put("name", entry.getKey());
                    category.put("amount", entry.getValue());
                    return category;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private List<Map<String, Object>> getAvailableBenefitPackages() {
        List<Map<String, Object>> packages = new ArrayList<>();
        
        Map<String, Object> allGreenLife = new HashMap<>();
        allGreenLife.put("code", "ALL_GREEN_LIFE");
        allGreenLife.put("name", "올인원 그린라이프 캐시백");
        allGreenLife.put("description", "친환경 생활 종합 혜택");
        packages.add(allGreenLife);
        
        Map<String, Object> greenMobility = new HashMap<>();
        greenMobility.put("code", "GREEN_MOBILITY");
        greenMobility.put("name", "그린 모빌리티 캐시백");
        greenMobility.put("description", "친환경 교통수단 특화 혜택");
        packages.add(greenMobility);
        
        Map<String, Object> zeroWaste = new HashMap<>();
        zeroWaste.put("code", "ZERO_WASTE_LIFE");
        zeroWaste.put("name", "제로웨이스트 라이프 캐시백");
        zeroWaste.put("description", "제로웨이스트 라이프스타일 혜택");
        packages.add(zeroWaste);
        
        return packages;
    }

    /**
     * 혜택 추천 상세 분석
     */
    public Map<String, Object> getRecommendationAnalysis(Long userId, EcoConsumptionAnalysisResponse consumptionAnalysis, String packageCode) {
        log.info("📊 사용자 {}의 혜택 추천 상세 분석 시작", userId);
        
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // 소비 패턴 분석
            Map<String, Object> consumptionPattern = analyzeConsumptionPattern(consumptionAnalysis);
            
            // 특정 패키지 분석 (지정된 경우)
            if (packageCode != null && !packageCode.isEmpty()) {
                Map<String, Object> packageAnalysis = analyzeSpecificPackage(packageCode, consumptionPattern);
                analysis.put("packageAnalysis", packageAnalysis);
            }
            
            // 전체 패키지 비교
            Map<String, Object> allPackagesComparison = compareAllPackages(consumptionPattern);
            analysis.put("allPackagesComparison", allPackagesComparison);
            
            // 소비 인사이트
            Map<String, Object> insights = generateConsumptionInsights(consumptionPattern);
            analysis.put("insights", insights);
            
            // 추천 이유 상세 분석
            Map<String, Object> recommendationReason = generateDetailedRecommendationReason(consumptionPattern);
            analysis.put("recommendationReason", recommendationReason);
            
            log.info("✅ 혜택 추천 상세 분석 완료");
            return analysis;
            
        } catch (Exception e) {
            log.error("❌ 혜택 추천 상세 분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("혜택 추천 상세 분석에 실패했습니다.", e);
        }
    }

    /**
     * 혜택 패키지 비교
     */
    public Map<String, Object> compareBenefitPackages(Long userId, EcoConsumptionAnalysisResponse consumptionAnalysis, java.util.List<String> packageCodes) {
        log.info("⚖️ 사용자 {}의 혜택 패키지 비교 시작", userId);
        
        try {
            Map<String, Object> comparison = new HashMap<>();
            Map<String, Object> consumptionPattern = analyzeConsumptionPattern(consumptionAnalysis);
            
            java.util.List<Map<String, Object>> packageComparisons = new ArrayList<>();
            
            for (String packageCode : packageCodes) {
                Map<String, Object> packageComparison = new HashMap<>();
                
                // 패키지 정보
                packageComparison.put("packageCode", packageCode);
                packageComparison.put("packageName", getPackageName(packageCode));
                
                // 예상 혜택 계산
                Map<String, Object> expectedBenefits = calculateExpectedBenefits(packageCode, consumptionAnalysis);
                packageComparison.put("expectedBenefits", expectedBenefits);
                
                // 적합도 점수
                double suitabilityScore = calculateSuitabilityScore(packageCode, consumptionPattern);
                packageComparison.put("suitabilityScore", suitabilityScore);
                
                // 장단점
                Map<String, Object> prosAndCons = getProsAndCons(packageCode, consumptionPattern);
                packageComparison.put("prosAndCons", prosAndCons);
                
                packageComparisons.add(packageComparison);
            }
            
            // 점수 기준으로 정렬
            packageComparisons.sort((a, b) -> 
                Double.compare((Double) b.get("suitabilityScore"), (Double) a.get("suitabilityScore")));
            
            comparison.put("packageComparisons", packageComparisons);
            comparison.put("consumptionPattern", consumptionPattern);
            comparison.put("recommendation", packageComparisons.get(0)); // 최고 점수 패키지
            
            log.info("✅ 혜택 패키지 비교 완료");
            return comparison;
            
        } catch (Exception e) {
            log.error("❌ 혜택 패키지 비교 실패: {}", e.getMessage(), e);
            throw new RuntimeException("혜택 패키지 비교에 실패했습니다.", e);
        }
    }

    // 추가 유틸리티 메서드들
    private Map<String, Object> analyzeSpecificPackage(String packageCode, Map<String, Object> consumptionPattern) {
        Map<String, Object> analysis = new HashMap<>();
        
        analysis.put("packageCode", packageCode);
        analysis.put("packageName", getPackageName(packageCode));
        
        // 적합도 분석
        double suitabilityScore = calculateSuitabilityScore(packageCode, consumptionPattern);
        analysis.put("suitabilityScore", suitabilityScore);
        
        // 예상 혜택
        Map<String, BigDecimal> cashbackRates = getCashbackRates(packageCode);
        analysis.put("cashbackRates", cashbackRates);
        
        return analysis;
    }

    private Map<String, Object> compareAllPackages(Map<String, Object> consumptionPattern) {
        Map<String, Object> comparison = new HashMap<>();
        
        java.util.List<String> allPackages = java.util.Arrays.asList("ALL_GREEN_LIFE", "GREEN_MOBILITY", "ZERO_WASTE_LIFE");
        java.util.List<Map<String, Object>> packageScores = new ArrayList<>();
        
        for (String packageCode : allPackages) {
            Map<String, Object> packageScore = new HashMap<>();
            packageScore.put("packageCode", packageCode);
            packageScore.put("packageName", getPackageName(packageCode));
            packageScore.put("score", calculateSuitabilityScore(packageCode, consumptionPattern));
            packageScores.add(packageScore);
        }
        
        // 점수 기준으로 정렬
        packageScores.sort((a, b) -> 
            Double.compare((Double) b.get("score"), (Double) a.get("score")));
        
        comparison.put("packageScores", packageScores);
        comparison.put("bestPackage", packageScores.get(0));
        
        return comparison;
    }

    private Map<String, Object> generateConsumptionInsights(Map<String, Object> consumptionPattern) {
        Map<String, Object> insights = new HashMap<>();
        
        BigDecimal ecoRatio = (BigDecimal) consumptionPattern.get("ecoRatio");
        String consumptionType = (String) consumptionPattern.get("consumptionType");
        String topCategory = (String) consumptionPattern.get("topCategory");
        
        insights.put("ecoRatio", ecoRatio);
        insights.put("consumptionType", consumptionType);
        insights.put("topCategory", topCategory);
        
        // 인사이트 메시지 생성
        String insightMessage = generateInsightMessage(ecoRatio, consumptionType, topCategory);
        insights.put("insightMessage", insightMessage);
        
        // 개선 제안
        java.util.List<String> improvementSuggestions = generateImprovementSuggestions(consumptionType, ecoRatio);
        insights.put("improvementSuggestions", improvementSuggestions);
        
        return insights;
    }

    private Map<String, Object> generateDetailedRecommendationReason(Map<String, Object> consumptionPattern) {
        Map<String, Object> reason = new HashMap<>();
        
        BigDecimal ecoRatio = (BigDecimal) consumptionPattern.get("ecoRatio");
        String consumptionType = (String) consumptionPattern.get("consumptionType");
        String topCategory = (String) consumptionPattern.get("topCategory");
        
        reason.put("ecoRatio", ecoRatio);
        reason.put("consumptionType", consumptionType);
        reason.put("topCategory", topCategory);
        
        // 상세 추천 이유
        String detailedReason = generateDetailedReason(ecoRatio, consumptionType, topCategory);
        reason.put("detailedReason", detailedReason);
        
        // 예상 효과
        Map<String, Object> expectedEffects = generateExpectedEffects(consumptionType);
        reason.put("expectedEffects", expectedEffects);
        
        return reason;
    }

    private double calculateSuitabilityScore(String packageCode, Map<String, Object> consumptionPattern) {
        String consumptionType = (String) consumptionPattern.get("consumptionType");
        BigDecimal ecoRatio = (BigDecimal) consumptionPattern.get("ecoRatio");
        
        double score = 0.0;
        
        // 소비 패턴 매칭 점수
        if (consumptionType.equals("ECO_FOCUSED") && packageCode.equals("ALL_GREEN_LIFE")) {
            score += 0.4;
        } else if (consumptionType.equals("MOBILITY_FOCUSED") && packageCode.equals("GREEN_MOBILITY")) {
            score += 0.4;
        } else if (consumptionType.equals("ZERO_WASTE_FOCUSED") && packageCode.equals("ZERO_WASTE_LIFE")) {
            score += 0.4;
        } else {
            score += 0.2;
        }
        
        // 친환경 비율 점수
        if (ecoRatio.compareTo(new BigDecimal("70")) >= 0) {
            score += 0.3;
        } else if (ecoRatio.compareTo(new BigDecimal("40")) >= 0) {
            score += 0.2;
        } else {
            score += 0.1;
        }
        
        // 카테고리 매칭 점수
        score += 0.3; // 기본 점수
        
        return Math.min(score, 1.0);
    }

    private Map<String, Object> getProsAndCons(String packageCode, Map<String, Object> consumptionPattern) {
        Map<String, Object> prosAndCons = new HashMap<>();
        java.util.List<String> pros = new ArrayList<>();
        java.util.List<String> cons = new ArrayList<>();
        
        String consumptionType = (String) consumptionPattern.get("consumptionType");
        
        switch (packageCode) {
            case "ALL_GREEN_LIFE":
                pros.add("다양한 친환경 카테고리에서 혜택 제공");
                pros.add("균형잡힌 캐시백 비율");
                if (!consumptionType.equals("ECO_FOCUSED")) {
                    cons.add("특정 카테고리에서 높은 혜택을 받기 어려움");
                }
                break;
            case "GREEN_MOBILITY":
                pros.add("교통 관련 높은 캐시백 비율");
                pros.add("모빌리티 특화 혜택");
                if (!consumptionType.equals("MOBILITY_FOCUSED")) {
                    cons.add("교통 외 카테고리에서 낮은 혜택");
                }
                break;
            case "ZERO_WASTE_LIFE":
                pros.add("제로웨이스트 관련 최고 캐시백 비율");
                pros.add("환경 친화적 라이프스타일 지원");
                if (!consumptionType.equals("ZERO_WASTE_FOCUSED")) {
                    cons.add("제한된 카테고리에서만 높은 혜택");
                }
                break;
        }
        
        prosAndCons.put("pros", pros);
        prosAndCons.put("cons", cons);
        
        return prosAndCons;
    }

    private String getPackageName(String packageCode) {
        switch (packageCode) {
            case "ALL_GREEN_LIFE":
                return "올인원 그린라이프 캐시백";
            case "GREEN_MOBILITY":
                return "그린 모빌리티 캐시백";
            case "ZERO_WASTE_LIFE":
                return "제로웨이스트 라이프 캐시백";
            default:
                return "알 수 없는 패키지";
        }
    }

    private String generateInsightMessage(BigDecimal ecoRatio, String consumptionType, String topCategory) {
        if (ecoRatio.compareTo(new BigDecimal("70")) >= 0) {
            return String.format("친환경 소비 비율이 %.1f%%로 매우 높습니다. %s 카테고리에서 가장 많은 소비를 하고 있습니다.", 
                    ecoRatio, topCategory);
        } else if (ecoRatio.compareTo(new BigDecimal("40")) >= 0) {
            return String.format("친환경 소비 비율이 %.1f%%로 보통 수준입니다. %s 카테고리에서 가장 많은 소비를 하고 있습니다.", 
                    ecoRatio, topCategory);
        } else {
            return String.format("친환경 소비 비율이 %.1f%%로 개선의 여지가 있습니다. %s 카테고리에서 가장 많은 소비를 하고 있습니다.", 
                    ecoRatio, topCategory);
        }
    }

    private java.util.List<String> generateImprovementSuggestions(String consumptionType, BigDecimal ecoRatio) {
        java.util.List<String> suggestions = new ArrayList<>();
        
        if (ecoRatio.compareTo(new BigDecimal("50")) < 0) {
            suggestions.add("친환경 가맹점 이용을 늘려보세요");
            suggestions.add("대중교통 이용을 늘려보세요");
        }
        
        switch (consumptionType) {
            case "MOBILITY_FOCUSED":
                suggestions.add("전기차 충전소 이용을 늘려보세요");
                suggestions.add("공유 모빌리티 이용을 늘려보세요");
                break;
            case "ZERO_WASTE_FOCUSED":
                suggestions.add("리필스테이션 이용을 늘려보세요");
                suggestions.add("중고거래 플랫폼을 활용해보세요");
                break;
            default:
                suggestions.add("다양한 친환경 카테고리에서 소비해보세요");
                break;
        }
        
        return suggestions;
    }

    private String generateDetailedReason(BigDecimal ecoRatio, String consumptionType, String topCategory) {
        StringBuilder reason = new StringBuilder();
        
        reason.append("귀하의 소비 패턴을 분석한 결과, ");
        
        if (ecoRatio.compareTo(new BigDecimal("70")) >= 0) {
            reason.append("친환경 소비 비율이 매우 높아 ");
        } else if (ecoRatio.compareTo(new BigDecimal("40")) >= 0) {
            reason.append("친환경 소비 비율이 보통 수준이어서 ");
        } else {
            reason.append("친환경 소비 비율이 낮아 ");
        }
        
        reason.append(String.format("%s 카테고리에서 가장 많은 소비를 하고 있어 ", topCategory));
        
        switch (consumptionType) {
            case "ECO_FOCUSED":
                reason.append("종합적인 친환경 혜택 패키지를 추천합니다.");
                break;
            case "MOBILITY_FOCUSED":
                reason.append("모빌리티 특화 혜택 패키지를 추천합니다.");
                break;
            case "ZERO_WASTE_FOCUSED":
                reason.append("제로웨이스트 특화 혜택 패키지를 추천합니다.");
                break;
            default:
                reason.append("균형잡힌 혜택 패키지를 추천합니다.");
                break;
        }
        
        return reason.toString();
    }

    private Map<String, Object> generateExpectedEffects(String consumptionType) {
        Map<String, Object> effects = new HashMap<>();
        
        switch (consumptionType) {
            case "ECO_FOCUSED":
                effects.put("expectedCashbackIncrease", "15-25%");
                effects.put("ecoCategoryCoverage", "전체 친환경 카테고리");
                effects.put("savingsPotential", "월 3-5만원");
                break;
            case "MOBILITY_FOCUSED":
                effects.put("expectedCashbackIncrease", "20-30%");
                effects.put("ecoCategoryCoverage", "교통 관련 카테고리");
                effects.put("savingsPotential", "월 2-4만원");
                break;
            case "ZERO_WASTE_FOCUSED":
                effects.put("expectedCashbackIncrease", "25-35%");
                effects.put("ecoCategoryCoverage", "제로웨이스트 카테고리");
                effects.put("savingsPotential", "월 1-3만원");
                break;
            default:
                effects.put("expectedCashbackIncrease", "10-20%");
                effects.put("ecoCategoryCoverage", "다양한 카테고리");
                effects.put("savingsPotential", "월 1-2만원");
                break;
        }
        
        return effects;
    }
}
