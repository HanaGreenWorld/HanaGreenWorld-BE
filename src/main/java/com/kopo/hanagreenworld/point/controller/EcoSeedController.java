package com.kopo.hanagreenworld.point.controller;

import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.dto.EcoSeedConvertRequest;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.dto.EcoSeedResponse;
import com.kopo.hanagreenworld.point.dto.EcoSeedTransactionResponse;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/eco-seeds")
@RequiredArgsConstructor
@Tag(name = "원큐씨앗 API", description = "원큐씨앗 적립, 사용, 전환 API")
public class EcoSeedController {

    private final EcoSeedService ecoSeedService;

    @GetMapping
    @Operation(summary = "원큐씨앗 정보 조회", description = "현재 사용자의 원큐씨앗 잔액 및 정보를 조회합니다.")
    public ResponseEntity<EcoSeedResponse> getEcoSeedInfo() {
        log.info("원큐씨앗 정보 조회 요청");
        EcoSeedResponse response = ecoSeedService.getEcoSeedInfo();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @Operation(summary = "회원 프로필 정보 조회", description = "현재 사용자의 member_profile 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getMemberProfile() {
        log.info("회원 프로필 정보 조회 요청");
        Map<String, Object> profile = ecoSeedService.getMemberProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/stats")
    @Operation(summary = "사용자 통계 정보 조회", description = "현재 사용자의 레벨, 탄소 절약량 등 통계 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            log.info("사용자 통계 정보 조회 요청");
            Map<String, Object> stats = ecoSeedService.getUserStats();
            log.info("사용자 통계 정보 조회 성공");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("사용자 통계 정보 조회 실패: {}", e.getMessage(), e);
            
            // 에러 시 기본값 반환
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalPoints", 0L);
            defaultStats.put("totalCarbonSaved", 0.0);
            defaultStats.put("totalActivities", 0);
            defaultStats.put("monthlyPoints", 0L);
            defaultStats.put("monthlyCarbonSaved", 0.0);
            defaultStats.put("monthlyActivities", 0);
            
            // 기본 레벨 정보
            Map<String, Object> currentLevel = new HashMap<>();
            currentLevel.put("id", "beginner");
            currentLevel.put("name", "친환경 새내기");
            currentLevel.put("description", "🌱 환경 보호 여정을 시작했어요!");
            currentLevel.put("requiredPoints", 0L);
            currentLevel.put("icon", "🌱");
            currentLevel.put("color", "#10B981");
            defaultStats.put("currentLevel", currentLevel);
            
            Map<String, Object> nextLevel = new HashMap<>();
            nextLevel.put("id", "intermediate");
            nextLevel.put("name", "친환경 실천가");
            nextLevel.put("description", "🌿 환경 보호를 실천하고 있어요!");
            nextLevel.put("requiredPoints", 5000L);
            nextLevel.put("icon", "🌿");
            nextLevel.put("color", "#059669");
            defaultStats.put("nextLevel", nextLevel);
            
            defaultStats.put("progressToNextLevel", 0.0);
            defaultStats.put("pointsToNextLevel", 5000L);
            
            log.info("기본 통계 정보 반환");
            return ResponseEntity.ok(defaultStats);
        }
    }

    @PostMapping("/earn")
    @Operation(summary = "원큐씨앗 적립", description = "원큐씨앗을 적립합니다.")
    public ResponseEntity<EcoSeedResponse> earnEcoSeeds(@Valid @RequestBody EcoSeedEarnRequest request) {
        log.info("원큐씨앗 적립 요청: {} - {}개", request.getCategory(), request.getPointsAmount());
        EcoSeedResponse response = ecoSeedService.earnEcoSeeds(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/convert")
    @Operation(summary = "하나머니 전환", description = "원큐씨앗을 하나머니로 전환합니다.")
    public ResponseEntity<EcoSeedResponse> convertToHanaMoney(@Valid @RequestBody EcoSeedConvertRequest request) {
        log.info("하나머니 전환 요청: {}개", request.getPointsAmount());
        EcoSeedResponse response = ecoSeedService.convertToHanaMoney(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "거래 내역 조회", description = "원큐씨앗 거래 내역을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("거래 내역 조회 요청");
        Page<EcoSeedTransactionResponse> page = ecoSeedService.getTransactionHistory(pageable);
        
        // Page 객체를 안전한 DTO로 변환
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());
        response.put("size", page.getSize());
        response.put("first", page.isFirst());
        response.put("last", page.isLast());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/category/{category}")
    @Operation(summary = "카테고리별 거래 내역 조회", description = "특정 카테고리의 원큐씨앗 거래 내역을 조회합니다.")
    public ResponseEntity<List<EcoSeedTransactionResponse>> getTransactionHistoryByCategory(
            @PathVariable PointCategory category) {
        log.info("카테고리별 거래 내역 조회 요청: {}", category);
        List<EcoSeedTransactionResponse> response = ecoSeedService.getTransactionHistoryByCategory(category);
        return ResponseEntity.ok(response);
    }

    // 편의를 위한 API들
    @PostMapping("/earn/walking")
    @Operation(summary = "걷기로 원큐씨앗 적립", description = "걷기 활동으로 원큐씨앗을 적립합니다.")
    public ResponseEntity<EcoSeedResponse> earnFromWalking(@RequestParam Integer steps) {
        log.info("걷기로 원큐씨앗 적립 요청: {}걸음", steps);
        
        // 걸음 수에 따른 원큐씨앗 계산 (1000걸음 = 1원큐씨앗)
        int points = steps / 1000;
        if (points == 0 && steps > 0) points = 1; // 최소 1개

        EcoSeedEarnRequest request = EcoSeedEarnRequest.builder()
                .category(PointCategory.WALKING)
                .pointsAmount(points)
                .description(steps + "걸음")
                .build();
        
        EcoSeedResponse response = ecoSeedService.earnEcoSeeds(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/earn/quiz")
    @Operation(summary = "퀴즈로 원큐씨앗 적립", description = "퀴즈 완료로 원큐씨앗을 적립합니다.")
    public ResponseEntity<EcoSeedResponse> earnFromQuiz(@RequestParam String quizType) {
        log.info("퀴즈로 원큐씨앗 적립 요청: {}", quizType);


        EcoSeedEarnRequest request = EcoSeedEarnRequest.builder()
                .category(PointCategory.DAILY_QUIZ)
                .pointsAmount(5)
                .description(quizType + " 퀴즈 완료로 원큐씨앗 적립")
                .build();
        
        EcoSeedResponse response = ecoSeedService.earnEcoSeeds(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/earn/challenge")
    @Operation(summary = "챌린지로 원큐씨앗 적립", description = "챌린지 완료로 원큐씨앗을 적립합니다.")
    public ResponseEntity<EcoSeedResponse> earnFromChallenge(@RequestParam String challengeName) {
        log.info("챌린지로 원큐씨앗 적립 요청: {}", challengeName);

        EcoSeedEarnRequest request = EcoSeedEarnRequest.builder()
                .category(PointCategory.ECO_CHALLENGE)
                .pointsAmount(10)
                .description(challengeName + " 챌린지 완료로 원큐씨앗 적립")
                .build();
        
        EcoSeedResponse response = ecoSeedService.earnEcoSeeds(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calendar")
    @Operation(summary = "달력 데이터 조회", description = "월별 씨앗 획득 현황을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCalendarData(
            @RequestParam int year,
            @RequestParam int month) {
        log.info("달력 데이터 조회 요청: {}.{}", year, month);
        Map<String, Object> response = ecoSeedService.getCalendarData(year, month);
        return ResponseEntity.ok(response);
    }
}
