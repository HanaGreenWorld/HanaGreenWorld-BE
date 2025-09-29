package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardConsumptionSummaryResponse {
    private Long totalAmount;
    private Long totalCashback;
    private Map<String, Integer> categoryAmounts;
    private List<CardTransactionResponse> recentTransactions;
}
