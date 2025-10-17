package com.kopo.hanagreenworld.scheduler.controller;

import com.kopo.hanagreenworld.scheduler.MonthlyDataResetScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
@Tag(name = "스케줄러 관리", description = "스케줄러 수동 실행 및 관리 API")
public class SchedulerController {

    private final MonthlyDataResetScheduler monthlyDataResetScheduler;

    @PostMapping("/reset-monthly-data")
    @Operation(summary = "월간 데이터 초기화 수동 실행", description = "모든 사용자의 이번달 데이터를 수동으로 초기화합니다.")
    public ResponseEntity<Map<String, Object>> resetMonthlyDataManually() {
        try {
            log.info("🔧 관리자가 월간 데이터 초기화를 수동 실행했습니다.");
            
            monthlyDataResetScheduler.resetMonthlyData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "월간 데이터 초기화가 성공적으로 실행되었습니다.");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("월간 데이터 초기화 수동 실행 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "월간 데이터 초기화 실행 중 오류가 발생했습니다: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "스케줄러 상태 확인", description = "스케줄러의 현재 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("monthlyDataResetScheduler", "활성화됨");
        status.put("cronExpression", "0 0 0 1 * ? (매월 1일 0시)");
        status.put("description", "모든 사용자의 이번달 데이터 초기화");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
}
