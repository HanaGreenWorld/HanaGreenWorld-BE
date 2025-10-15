package com.kopo.hanagreenworld.merchant.controller;

import com.kopo.hanagreenworld.merchant.service.EcoMerchantMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/eco-merchant")
@RequiredArgsConstructor
public class EcoMerchantMatchingController {

    private final EcoMerchantMatchingService ecoMerchantMatchingService;

    @PostMapping("/matching")
    @Operation(
        summary = "친환경 가맹점 매칭 처리",
        description = "하나카드에서 거래내역의 사업자 번호로 친환경 가맹점 매칭 및 추가 혜택 처리"
    )
    public ResponseEntity<Map<String, Object>> processEcoMerchantMatching(
            @RequestBody Map<String, Object> request) {

        try {
            Long userId = ((Number) request.get("userId")).longValue();
            String businessNumber = (String) request.get("businessNumber");
            String merchantName = (String) request.get("merchantName");
            Long amount = ((Number) request.get("amount")).longValue();
            String transactionDate = (String) request.get("transactionDate");

            Map<String, Object> result = ecoMerchantMatchingService.processEcoMerchantTransaction(
                userId, businessNumber, merchantName, amount, transactionDate);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("친환경 가맹점 매칭 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{userId}/history")
    @Operation(
        summary = "친환경 가맹점 이용 내역 조회",
        description = "사용자의 친환경 가맹점 이용 내역을 조회합니다."
    )
    public ResponseEntity<Map<String, Object>> getUserEcoMerchantHistory(@PathVariable Long userId) {

        try {
            var history = ecoMerchantMatchingService.getUserEcoMerchantHistory(userId);
            var stats = ecoMerchantMatchingService.getEcoMerchantStats(userId);

            Map<String, Object> response = Map.of(
                "success", true,
                "userId", userId,
                "history", history,
                "stats", stats
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{userId}/stats")
    @Operation(
        summary = "친환경 가맹점 통계 조회",
        description = "사용자의 친환경 가맹점 이용 통계를 조회합니다."
    )
    public ResponseEntity<Map<String, Object>> getEcoMerchantStats(@PathVariable Long userId) {
        
        try {
            Map<String, Object> stats = ecoMerchantMatchingService.getEcoMerchantStats(userId);

            Map<String, Object> response = Map.of(
                "success", true,
                "userId", userId,
                "stats", stats
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
