package com.kopo.hanagreenworld.merchant.dto;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LocationSearchRequest {
    
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "33.0", message = "위도는 33.0 이상이어야 합니다")
    @DecimalMax(value = "39.0", message = "위도는 39.0 이하여야 합니다")
    private BigDecimal latitude;
    
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "124.0", message = "경도는 124.0 이상이어야 합니다")
    @DecimalMax(value = "132.0", message = "경도는 132.0 이하여야 합니다")
    private BigDecimal longitude;
    
    @Min(value = 1, message = "검색 반경은 최소 1km 이상이어야 합니다")
    @Max(value = 50, message = "검색 반경은 최대 50km 이하여야 합니다")
    private Integer radius = 10; // 기본값 10km
    
    private EcoMerchant.MerchantCategory category; // 특정 카테고리 필터링
    
    private String searchKeyword; // 가맹점명 검색
    
    private Boolean verifiedOnly = false; // 검증된 가맹점만 조회
}

