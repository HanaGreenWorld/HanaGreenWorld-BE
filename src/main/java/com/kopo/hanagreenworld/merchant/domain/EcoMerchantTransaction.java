package com.kopo.hanagreenworld.merchant.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 친환경 가맹점 거래 내역
 * 카드 거래에서 친환경 가맹점 매칭 시 생성되는 거래 내역
 */
@Entity
@Table(
    name = "eco_merchant_transactions",
    indexes = {
        @Index(name = "idx_eco_transaction_member", columnList = "member_id"),
        @Index(name = "idx_eco_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_eco_transaction_merchant", columnList = "business_number"),
        @Index(name = "idx_eco_transaction_card_id", columnList = "card_transaction_id")
    }
)
@Getter
@NoArgsConstructor
public class EcoMerchantTransaction extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eco_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eco_merchant_id", nullable = false)
    private EcoMerchant ecoMerchant;

    // 카드 거래 정보
    @Column(name = "card_transaction_id", nullable = false)
    private Long cardTransactionId; // 하나카드의 거래 ID

    @Column(name = "merchant_name", length = 200, nullable = false)
    private String merchantName;

    @Column(name = "business_number", length = 20, nullable = false)
    private String businessNumber;

    @Column(name = "transaction_amount", nullable = false)
    private Long transactionAmount; // 실제 거래 금액

    @Column(name = "transaction_date", nullable = false)
    private java.time.LocalDateTime transactionDate;

    @Column(name = "category", length = 50)
    private String category; // 카드 거래 카테고리

    @Column(name = "merchant_category", length = 50)
    private String merchantCategory; // 가맹점 카테고리

    // 원큐씨앗 혜택 정보
    @Column(name = "earned_seeds", nullable = false)
    private Long earnedSeeds; // 지급된 원큐씨앗

    @Column(name = "user_level", length = 20)
    private String userLevel; // 사용자 레벨 (BRONZE, SILVER, GOLD, PLATINUM)

    @Column(name = "benefit_rate", precision = 5, scale = 2)
    private BigDecimal benefitRate; // 혜택 비율 (예: 0.01 = 1%)

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = true; // 처리 완료 여부

    @Builder
    public EcoMerchantTransaction(Member member, EcoMerchant ecoMerchant, Long cardTransactionId,
                                String merchantName, String businessNumber, Long transactionAmount,
                                java.time.LocalDateTime transactionDate, String category, String merchantCategory,
                                Long earnedSeeds, String userLevel, BigDecimal benefitRate, Boolean isProcessed) {
        this.member = member;
        this.ecoMerchant = ecoMerchant;
        this.cardTransactionId = cardTransactionId;
        this.merchantName = merchantName;
        this.businessNumber = businessNumber;
        this.transactionAmount = transactionAmount;
        this.transactionDate = transactionDate;
        this.category = category;
        this.merchantCategory = merchantCategory;
        this.earnedSeeds = earnedSeeds;
        this.userLevel = userLevel;
        this.benefitRate = benefitRate;
        this.isProcessed = isProcessed != null ? isProcessed : true;
    }

    /**
     * 거래 완료 처리
     */
    public void markAsProcessed() {
        this.isProcessed = true;
    }

    /**
     * 거래 취소 처리
     */
    public void markAsCancelled() {
        this.isProcessed = false;
    }
}
