package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedCustomerInfoRequest {

    private Long memberId;

    private Boolean customerConsent;

    private String[] targetServices;

    private String infoType;
}









