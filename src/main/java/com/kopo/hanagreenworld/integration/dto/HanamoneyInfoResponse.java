package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HanamoneyInfoResponse {

    private HanamoneyInfo hanamoneyInfo;
    private List<TransactionInfo> recentTransactions;
    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HanamoneyInfo {
        private String membershipId;
        private BigDecimal currentBalance;
        private BigDecimal totalEarned;
        private BigDecimal totalSpent;
        private String membershipLevel;
        private boolean isActive;
        private LocalDateTime joinDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {
        private String transactionType;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private LocalDateTime transactionDate;
    }
}









