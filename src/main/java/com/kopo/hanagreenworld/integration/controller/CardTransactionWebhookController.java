package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.merchant.event.CardTransactionCreatedEvent;
import com.kopo.hanagreenworld.merchant.service.EcoMerchantMatchingService;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/integration/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Transaction Webhook", description = "카드 거래 웹훅 API")
public class CardTransactionWebhookController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/card-transaction")
    @Operation(
        summary = "카드 거래 웹훅", 
        description = "하나카드에서 카드 거래 발생 시 친환경 가맹점 매칭 및 원큐씨앗 지급을 처리합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleCardTransactionWebhook(
            @RequestBody Map<String, Object> request) {
        
        try {
            // 웹훅 데이터 파싱
            Long transactionId = Long.valueOf(request.get("transactionId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            String merchantName = (String) request.get("merchantName");
            String businessNumber = (String) request.get("businessNumber");
            Long amount = Long.valueOf(request.get("amount").toString());
            String category = (String) request.get("category");
            String merchantCategory = (String) request.get("merchantCategory");
            
            // transactionDate 파싱
            LocalDateTime transactionDate;
            Object transactionDateObj = request.get("transactionDate");
            if (transactionDateObj instanceof String) {
                transactionDate = LocalDateTime.parse((String) transactionDateObj);
            } else {
                transactionDate = LocalDateTime.now();
            }

            // 이벤트 발행 (비동기 처리로 중복 방지)
            CardTransactionCreatedEvent event = CardTransactionCreatedEvent.of(
                transactionId, userId, merchantName, businessNumber, 
                amount, transactionDate, category, merchantCategory
            );
            eventPublisher.publishEvent(event);


            return ResponseEntity.ok(ApiResponse.success(
                "카드 거래 웹훅 이벤트가 발행되었습니다.",
                Map.of("transactionId", transactionId, "status", "event_published")
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("카드 거래 웹훅 처리에 실패했습니다: " + e.getMessage())
            );
        }
    }
}
