package com.kopo.hanagreenworld.member.controller;

import com.kopo.hanagreenworld.member.domain.EcoReport;
import com.kopo.hanagreenworld.member.service.EcoReportService;
import com.kopo.hanagreenworld.member.dto.EcoReportResponse;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/eco-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Eco Report API", description = "친환경 리포트 관련 API")
public class EcoReportController {

    private final EcoReportService ecoReportService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(
        summary = "사용자 리포트 목록 조회",
        description = "현재 로그인한 사용자의 모든 친환경 리포트를 최신순으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<EcoReportResponse>>> getReportsByMemberId(
            @Parameter(description = "회원 ID") @RequestParam Long memberId) {
        
        try {
            List<EcoReport> reports = ecoReportService.getReportsByMemberId(memberId);
            List<EcoReportResponse> responseList = reports.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(responseList));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("리포트 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{reportMonth}")
    @Operation(
        summary = "특정 월 리포트 조회",
        description = "지정된 월의 친환경 리포트를 조회합니다."
    )
    public ResponseEntity<ApiResponse<EcoReportResponse>> getReportByMonth(
            @Parameter(description = "회원 ID") @RequestParam Long memberId,
            @Parameter(description = "리포트 월 (YYYY-MM 형식)") @PathVariable String reportMonth) {
        
        try {
            Optional<EcoReport> report = ecoReportService.getReportByMemberIdAndMonth(memberId, reportMonth);
            
            if (report.isEmpty()) {
                log.warn("리포트를 찾을 수 없음 - memberId: {}, reportMonth: {}", memberId, reportMonth);
                return ResponseEntity.notFound().build();
            }
            
            EcoReportResponse response = convertToResponse(report.get());

            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("리포트 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/generate")
    @Operation(
        summary = "리포트 수동 생성",
        description = "지정된 사용자의 특정 월 리포트를 수동으로 생성합니다. (관리자/개발용)"
    )
    public ResponseEntity<ApiResponse<EcoReportResponse>> generateReport(
            @Parameter(description = "회원 ID") @RequestParam Long memberId,
            @Parameter(description = "리포트 월 (YYYY-MM 형식)") @RequestParam String reportMonth) {
        
        try {
            EcoReport report = ecoReportService.generateMonthlyReport(memberId, reportMonth);
            EcoReportResponse response = convertToResponse(report);

            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("리포트 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    private EcoReportResponse convertToResponse(EcoReport report) {
        try {
            return EcoReportResponse.builder()
                    .reportId(report.getId())
                    .reportMonth(report.getReportMonth())
                    .summary(createSummary(report))
                    .statistics(createStatistics(report))
                    .activities(parseActivitiesData(report.getActivitiesData()))
                    .financialBenefit(parseFinancialBenefit(report.getFinancialBenefit()))
                    .ranking(parseUserRanking(report.getUserRanking()))
                    .environmentalImpact(parseEnvironmentalImpact(report.getEnvironmentalImpact()))
                    .createdAt(report.getCreatedAt())
                    .updatedAt(report.getCreatedAt())
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("리포트 응답 변환에 실패했습니다.", e);
        }
    }

    private EcoReportResponse.Summary createSummary(EcoReport report) {
        return EcoReportResponse.Summary.builder()
                .currentLevel(report.getCurrentLevel())
                .levelProgress(report.getLevelProgress() != null ? report.getLevelProgress().doubleValue() : 0.0)
                .pointsToNextLevel(report.getPointsToNextLevel())
                .topActivity(report.getTopActivity())
                .topActivityMessage(generateTopActivityMessage(report.getTopActivity()))
                .build();
    }

    private EcoReportResponse.Statistics createStatistics(EcoReport report) {
        return EcoReportResponse.Statistics.builder()
                .totalSeeds(report.getTotalSeeds())
                .totalActivities(report.getTotalActivities())
                .totalCarbonKg(report.getTotalCarbonKg() != null ? report.getTotalCarbonKg().doubleValue() : 0.0)
                .build();
    }

    private List<EcoReportResponse.Activity> parseActivitiesData(String activitiesDataJson) {
        try {
            if (activitiesDataJson == null || activitiesDataJson.trim().isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(activitiesDataJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EcoReportResponse.Activity.class));
        } catch (Exception e) {
            log.error("활동 데이터 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private EcoReportResponse.FinancialBenefit parseFinancialBenefit(String financialBenefitJson) {
        try {
            if (financialBenefitJson == null || financialBenefitJson.trim().isEmpty()) {
                return EcoReportResponse.FinancialBenefit.builder()
                        .savingsInterest(0)
                        .cardDiscount(0)
                        .loanBenefit(0)
                        .total(0)
                        .build();
            }
            return objectMapper.readValue(financialBenefitJson, EcoReportResponse.FinancialBenefit.class);
        } catch (Exception e) {
            log.error("금융 혜택 파싱 실패: {}", e.getMessage());
            return EcoReportResponse.FinancialBenefit.builder()
                    .savingsInterest(0)
                    .cardDiscount(0)
                    .loanBenefit(0)
                    .total(0)
                    .build();
        }
    }

    private EcoReportResponse.Ranking parseUserRanking(String userRankingJson) {
        try {
            if (userRankingJson == null || userRankingJson.trim().isEmpty()) {
                return EcoReportResponse.Ranking.builder()
                        .percentile(50)
                        .totalUsers(1000L)
                        .rank(500L)
                        .build();
            }
            return objectMapper.readValue(userRankingJson, EcoReportResponse.Ranking.class);
        } catch (Exception e) {
            log.error("사용자 랭킹 파싱 실패: {}", e.getMessage());
            return EcoReportResponse.Ranking.builder()
                    .percentile(50)
                    .totalUsers(1000L)
                    .rank(500L)
                    .build();
        }
    }

    /**
     * 환경 가치 환산 파싱
     */
    private EcoReportResponse.EnvironmentalImpact parseEnvironmentalImpact(String environmentalImpactJson) {
        try {
            if (environmentalImpactJson == null || environmentalImpactJson.trim().isEmpty()) {
                return EcoReportResponse.EnvironmentalImpact.builder()
                        .carbonKg(0.0)
                        .trees(0.0)
                        .waterLiters(0.0)
                        .plasticBags(0.0)
                        .build();
            }
            return objectMapper.readValue(environmentalImpactJson, EcoReportResponse.EnvironmentalImpact.class);
        } catch (Exception e) {
            log.error("환경 가치 환산 파싱 실패: {}", e.getMessage());
            return EcoReportResponse.EnvironmentalImpact.builder()
                    .carbonKg(0.0)
                    .trees(0.0)
                    .waterLiters(0.0)
                    .plasticBags(0.0)
                    .build();
        }
    }

    /**
     * Top 활동 메시지 생성
     */
    private String generateTopActivityMessage(String topActivity) {
        if (topActivity == null || topActivity.trim().isEmpty()) {
            return "친환경 활동을 시작해보세요!";
        }
        
        switch (topActivity) {
            case "걷기":
                return "자동차 말고 걷기로 탄소를 줄이셨네요!";
            case "퀴즈":
                return "친환경 지식을 쌓아가고 계시네요!";
            case "전자영수증":
                return "디지털 소비로 환경을 보호하고 계세요!";
            case "챌린지":
                return "친환경 챌린지로 지구를 지키고 계세요!";
            default:
                return "친환경 활동으로 지구를 보호하고 계세요!";
        }
    }
}
