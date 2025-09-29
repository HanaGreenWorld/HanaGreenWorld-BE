package com.kopo.hanagreenworld.product.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_code", length = 50, nullable = false, unique = true)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    public enum ProductType {
        SAVINGS("적금"),
        LOAN("대출"),
        INVESTMENT("투자"),
        CARD("카드");
        
        private final String displayName;
        
        ProductType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private ProductProvider provider;

    public enum ProductProvider {
        HANA_BANK("하나은행"),
        HANA_CARD("하나카드");
        
        private final String displayName;
        
        ProductProvider(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "external_id")
    private String externalId; // 외부 시스템 ID (하나카드 카드번호, 하나은행 계좌번호 등)

    @Column(name = "is_eco_friendly", nullable = false)
    private Boolean isEcoFriendly = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Product(String productCode, ProductType productType, ProductProvider provider,
                  String name, String description, String externalId, 
                  Boolean isEcoFriendly, Boolean isActive) {
        this.productCode = productCode;
        this.productType = productType;
        this.provider = provider;
        this.name = name;
        this.description = description;
        this.externalId = externalId;
        this.isEcoFriendly = isEcoFriendly == null ? false : isEcoFriendly;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }
}