package com.kopo.hanagreenworld.point.dto;

import com.kopo.hanagreenworld.point.domain.PointTransaction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EcoSeedTransactionResponse {
    private Long transactionId;
    private String transactionType;  // "EARN" 또는 "USE"
    private String category;
    private String categoryDisplayName;
    private String categoryImageUrl;
    private String description;
    private Integer pointsAmount;
    private Long balanceAfter;
    private LocalDateTime occurredAt;

    public static EcoSeedTransactionResponse from(PointTransaction transaction) {
        return EcoSeedTransactionResponse.builder()
                .transactionId(transaction.getId())
                .transactionType(transaction.getPointTransactionType().name())
                .category(transaction.getCategory().name())
                .categoryDisplayName(transaction.getCategory().getDisplayName())
                .categoryImageUrl(transaction.getCategory().getImageUrl())
                .description(transaction.getDescription())
                .pointsAmount(transaction.getPointsAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .occurredAt(transaction.getOccurredAt())
                .build();
    }
}
