package com.kopo.hanagreenworld.point.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EcoSeedResponse {
    private Long totalSeeds;        // 총 적립된 원큐씨앗
    private Long currentSeeds;      // 현재 사용 가능한 원큐씨앗
    private Long usedSeeds;         // 사용된 원큐씨앗
    private Long convertedSeeds;    // 하나머니로 전환된 원큐씨앗
    private String message;
}
