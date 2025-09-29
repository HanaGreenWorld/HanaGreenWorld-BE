package com.kopo.hanagreenworld.activity.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "electronic_receipt_records",
    indexes = {
        @Index(name = "idx_receipt_member_date", columnList = "member_id, activity_date"),
    }
)
@Getter
@NoArgsConstructor
public class ElectronicReceiptRecord extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 공통 필드
    @Column(name = "activity_amount", nullable = false)
    private Long activityAmount; // 전자확인증 건수 (보통 1)

    @Column(name = "carbon_saved", precision = 5, scale = 2, nullable = false)
    private BigDecimal carbonSaved; // kg

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // 전자확인증 전용 상세
    @Column(name = "transaction_id", length = 100)
    private String transactionId;  // 거래 ID

    @Column(name = "transaction_type", length = 50)
    private String transactionType;  // 입금, 지급, 만기갱신, 해지 등

    @Column(name = "transaction_amount")
    private Long transactionAmount;  // 거래 금액

    @Column(name = "branch_name", length = 100)
    private String branchName;  // 영업점명

    @Column(name = "claimed_reward_at")
    private LocalDateTime claimedRewardAt;

    @Builder
    public ElectronicReceiptRecord(Member member, Long activityAmount, BigDecimal carbonSaved, 
                                 Integer pointsAwarded, LocalDateTime activityDate,
                                 String transactionId, String transactionType, Long transactionAmount, 
                                 String branchName, LocalDateTime claimedRewardAt) {
        this.member = member;
        this.activityAmount = activityAmount;
        this.carbonSaved = carbonSaved;
        this.pointsAwarded = pointsAwarded;
        this.activityDate = activityDate == null ? LocalDateTime.now() : activityDate;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;
        this.branchName = branchName;
        this.claimedRewardAt = claimedRewardAt;
    }

    public void markClaimed(LocalDateTime when) { 
        this.claimedRewardAt = when; 
    }
}