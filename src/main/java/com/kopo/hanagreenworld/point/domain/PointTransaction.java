package com.kopo.hanagreenworld.point.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_transactions")
@Getter
@NoArgsConstructor
public class PointTransaction extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private PointTransactionType pointTransactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PointCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_amount", nullable = false)
    private Integer pointsAmount;

    @Column(name = "balance_after")
    private Long balanceAfter;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    public PointTransaction(Member member, PointTransactionType pointTransactionType, PointCategory category,
                            String description, Integer pointsAmount, Long balanceAfter,
                            LocalDateTime occurredAt) {
        this.member = member;
        this.pointTransactionType = pointTransactionType;
        this.category = category;
        this.description = description;
        // USE와 CONVERT 타입일 때는 음수로 저장 (부호 통일)
        this.pointsAmount = (pointTransactionType == PointTransactionType.USE || 
                           pointTransactionType == PointTransactionType.CONVERT) ? 
                           -Math.abs(pointsAmount) : Math.abs(pointsAmount);
        this.balanceAfter = balanceAfter;
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}