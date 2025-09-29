package com.kopo.hanagreenworld.point.dto;

import com.kopo.hanagreenworld.point.domain.PointCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EcoSeedEarnRequest {
    
    @NotNull(message = "카테고리는 필수입니다.")
    private PointCategory category;
    
    @NotNull(message = "적립할 원큐씨앗 수량은 필수입니다.")
    @Positive(message = "적립할 원큐씨앗은 0보다 커야 합니다.")
    private Integer pointsAmount;
    
    private String description;

    @Builder
    public EcoSeedEarnRequest(PointCategory category, Integer pointsAmount, String description) {
        this.category = category;
        this.pointsAmount = pointsAmount;
        this.description = description;
    }
}
