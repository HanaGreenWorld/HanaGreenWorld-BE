package com.kopo.hanagreenworld.merchant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "merchant_benefits",
    indexes = {
        @Index(name = "idx_merchant_benefit_merchant", columnList = "merchant_id"),
        @Index(name = "idx_merchant_benefit_active", columnList = "is_active")
    }
)
@Getter
@NoArgsConstructor
public class MerchantBenefit extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private EcoMerchant merchant;

    @Column(name = "benefit_type", length = 50, nullable = false)
    private String benefitType; // CASHBACK, POINT, DISCOUNT

    @Column(name = "rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal rate; // 혜택 비율 (%)

    @Column(name = "min_amount")
    private Long minAmount; // 최소 결제 금액

    @Column(name = "max_amount")
    private Long maxAmount; // 최대 혜택 금액

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public MerchantBenefit(EcoMerchant merchant, String benefitType, BigDecimal rate,
                          Long minAmount, Long maxAmount, String description,
                          LocalDateTime startDate, LocalDateTime endDate, Boolean isActive) {
        this.merchant = merchant;
        this.benefitType = benefitType;
        this.rate = rate;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }
}