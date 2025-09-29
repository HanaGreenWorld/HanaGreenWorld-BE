package com.kopo.hanagreenworld.merchant.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "eco_merchants",
    indexes = {
        @Index(name = "idx_merchant_category", columnList = "category"),
        @Index(name = "idx_merchant_location", columnList = "latitude, longitude"),
        @Index(name = "idx_merchant_active", columnList = "is_active")
    }
)
@Getter
@NoArgsConstructor
public class EcoMerchant extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Long id;

    @Column(name = "business_number", length = 20, nullable = false, unique = true)
    private String businessNumber; // 사업자번호

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private MerchantCategory category;

    public enum MerchantCategory {
        ECO_FOOD("친환경 식품/매장", "/static/assets/hana3dIcon/hanaIcon3d_105.png"),
        EV_CHARGING("전기차 충전", "/static/assets/hana3dIcon/hanaIcon3d_29.png"),
        RECYCLING_STORE("재활용/제로웨이스트", "/static/assets/zero_waste.png"),
        GREEN_BEAUTY("친환경 뷰티", "/static/assets/hana3dIcon/hanaIcon3d_4_119.png"),
        ECO_SHOPPING("친환경 쇼핑", "/static/assets/hana3dIcon/hanaIcon3d_107.png"),
        ORGANIC_CAFE("유기농 카페", "/static/assets/hana3dIcon/hanaIcon3d_4_89.png");
        
        private final String displayName;
        private final String imageUrl;
        
        MerchantCategory(String displayName, String imageUrl) {
            this.displayName = displayName;
            this.imageUrl = imageUrl;
        }

        public String getDisplayName() { return displayName; }
        public String getImageUrl() { return imageUrl; }
    }

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 위치 정보
    @Column(name = "address", length = 500, nullable = false)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    // 연락처 정보
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // 영업 정보
    @Column(name = "business_hours", length = 200)
    private String businessHours; // "09:00-18:00"

    @Column(name = "holiday", length = 100)
    private String holiday; // "월요일"

    // 친환경 인증 정보
    @Column(name = "eco_certifications", columnDefinition = "TEXT")
    private String ecoCertifications; // JSON 형태로 저장

    @Column(name = "eco_practices", columnDefinition = "TEXT")
    private String ecoPractices; // 친환경 실천 사항 JSON

    // 상태
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // 검증된 가맹점 여부

    @Builder
    public EcoMerchant(String businessNumber, String name, MerchantCategory category,
                      String description, String address, BigDecimal latitude, BigDecimal longitude,
                      String phoneNumber, String websiteUrl, String businessHours, String holiday,
                      String ecoCertifications, String ecoPractices, Boolean isActive, Boolean isVerified) {
        this.businessNumber = businessNumber;
        this.name = name;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.websiteUrl = websiteUrl;
        this.businessHours = businessHours;
        this.holiday = holiday;
        this.ecoCertifications = ecoCertifications;
        this.ecoPractices = ecoPractices;
        this.isActive = isActive == null ? true : isActive;
        this.isVerified = isVerified == null ? false : isVerified;
    }

    public void deactivate() { this.isActive = false; }
    public void verify() { this.isVerified = true; }
}