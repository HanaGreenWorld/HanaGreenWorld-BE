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
public class IntegratedFinancialProductsResponse {
    private Long customerId;
    private String customerName;
    private List<SavingsProduct> savingsProducts;
    private List<LoanProduct> loanProducts;
    private List<InvestmentProduct> investmentProducts;
    private LocalDateTime lastUpdated;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingsProduct {
        private Long productId;
        private String productName;
        private String accountNumber;
        private BigDecimal balance;
        private BigDecimal interestRate;
        private String status;
        private LocalDateTime maturityDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanProduct {
        private Long productId;
        private String productName;
        private String accountNumber;
        private BigDecimal loanAmount;
        private BigDecimal remainingAmount;
        private BigDecimal interestRate;
        private String status;
        private LocalDateTime maturityDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentProduct {
        private Long productId;
        private String productName;
        private String accountNumber;
        private BigDecimal investmentAmount;
        private BigDecimal currentValue;
        private BigDecimal returnRate;
        private String status;
    }
}
