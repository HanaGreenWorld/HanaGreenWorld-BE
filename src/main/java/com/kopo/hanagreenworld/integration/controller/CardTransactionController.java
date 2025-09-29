package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.common.response.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.integration.service.CardTransactionIntegrationService;
import com.kopo.hanagreenworld.integration.dto.CardTransactionResponse;
import com.kopo.hanagreenworld.integration.dto.CardConsumptionSummaryResponse;
import com.kopo.hanagreenworld.integration.dto.CardIntegratedInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/integration/cards")
@RequiredArgsConstructor
@Tag(name = "카드 통합 API", description = "하나카드 서버와 연동하는 카드 관련 통합 API")
public class CardTransactionController {

    private final CardTransactionIntegrationService cardTransactionIntegrationService;

    /**
     * 카드 거래내역 조회 API 💳
     * 
     * @param memberId 회원 ID
     * @return 카드 거래내역 목록
     */
    @GetMapping("/{memberId}/transactions")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "카드 거래내역 조회",
        description = "하나카드 서버에서 고객의 카드 거래내역을 조회합니다. " +
                     "💳 최근 거래내역, 카테고리별 분류, 금액 정보를 제공합니다."
    )
    public ResponseEntity<ApiResponse<List<CardTransactionResponse>>> getCardTransactions(
            @PathVariable Long memberId) {
        
        try {
            log.info("카드 거래내역 조회 요청 - 회원ID: {}", memberId);

            List<CardTransactionResponse> transactions = cardTransactionIntegrationService.getCardTransactions(memberId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    transactions, "카드 거래내역 조회가 완료되었습니다. 💳"));

        } catch (Exception e) {
            log.error("카드 거래내역 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카드 거래내역 조회에 실패했습니다."));
        }
    }

    /**
     * 월간 소비현황 조회 API 📊
     * 
     * @param memberId 회원 ID
     * @return 월간 소비현황 요약
     */
    @GetMapping("/{memberId}/consumption/summary")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "월간 소비현황 조회",
        description = "하나카드 서버에서 고객의 이번 달 소비현황을 조회합니다. " +
                     "📊 총 소비금액, 카테고리별 분석, 캐시백 정보를 제공합니다."
    )
    public ResponseEntity<ApiResponse<CardConsumptionSummaryResponse>> getMonthlyConsumptionSummary(
            @PathVariable Long memberId) {
        
        try {
            log.info("월간 소비현황 조회 요청 - 회원ID: {}", memberId);

            CardConsumptionSummaryResponse summary = cardTransactionIntegrationService.getMonthlyConsumptionSummary(memberId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    summary, "월간 소비현황 조회가 완료되었습니다. 📊"));

        } catch (Exception e) {
            log.error("월간 소비현황 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("월간 소비현황 조회에 실패했습니다."));
        }
    }

    /**
     * 카테고리별 거래내역 조회 API 🏷️
     * 
     * @param memberId 회원 ID
     * @param category 카테고리
     * @return 카테고리별 거래내역
     */
    @GetMapping("/{memberId}/transactions/category/{category}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "카테고리별 거래내역 조회",
        description = "특정 카테고리의 카드 거래내역을 조회합니다. " +
                     "🏷️ 식비, 교통비, 쇼핑 등 카테고리별 소비 패턴을 분석할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<List<CardTransactionResponse>>> getTransactionsByCategory(
            @PathVariable Long memberId,
            @PathVariable String category) {
        
        try {
            log.info("카테고리별 거래내역 조회 요청 - 회원ID: {}, 카테고리: {}", memberId, category);

            List<CardTransactionResponse> transactions = cardTransactionIntegrationService.getTransactionsByCategory(memberId, category);
            
            return ResponseEntity.ok(ApiResponse.success(
                    transactions, "카테고리별 거래내역 조회가 완료되었습니다. 🏷️"));

        } catch (Exception e) {
            log.error("카테고리별 거래내역 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카테고리별 거래내역 조회에 실패했습니다."));
        }
    }

    /**
     * 카드 통합 정보 조회 API 🎯
     * 카드 목록, 거래내역, 소비현황을 한 번에 조회
     * 
     * @param memberId 회원 ID
     * @return 카드 통합 정보 (목록 + 거래내역 + 소비현황)
     */
    @GetMapping("/{memberId}/integrated")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "카드 통합 정보 조회",
        description = "카드 목록, 거래내역, 월간 소비현황을 한 번에 조회합니다. " +
                     "🎯 프론트엔드에서 여러 API 호출 대신 이 API 하나로 모든 카드 정보를 가져올 수 있습니다."
    )
    public ResponseEntity<ApiResponse<CardIntegratedInfoResponse>> getCardIntegratedInfo(
            @PathVariable Long memberId) {
        
        try {
            log.info("카드 통합 정보 조회 요청 - 회원ID: {}", memberId);

            CardIntegratedInfoResponse integratedInfo = cardTransactionIntegrationService.getCardIntegratedInfo(memberId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    integratedInfo, "카드 통합 정보 조회가 완료되었습니다. 🎯"));

        } catch (Exception e) {
            log.error("카드 통합 정보 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카드 통합 정보 조회에 실패했습니다."));
        }
    }
}
