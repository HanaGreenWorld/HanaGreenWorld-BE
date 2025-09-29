package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankProductInfo {
    private String productCode;
    private String productName;
    private String productType;
    private BigDecimal amount;
    private String status;
    private String subscriptionDate;
    private String maturityDate;
}


