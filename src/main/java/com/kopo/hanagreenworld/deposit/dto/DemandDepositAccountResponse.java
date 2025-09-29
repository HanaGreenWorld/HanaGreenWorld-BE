package com.kopo.hanagreenworld.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandDepositAccountResponse {

    private Long id;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String accountType;
    private String accountTypeDescription;
    private Long balance;
    private Long availableBalance;
    private LocalDate openDate;
    private LocalDate maturityDate;
    private BigDecimal baseInterestRate;
    private String status;
    private String statusDescription;
    private Boolean isActive;
    private LocalDateTime lastTransactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
