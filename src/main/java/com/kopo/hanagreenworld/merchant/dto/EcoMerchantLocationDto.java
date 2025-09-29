package com.kopo.hanagreenworld.merchant.dto;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EcoMerchantLocationDto {
    private Long id;
    private String name;
    private String category;
    private String categoryDisplayName;
    private String categoryImageUrl;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneNumber;
    private String businessHours;
    private Boolean isVerified;
    private Double distance; // 사용자 위치로부터의 거리 (km)
    private String description;
    private String websiteUrl;
    private String ecoCertifications;
    private String ecoPractices;

    public static EcoMerchantLocationDto from(EcoMerchant merchant) {
        return EcoMerchantLocationDto.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .category(merchant.getCategory().name())
                .categoryDisplayName(merchant.getCategory().getDisplayName())
                .categoryImageUrl(merchant.getCategory().getImageUrl())
                .address(merchant.getAddress())
                .latitude(merchant.getLatitude())
                .longitude(merchant.getLongitude())
                .phoneNumber(merchant.getPhoneNumber())
                .businessHours(merchant.getBusinessHours())
                .isVerified(merchant.getIsVerified())
                .description(merchant.getDescription())
                .websiteUrl(merchant.getWebsiteUrl())
                .ecoCertifications(merchant.getEcoCertifications())
                .ecoPractices(merchant.getEcoPractices())
                .build();
    }

    public static EcoMerchantLocationDto from(EcoMerchant merchant, Double distance) {
        EcoMerchantLocationDto dto = from(merchant);
        dto.distance = distance;
        return dto;
    }
}

