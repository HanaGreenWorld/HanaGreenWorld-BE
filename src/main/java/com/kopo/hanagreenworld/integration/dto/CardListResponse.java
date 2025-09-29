package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 카드 목록 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardListResponse {

    private List<CardInfo> cards;
    private CardSummary summary;
    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {
        private String cardNumber; // 마스킹된 카드번호
        private String cardName;
        private String cardType; // CREDIT, DEBIT
        private String cardStatus; // ACTIVE, INACTIVE
        private BigDecimal creditLimit;
        private BigDecimal availableLimit;
        private BigDecimal monthlyUsage;
        private LocalDateTime issueDate;
        private LocalDateTime expiryDate;
        private List<String> benefits;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardSummary {
        private int totalCardCount;
        private int activeCardCount;
        private BigDecimal totalCreditLimit;
        private BigDecimal totalAvailableLimit;
        private BigDecimal monthlyTotalUsage;
        private String primaryCardType;
    }
}









