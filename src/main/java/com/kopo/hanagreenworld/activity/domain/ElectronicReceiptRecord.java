package com.kopo.hanagreenworld.activity.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전자확인증 기록 엔티티
 * 하나은행에서 받은 전자확인증 정보를 저장
 */
@Entity
@Table(name = "electronic_receipt_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElectronicReceiptRecord extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "transaction_id", nullable = false, length = 100, unique = true)
    private String transactionId;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Column(name = "transaction_amount", nullable = false)
    private Long transactionAmount;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate;

    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned = 3; // 전자확인증당 3포인트

    @Builder
    public ElectronicReceiptRecord(Member member, String transactionId, String transactionType,
                                 Long transactionAmount, String branchName, LocalDateTime receiptDate) {
        this.member = member;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;
        this.branchName = branchName;
        this.receiptDate = receiptDate;
        this.pointsEarned = 3;
    }
}

