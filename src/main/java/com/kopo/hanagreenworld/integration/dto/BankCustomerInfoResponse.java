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
public class BankCustomerInfoResponse {
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String customerGrade;
    private String status;
    private LocalDateTime joinDate;
    private List<AccountInfo> accounts;
    private List<ProductInfo> products;
    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private String accountNumber;
        private String accountType;
        private String accountName;
        private BigDecimal balance;
        private String status;
        private LocalDateTime openDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private Long productId;
        private String productName;
        private String productType;
        private BigDecimal amount;
        private String status;
        private LocalDateTime subscriptionDate;
    }
}
