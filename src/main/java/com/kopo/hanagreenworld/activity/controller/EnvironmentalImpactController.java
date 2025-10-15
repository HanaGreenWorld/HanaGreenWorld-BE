package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.activity.service.EnvironmentalImpactService;
import com.kopo.hanagreenworld.activity.dto.EnvironmentalImpactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/environmental-impact")
@RequiredArgsConstructor
@Tag(name = "환경 임팩트 API", description = "사용자의 환경 임팩트 정보 조회 API")
public class EnvironmentalImpactController {

    private final EnvironmentalImpactService environmentalImpactService;

    @GetMapping("/{userId}")
    @Operation(summary = "환경 임팩트 조회", description = "사용자의 환경 임팩트 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<EnvironmentalImpactResponse>> getEnvironmentalImpact(@PathVariable Long userId) {
        try {
            EnvironmentalImpactResponse impact = environmentalImpactService.getEnvironmentalImpact(userId);

            return ResponseEntity.ok(ApiResponse.success("환경 임팩트 정보를 조회했습니다.", impact));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("환경 임팩트 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}/monthly")
    @Operation(summary = "월간 환경 임팩트 조회", description = "사용자의 월간 환경 임팩트 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<EnvironmentalImpactResponse>> getMonthlyEnvironmentalImpact(@PathVariable Long userId) {
        try {
            EnvironmentalImpactResponse impact = environmentalImpactService.getMonthlyEnvironmentalImpact(userId);

            return ResponseEntity.ok(ApiResponse.success("월간 환경 임팩트 정보를 조회했습니다.", impact));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("월간 환경 임팩트 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
