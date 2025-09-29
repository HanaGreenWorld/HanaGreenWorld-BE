package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.dto.WalkingConsentRequest;
import com.kopo.hanagreenworld.activity.dto.WalkingConsentResponse;
import com.kopo.hanagreenworld.activity.dto.WalkingStepsRequest;
import com.kopo.hanagreenworld.activity.dto.WalkingResponse;
import com.kopo.hanagreenworld.activity.service.WalkingService;
import com.kopo.hanagreenworld.common.response.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Walking Controller", description = "걷기 활동 관련 API")
@RestController
@RequestMapping("/walking")
@RequiredArgsConstructor
public class WalkingController {

    private final WalkingService walkingService;

    @Operation(summary = "걷기 측정 동의 상태 조회", description = "현재 사용자의 걷기 측정 동의 상태를 조회합니다.")
    @GetMapping("/consent")
    public ResponseEntity<ApiResponse<WalkingConsentResponse>> getWalkingConsent() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        WalkingConsentResponse response = walkingService.getWalkingConsent(memberId);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "걷기 측정 동의 상태 업데이트", description = "걷기 측정 동의 상태를 업데이트합니다.")
    @PutMapping("/consent")
    public ResponseEntity<ApiResponse<WalkingConsentResponse>> updateWalkingConsent(
            @RequestBody WalkingConsentRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        WalkingConsentResponse response = walkingService.updateWalkingConsent(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "걸음수 제출", description = "걸음수를 제출하고 포인트를 적립합니다.")
    @PostMapping("/steps")
    public ResponseEntity<ApiResponse<WalkingResponse>> submitWalkingSteps(
            @RequestBody WalkingStepsRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        WalkingResponse response = walkingService.submitWalkingSteps(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "오늘의 걷기 기록 조회", description = "오늘 제출한 걷기 기록을 조회합니다.")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<WalkingResponse>> getTodayWalkingRecord() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        WalkingResponse response = walkingService.getTodayWalkingRecord(memberId);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "월간 걷기 통계 조회", description = "특정 월의 걷기 통계를 조회합니다.")
    @GetMapping("/stats/{year}/{month}")
    public ResponseEntity<ApiResponse<Object[]>> getMonthlyWalkingStats(
            @PathVariable int year, @PathVariable int month) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Object[] stats = walkingService.getMonthlyWalkingStats(memberId, year, month);
        return ResponseEntity.ok(ApiResponse.success(stats, "월간 걷기 통계를 조회했습니다."));
    }

    @Operation(summary = "연속 걷기 일수 조회", description = "현재 연속으로 걷기 활동을 한 일수를 조회합니다.")
    @GetMapping("/streak")
    public ResponseEntity<ApiResponse<Integer>> getWalkingStreak() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Integer streak = walkingService.getWalkingStreak(memberId);
        return ResponseEntity.ok(ApiResponse.success(streak, "연속 걷기 일수를 조회했습니다."));
    }

    @Operation(summary = "최근 걷기 기록 조회", description = "사용자의 최근 걷기 기록 5개를 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<WalkingResponse>>> getRecentWalkingRecords() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<WalkingResponse> records = walkingService.getRecentWalkingRecords(memberId, 5);
        return ResponseEntity.ok(ApiResponse.success(records, "최근 걷기 기록을 조회했습니다."));
    }

    @Operation(summary = "걷기 컨트롤러 헬스체크", description = "걷기 컨트롤러가 정상적으로 작동하는지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Walking Controller is working!");
    }
}
