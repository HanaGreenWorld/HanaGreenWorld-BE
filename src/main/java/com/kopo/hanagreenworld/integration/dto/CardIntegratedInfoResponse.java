package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 카드 통합 정보 응답 DTO
 * 카드 목록, 거래내역, 소비현황을 한 번에 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardIntegratedInfoResponse {
    
    /**
     * 카드 목록 정보
     */
    private CardListInfo cardList;
    
    /**
     * 카드 거래내역
     */
    private List<CardTransactionResponse> transactions;
    
    /**
     * 월간 소비현황
     */
    private CardConsumptionSummaryResponse consumptionSummary;
    
    /**
     * 친환경 혜택 정보
     */
    private Map<String, Object> ecoBenefits;
    
    /**
     * 카드 목록 정보 내부 클래스
     */
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
        
        // 실제 카드 목록 추가 💳
        private List<CardDetail> cards;
    }
    
    /**
     * 개별 카드 상세 정보
     */
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
