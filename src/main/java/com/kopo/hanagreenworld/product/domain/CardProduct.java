package com.kopo.hanagreenworld.product.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_products")
@Getter
@NoArgsConstructor
public class CardProduct extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "card_type", length = 50, nullable = false)
    private String cardType; // 그린라이프카드, 원큐씨앗카드 등

    @Column(name = "card_image_url", length = 500)
    private String cardImageUrl; // 카드 이미지
    
    @Column(name = "card_design_code", length = 50)
    private String cardDesignCode; // 카드 디자인 코드

    @Column(name = "annual_fee")
    private Integer annualFee;

    @Column(name = "credit_limit")
    private Long creditLimit;

    @Column(name = "card_category", length = 50)
    private String cardCategory; // 신용카드, 체크카드

    @Column(name = "eco_benefit_focus", length = 200)
    private String ecoBenefitFocus; // 친환경 가맹점, 대중교통 등

    @Builder
    public CardProduct(Product product, String cardType, Integer annualFee, 
                      Long creditLimit, String cardCategory, String ecoBenefitFocus) {
        this.product = product;
        this.cardType = cardType;
        this.annualFee = annualFee;
        this.creditLimit = creditLimit;
        this.cardCategory = cardCategory;
        this.ecoBenefitFocus = ecoBenefitFocus;
    }
}