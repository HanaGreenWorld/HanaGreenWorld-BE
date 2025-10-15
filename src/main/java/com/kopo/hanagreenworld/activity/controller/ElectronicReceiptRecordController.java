package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.dto.ElectronicReceiptRecordResponse;
import com.kopo.hanagreenworld.activity.service.ElectronicReceiptRecordService;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전자확인증 기록 컨트롤러
 */
@RestController
@RequestMapping("/api/electronic-receipts")
@RequiredArgsConstructor
@Slf4j
public class ElectronicReceiptRecordController {

    private final ElectronicReceiptRecordService electronicReceiptRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ElectronicReceiptRecordResponse>>> getElectronicReceiptRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

            Page<ElectronicReceiptRecordResponse> records = electronicReceiptRecordService
                .getElectronicReceiptRecords(memberId, pageable);

            return ResponseEntity.ok(ApiResponse.success("전자확인증 기록을 조회했습니다.", records));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("전자확인증 기록 조회에 실패했습니다.", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ElectronicReceiptRecordResponse>>> getAllElectronicReceiptRecords() {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

            List<ElectronicReceiptRecordResponse> records = electronicReceiptRecordService
                .getElectronicReceiptRecords(memberId);

            return ResponseEntity.ok(ApiResponse.success("전자확인증 전체 기록을 조회했습니다.", records));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("전자확인증 전체 기록 조회에 실패했습니다.", e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ElectronicReceiptRecordResponse>>> getElectronicReceiptRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

            List<ElectronicReceiptRecordResponse> records = electronicReceiptRecordService
                .getElectronicReceiptRecordsByDateRange(memberId, startDate, endDate);

            return ResponseEntity.ok(ApiResponse.success("전자확인증 기간별 기록을 조회했습니다.", records));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("전자확인증 기간별 기록 조회에 실패했습니다.", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ElectronicReceiptRecordService.ElectronicReceiptStats>> getElectronicReceiptStats() {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

            ElectronicReceiptRecordService.ElectronicReceiptStats stats = electronicReceiptRecordService
                .getElectronicReceiptStats(memberId);

            return ResponseEntity.ok(ApiResponse.success("전자확인증 통계를 조회했습니다.", stats));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("전자확인증 통계 조회에 실패했습니다.", e.getMessage()));
        }
    }
}
