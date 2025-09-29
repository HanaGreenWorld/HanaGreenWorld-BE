package com.kopo.hanagreenworld.ai.controller;

import com.kopo.hanagreenworld.ai.service.BenefitRecommendationService;
import com.kopo.hanagreenworld.point.dto.EcoConsumptionAnalysisResponse;
import com.kopo.hanagreenworld.point.service.EcoConsumptionService;
import com.kopo.hanagreenworld.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 기반 혜택 추천 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/benefit-recommendation")
@RequiredArgsConstructor
public class BenefitRecommendationController {

    private final BenefitRecommendationService benefitRecommendationService;
    private final EcoConsumptionService ecoConsumptionService;

    /**
     * 사용자별 개인화된 혜택 추천
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recommendBenefitPackage(
            @PathVariable Long userId) {
        
        log.info("🔍 사용자 {}의 혜택 추천 요청", userId);
        
        try {
            // 1. 사용자 소비 패턴 분석 데이터 조회
            EcoConsumptionAnalysisResponse consumptionAnalysis = 
                    ecoConsumptionService.getEcoConsumptionAnalysis(userId);
            
            // 2. AI 기반 혜택 추천
            Map<String, Object> recommendation = benefitRecommendationService
                    .recommendBenefitPackage(userId, consumptionAnalysis);
            
            log.info("✅ 사용자 {}의 혜택 추천 완료", userId);
            
            return ResponseEntity.ok(ApiResponse.success(recommendation, "혜택 추천이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("❌ 혜택 추천 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("혜택 추천에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 혜택 추천 상세 분석
     */
    @GetMapping("/users/{userId}/analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationAnalysis(
            @PathVariable Long userId,
            @RequestParam(required = false) String packageCode) {
        
        log.info("📊 사용자 {}의 혜택 추천 분석 요청", userId);
        
        try {
            // 소비 패턴 분석
            EcoConsumptionAnalysisResponse consumptionAnalysis = 
                    ecoConsumptionService.getEcoConsumptionAnalysis(userId);
            
            // 추천 분석 생성
            Map<String, Object> analysis = benefitRecommendationService
                    .getRecommendationAnalysis(userId, consumptionAnalysis, packageCode);
            
            return ResponseEntity.ok(ApiResponse.success(analysis, "혜택 추천 분석이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("❌ 혜택 추천 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("혜택 추천 분석에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 혜택 패키지 비교
     */
    @PostMapping("/users/{userId}/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareBenefitPackages(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {
        
        log.info("⚖️ 사용자 {}의 혜택 패키지 비교 요청", userId);
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> packageCodes = (java.util.List<String>) request.get("packageCodes");
            
            if (packageCodes == null || packageCodes.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("비교할 패키지 코드가 필요합니다."));
            }
            
            // 소비 패턴 분석
            EcoConsumptionAnalysisResponse consumptionAnalysis = 
                    ecoConsumptionService.getEcoConsumptionAnalysis(userId);
            
            // 패키지 비교
            Map<String, Object> comparison = benefitRecommendationService
                    .compareBenefitPackages(userId, consumptionAnalysis, packageCodes);
            
            return ResponseEntity.ok(ApiResponse.success(comparison, "혜택 패키지 비교가 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("❌ 혜택 패키지 비교 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("혜택 패키지 비교에 실패했습니다: " + e.getMessage()));
        }
    }
}
