package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsAccountResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private String accountNumber;
    private Long balance;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal baseRate;
    private BigDecimal preferentialRate;
    private BigDecimal finalRate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
