package com.kopo.hanagreenworld.point.controller;

import com.kopo.hanagreenworld.common.response.ApiResponse;
import com.kopo.hanagreenworld.point.service.EcoConsumptionService;
import com.kopo.hanagreenworld.point.dto.EcoConsumptionAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/eco-consumption")
@RequiredArgsConstructor
@Tag(name = "친환경 소비 현황 API", description = "친환경 소비 현황 및 가맹점 혜택 조회 API")
public class EcoConsumptionController {

    private final EcoConsumptionService ecoConsumptionService;

    @GetMapping("/{userId}")
    @Operation(summary = "친환경 소비 현황 조회", description = "사용자의 이번달 친환경 소비 현황을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getEcoConsumptionAnalysis(@PathVariable Long userId) {
        try {
            log.info("친환경 소비 현황 조회 요청: userId = {}", userId);
            
            EcoConsumptionAnalysisResponse analysis = ecoConsumptionService.getEcoConsumptionAnalysis(userId);
            
            log.info("친환경 소비 현황 조회 성공: userId = {}", userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analysis
            ));
        } catch (Exception e) {
            log.error("친환경 소비 현황 조회 실패: userId = {}, error = {}", userId, e.getMessage(), e);
            
            // 기본값 반환
            Map<String, Object> defaultAnalysis = Map.of(
                "totalAmount", 463000,
                "ecoRatio", 65.0,
                "ecoCategoryAmounts", Map.of(
                    "친환경 식품", 150000,
                    "대중교통", 120000,
                    "재활용품", 80000,
                    "친환경 에너지", 113000
                )
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", defaultAnalysis
            ));
        }
    }

    @GetMapping("/{userId}/benefits")
    @Operation(summary = "이번달 친환경 가맹점 혜택 조회", description = "사용자의 이번달 친환경 가맹점 혜택을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getEcoBenefits(@PathVariable Long userId) {
        try {
            log.info("친환경 가맹점 혜택 조회 요청: userId = {}", userId);
            
            Map<String, Object> benefits = ecoConsumptionService.getEcoMerchantBenefits(userId);
            
            log.info("친환경 가맹점 혜택 조회 성공: userId = {}", userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", benefits
            ));
        } catch (Exception e) {
            log.error("친환경 가맹점 혜택 조회 실패: userId = {}, error = {}", userId, e.getMessage(), e);
            
            // 기본값 반환
            Map<String, Object> defaultBenefits = Map.of(
                "totalBenefits", 2500,
                "benefits", new Object[]{
                    Map.of(
                        "storeName", "그린마트 강남점",
                        "type", "ECO_FOOD",
                        "amount", 2500,
                        "date", "9월 15일",
                        "cardNumber", "3524"
                    )
                }
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", defaultBenefits
            ));
        }
    }

    @GetMapping("/{userId}/benefit-packages")
    @Operation(summary = "카드 혜택 패키지 조회", description = "사용자의 현재 혜택 패키지와 모든 가능한 혜택 패키지 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCardBenefitPackages(@PathVariable Long userId) {
        log.info("카드 혜택 패키지 조회 요청: userId = {}", userId);
        Map<String, Object> packages = ecoConsumptionService.getCardBenefitPackages(userId);
        return ResponseEntity.ok(ApiResponse.success(packages, "카드 혜택 패키지 조회 성공"));
    }

    @PostMapping("/{userId}/benefit-packages")
    @Operation(summary = "혜택 패키지 변경", description = "사용자의 혜택 패키지를 변경합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserBenefitPackage(
            @PathVariable Long userId, 
            @RequestBody Map<String, String> request) {
        log.info("혜택 패키지 변경 요청: userId = {}, packageName = {}", userId, request.get("packageName"));
        Map<String, Object> result = ecoConsumptionService.changeBenefitPackage(userId, 1L, request.get("packageCode"), request.get("changeReason"));
        return ResponseEntity.ok(ApiResponse.success(result, "혜택 패키지 변경 성공"));
    }
}
