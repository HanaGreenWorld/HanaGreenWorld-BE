package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 고객 정보 조회 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedCustomerInfoRequest {

    /**
     * 조회할 회원 ID (하나그린월드 회원)
     */
    private Long memberId;

    /**
     * 고객 동의 여부 (필수)
     */
    private Boolean customerConsent;

    /**
     * 조회할 관계사 목록 (BANK, CARD, ALL)
     */
    private String[] targetServices;

    /**
     * 조회할 정보 타입 (BASIC, ACCOUNT, PRODUCT, ALL)
     */
    private String infoType;
}









