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

    // 제출 날짜 (실제 활동 날짜)
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // 챌린지 참여 신청 날짜
    @Column(name = "participation_date")
    private LocalDateTime participationDate;

    // 사진 인증용: 이미지 URL
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 걸음수 인증용: 걸음 수
    @Column(name = "step_count")
    private Long stepCount;

    // AI 인증 결과
    @Column(name = "verification_status", length = 20, nullable = false)
    private String verificationStatus; // PENDING, APPROVED, REJECTED, NEEDS_REVIEW

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // AI 검증 관련 필드
    @Column(name = "ai_confidence")
    private Double aiConfidence; // AI 신뢰도 (0.0 ~ 1.0)

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation; // AI 판단 설명

    @Column(name = "ai_detected_items", length = 500)
    private String aiDetectedItems; // AI가 감지한 항목들 (JSON 문자열)

    // 개별 보상 포인트 (FIXED 정책일 때만 사용)
    @Column(name = "points_awarded")
    private Integer pointsAwarded;

    // 팀 점수 (TEAM_SCORE 정책일 때만 사용)
    @Column(name = "team_score_awarded")
    private Integer teamScoreAwarded;

    @Builder
    public ChallengeRecord(Challenge challenge, Member member, Long teamId,
                              LocalDateTime activityDate, LocalDateTime participationDate,
                              String imageUrl, Long stepCount, String verificationStatus,
                              Double aiConfidence, String aiExplanation, String aiDetectedItems) {
        this.challenge = challenge;
        this.member = member;
        this.teamId = teamId;
        this.activityDate = activityDate == null ? LocalDateTime.now() : activityDate;
        this.participationDate = participationDate == null ? LocalDateTime.now() : participationDate;
        this.imageUrl = imageUrl;
        this.stepCount = stepCount;
        this.verificationStatus = verificationStatus == null ? "PENDING" : verificationStatus;
        this.aiConfidence = aiConfidence;
        this.aiExplanation = aiExplanation;
        this.aiDetectedItems = aiDetectedItems;
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

    public void updateAiVerification(String status, Double confidence, String explanation, String detectedItems) {
        this.verificationStatus = status;
        this.aiConfidence = confidence;
        this.aiExplanation = explanation;
        this.aiDetectedItems = detectedItems;
    }

    public void needsReview(Double confidence, String explanation, String detectedItems) {
        this.verificationStatus = "NEEDS_REVIEW";
        this.aiConfidence = confidence;
        this.aiExplanation = explanation;
        this.aiDetectedItems = detectedItems;
    }
    
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void updateStepCount(Long stepCount) {
        this.stepCount = stepCount;
    }
}