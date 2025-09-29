package com.kopo.hanagreenworld.activity.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.kopo.hanagreenworld.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "challenge_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_challenge_member_date", 
                         columnNames = {"challenge_id", "member_id", "activity_date"})
    },
    indexes = {
        @Index(name = "idx_challenge_record_challenge", columnList = "challenge_id"),
        @Index(name = "idx_challenge_record_member", columnList = "member_id"),
        @Index(name = "idx_challenge_record_date", columnList = "activity_date")
    }
)
@Getter
@NoArgsConstructor
public class ChallengeRecord extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 팀 ID (팀 챌린지일 때만 사용)
    @Column(name = "team_id")
    private Long teamId;

    // 제출 날짜
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // 사진 인증용: 이미지 URL
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 걸음수 인증용: 걸음 수
    @Column(name = "step_count")
    private Long stepCount;

    // AI 인증 결과
    @Column(name = "verification_status", length = 20, nullable = false)
    private String verificationStatus; // PENDING, APPROVED, REJECTED

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 개별 보상 포인트 (FIXED 정책일 때만 사용)
    @Column(name = "points_awarded")
    private Integer pointsAwarded;

    // 팀 점수 (TEAM_SCORE 정책일 때만 사용)
    @Column(name = "team_score_awarded")
    private Integer teamScoreAwarded;

    @Builder
    public ChallengeRecord(Challenge challenge, Member member, Long teamId,
                              LocalDateTime activityDate, String imageUrl,
                              Long stepCount, String verificationStatus) {
        this.challenge = challenge;
        this.member = member;
        this.teamId = teamId;
        this.activityDate = activityDate == null ? LocalDateTime.now() : activityDate;
        this.imageUrl = imageUrl;
        this.stepCount = stepCount;
        this.verificationStatus = verificationStatus == null ? "PENDING" : verificationStatus;
    }

    public void approve(Integer points, Integer teamScore, LocalDateTime when) {
        this.verificationStatus = "APPROVED";
        this.verifiedAt = when;
        this.pointsAwarded = points;
        this.teamScoreAwarded = teamScore;
    }

    public void reject(LocalDateTime when) {
        this.verificationStatus = "REJECTED";
        this.verifiedAt = when;
    }
    
    public void updateVerificationStatus(String status) {
        this.verificationStatus = status;
        if ("VERIFIED".equals(status) || "APPROVED".equals(status)) {
            this.verifiedAt = LocalDateTime.now();
        }
    }
}