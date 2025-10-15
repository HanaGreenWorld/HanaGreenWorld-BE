package com.kopo.hanagreenworld.merchant.listener;

import com.kopo.hanagreenworld.merchant.service.EcoMerchantMatchingService;
import com.kopo.hanagreenworld.merchant.event.CardTransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardTransactionEventListener {

    private final EcoMerchantMatchingService ecoMerchantMatchingService;

    @EventListener
    @Async
    @Transactional
    public void handleCardTransactionCreated(CardTransactionCreatedEvent event) {
        try {
            Map<String, Object> result = ecoMerchantMatchingService.processEcoMerchantTransaction(
                event.getUserId(),
                event.getBusinessNumber(),
                event.getMerchantName(),
                event.getAmount(),
                event.getTransactionDate().toString()
            );

            boolean isEcoMerchant = (Boolean) result.getOrDefault("isEcoMerchant", false);
            if (isEcoMerchant) {
                log.info("친환경 가맹점 매칭 성공 - 거래ID: {}, 가맹점: {}, 추가씨앗: {}",
                        event.getTransactionId(), result.get("merchantName"), result.get("additionalSeeds"));
            } else {
                log.info("ℹ일반 가맹점 - 거래ID: {}, 가맹점: {}",
                        event.getTransactionId(), event.getMerchantName());
            }
            
        } catch (Exception e) {
            log.error("카드 거래 이벤트 처리 실패 - 거래ID: {}, 에러: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }
}
