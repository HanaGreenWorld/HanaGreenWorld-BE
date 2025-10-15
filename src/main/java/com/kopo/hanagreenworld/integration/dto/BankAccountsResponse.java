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
public class BankAccountsResponse {

    private List<SavingsAccountInfo> savingsAccounts;
    private List<LoanAccountInfo> loanAccounts;
    private List<InvestmentAccountInfo> investmentAccounts;
    private List<DemandDepositAccountInfo> demandDepositAccounts;
    private AccountSummary summary;
    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingsAccountInfo {
        private String accountNumber; // 마스킹된 계좌번호
        private String productName;
        private String accountType;
        private BigDecimal balance;
        private BigDecimal interestRate; // 적용금리
        private BigDecimal baseRate; // 기본금리
        private BigDecimal preferentialRate; // 우대금리
        private LocalDateTime openDate;
        private LocalDateTime maturityDate;
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanAccountInfo {
        private String accountNumber;
        private String productName;
        private String accountType;
        private BigDecimal loanAmount;
        private BigDecimal remainingAmount;
        private BigDecimal interestRate;
        private LocalDateTime openDate;
        private LocalDateTime maturityDate;
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentAccountInfo {
        private String accountNumber;
        private String productName;
        private String accountType;
        private BigDecimal investmentAmount;
        private BigDecimal currentValue;
        private BigDecimal returnRate;
        private LocalDateTime openDate;
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandDepositAccountInfo {
        private String accountNumber;
        private String accountName;
        private String accountType;
        private String accountHolderName;
        private String bankName;
        private String accountTypeDescription;
        private BigDecimal balance;
        private LocalDateTime openDate;
        private boolean isActive;
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountSummary {
        private BigDecimal totalSavingsBalance;
        private BigDecimal totalLoanBalance;
        private BigDecimal totalInvestmentValue;
        private BigDecimal totalDepositBalance;
        private int totalAccountCount;
        private String customerGrade;
    }
}
