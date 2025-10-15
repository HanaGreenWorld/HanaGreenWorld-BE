package com.kopo.hanagreenworld.merchant.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CardTransactionCreatedEvent {
    
    private final Long transactionId;
    private final Long userId;
    private final String merchantName;
    private final String businessNumber;
    private final Long amount;
    private final LocalDateTime transactionDate;
    private final String category;
    private final String merchantCategory;
    
    public static CardTransactionCreatedEvent of(Long transactionId, Long userId, String merchantName, 
                                               String businessNumber, Long amount, LocalDateTime transactionDate,
                                               String category, String merchantCategory) {
        return new CardTransactionCreatedEvent(
            transactionId, userId, merchantName, businessNumber, 
            amount, transactionDate, category, merchantCategory
        );
    }
}
