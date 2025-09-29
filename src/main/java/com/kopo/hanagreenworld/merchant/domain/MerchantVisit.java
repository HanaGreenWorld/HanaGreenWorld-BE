package com.kopo.hanagreenworld.merchant.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.product.domain.Product; // 카드 상품 정보

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "merchant_visits",
    indexes = {
        @Index(name = "idx_visit_member_merchant", columnList = "member_id, merchant_id"),
        @Index(name = "idx_visit_card", columnList = "card_product_id"),
        @Index(name = "idx_visit_date", columnList = "visit_date"),
        @Index(name = "idx_visit_transaction", columnList = "transaction_id")
    }
)
@Getter
@NoArgsConstructor
public class MerchantVisit extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private EcoMerchant merchant;

    // 카드 정보 (핵심!)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_product_id", nullable = false)
    private Product cardProduct; // 사용한 카드 상품

    @Column(name = "card_number_masked", length = 20)
    private String cardNumberMasked; // 마스킹된 카드번호 (****1234)

    // 거래 정보
    @Column(name = "transaction_id", length = 100)
    private String transactionId; // 카드 거래 ID

    @Column(name = "transaction_amount", nullable = false)
    private Long transactionAmount;

    @Column(name = "benefit_amount")
    private Long benefitAmount; // 받은 혜택 금액

    @Column(name = "benefit_rate", precision = 5, scale = 2)
    private BigDecimal benefitRate; // 적용된 혜택 비율

    @Column(name = "points_earned")
    private Integer pointsEarned; // 적립된 포인트 (원큐씨앗)

    // 방문 정보
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_time")
    private LocalDateTime visitTime;

    // 위치 정보 (실제 방문 위치)
    @Column(name = "visit_latitude", precision = 10, scale = 7)
    private BigDecimal visitLatitude;

    @Column(name = "visit_longitude", precision = 10, scale = 7)
    private BigDecimal visitLongitude;

    // 상태
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // 방문 인증 여부

    @Builder
    public MerchantVisit(Member member, EcoMerchant merchant, Product cardProduct,
                        String cardNumberMasked, String transactionId,
                        Long transactionAmount, Long benefitAmount, BigDecimal benefitRate,
                        Integer pointsEarned, LocalDate visitDate, LocalDateTime visitTime,
                        BigDecimal visitLatitude, BigDecimal visitLongitude, Boolean isVerified) {
        this.member = member;
        this.merchant = merchant;
        this.cardProduct = cardProduct;
        this.cardNumberMasked = cardNumberMasked;
        this.transactionId = transactionId;
        this.transactionAmount = transactionAmount;
        this.benefitAmount = benefitAmount;
        this.benefitRate = benefitRate;
        this.pointsEarned = pointsEarned;
        this.visitDate = visitDate == null ? LocalDate.now() : visitDate;
        this.visitTime = visitTime == null ? LocalDateTime.now() : visitTime;
        this.visitLatitude = visitLatitude;
        this.visitLongitude = visitLongitude;
        this.isVerified = isVerified == null ? false : isVerified;
    }

    public void verify() { this.isVerified = true; }
}