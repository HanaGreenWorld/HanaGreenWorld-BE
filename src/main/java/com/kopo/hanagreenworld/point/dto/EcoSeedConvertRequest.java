package com.kopo.hanagreenworld.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EcoSeedConvertRequest {
    
    @NotNull(message = "전환할 원큐씨앗 수량은 필수입니다.")
    @Positive(message = "전환할 원큐씨앗은 0보다 커야 합니다.")
    private Integer pointsAmount;
}
