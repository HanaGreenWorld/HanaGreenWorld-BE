package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardIntegratedInfoResponse {

    private CardListInfo cardList;

    private List<CardTransactionResponse> transactions;

    private CardConsumptionSummaryResponse consumptionSummary;

    private Map<String, Object> ecoBenefits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardListInfo {
        private Long totalCards;
        private Long totalCreditLimit;
        private Long usedAmount;
        private Long availableLimit;
        private String primaryCardName;
        private String primaryCardType;

        private List<CardDetail> cards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDetail {
        private String cardNumber;      // 마스킹된 카드번호
        private String cardName;        // 카드 이름
        private String cardType;        // 카드 타입
        private String cardStatus;      // 카드 상태
        private Long creditLimit;       // 신용한도
        private Long availableLimit;    // 사용가능한도
        private Long monthlyUsage;      // 월 사용금액
        private String cardImageUrl;    // 카드 이미지 URL
        private java.time.LocalDateTime issueDate;   // 발급일
        private java.time.LocalDateTime expiryDate;  // 만료일
        private List<String> benefits;  // 혜택 목록
    }
}
