package com.kopo.hanagreenworld.activity.dto;

import com.kopo.hanagreenworld.activity.domain.ElectronicReceiptRecord;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전자확인증 기록 응답 DTO
 */
@Getter
@NoArgsConstructor
public class ElectronicReceiptRecordResponse {
    
    private Long recordId;
    private String transactionId;
    private String transactionType;
    private Long transactionAmount;
    private String branchName;
    private LocalDateTime receiptDate;
    private Integer pointsEarned;
    private LocalDateTime createdAt;

    @Builder
    public ElectronicReceiptRecordResponse(Long recordId, String transactionId, String transactionType,
                                        Long transactionAmount, String branchName, LocalDateTime receiptDate,
                                        Integer pointsEarned, LocalDateTime createdAt) {
        this.recordId = recordId;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;
        this.branchName = branchName;
        this.receiptDate = receiptDate;
        this.pointsEarned = pointsEarned;
        this.createdAt = createdAt;
    }

    public static ElectronicReceiptRecordResponse from(ElectronicReceiptRecord record) {
        return ElectronicReceiptRecordResponse.builder()
            .recordId(record.getRecordId())
            .transactionId(record.getTransactionId())
            .transactionType(record.getTransactionType())
            .transactionAmount(record.getTransactionAmount())
            .branchName(record.getBranchName())
            .receiptDate(record.getReceiptDate())
            .pointsEarned(record.getPointsEarned())
            .createdAt(record.getCreatedAt())
            .build();
    }
}

