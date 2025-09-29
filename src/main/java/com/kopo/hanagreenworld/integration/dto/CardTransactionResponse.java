package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransactionResponse {
    private Long id;
    private String transactionDate;
    private String merchantName;
    private String category;
    private Long amount;
    private Long cashbackAmount;
}
