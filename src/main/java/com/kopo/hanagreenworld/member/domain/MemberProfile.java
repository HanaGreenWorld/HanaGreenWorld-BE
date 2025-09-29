package com.kopo.hanagreenworld.member.domain;

import jakarta.persistence.*;
import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_profiles")
@Getter
@NoArgsConstructor
public class MemberProfile extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    @Column
    private String nickname;

    // 레벨은 단순 enum으로 관리 (테이블 불필요)
    @Enumerated(EnumType.STRING)
    @Column(name = "eco_level", length = 20)
    private EcoLevel ecoLevel = EcoLevel.BEGINNER;

    // 현재 보유 포인트 (필수 - 실시간 조회 성능을 위해 유지)
    @Column(name = "current_points")
    private Long currentPoints = 0L;

    // 환경 관련 통계 (point_transactions와 별개)
    @Column(name = "total_carbon_saved")
    private Double totalCarbonSaved = 0.0;

    @Column(name = "total_activities_count")
    private Integer totalActivitiesCount = 0;
    
    @Column(name = "current_month_carbon_saved")
    private Double currentMonthCarbonSaved = 0.0;
    
    @Column(name = "current_month_activities_count")
    private Integer currentMonthActivitiesCount = 0;


    // 걷기 관련 컬럼 추가
    @Column(name = "walking_consent")
    private Boolean walkingConsent = false;

    @Column(name = "walking_consented_at")
    private LocalDateTime walkingConsentedAt;

    @Column(name = "walking_last_sync_at")
    private LocalDateTime walkingLastSyncAt;

    @Column(name = "walking_daily_goal_steps")
    private Integer walkingDailyGoalSteps = 10000;

    // 레벨 enum 정의
    public enum EcoLevel {
        BEGINNER("친환경 새내기", 0L, 1000L),
        INTERMEDIATE("친환경 실천가", 1000L, 5000L),
        EXPERT("친환경 전문가", 5000L, null);

        private final String displayName;
        private final Long minPoints;
        private final Long maxPoints;

        EcoLevel(String displayName, Long minPoints, Long maxPoints) {
            this.displayName = displayName;
            this.minPoints = minPoints;
            this.maxPoints = maxPoints;
        }

        public String getDisplayName() { return displayName; }
        public Long getMinPoints() { return minPoints; }
        public Long getMaxPoints() { return maxPoints; }
        public Long getRequiredPoints() { return maxPoints != null ? maxPoints : Long.MAX_VALUE; }
    }

    @Builder
    public MemberProfile(Member member, String nickname, EcoLevel ecoLevel) {
        this.member = member;
        this.nickname = nickname;
        this.ecoLevel = ecoLevel != null ? ecoLevel : EcoLevel.BEGINNER;
    }

    // 계산 메서드들
    public EcoLevel getNextLevel() {
        return switch (this.ecoLevel) {
            case BEGINNER -> EcoLevel.INTERMEDIATE;
            case INTERMEDIATE -> EcoLevel.EXPERT;
            case EXPERT -> null;
        };
    }

    public Double getProgressToNextLevel() {
        if (this.ecoLevel == EcoLevel.EXPERT) return 100.0;
        
        EcoLevel nextLevel = getNextLevel();
        if (nextLevel == null) return 100.0;
        
        long currentLevelMin = this.ecoLevel.getMinPoints();
        long nextLevelMin = nextLevel.getMinPoints();
        long totalRange = nextLevelMin - currentLevelMin;
        long userProgress = this.currentPoints - currentLevelMin;
        
        return Math.min(100.0, Math.max(0.0, (double) userProgress / totalRange * 100));
    }

    public Long getPointsToNextLevel() {
        if (this.ecoLevel == EcoLevel.EXPERT) return 0L;
        
        EcoLevel nextLevel = getNextLevel();
        if (nextLevel == null) return 0L;
        
        return Math.max(0L, nextLevel.getMinPoints() - this.currentPoints);
    }

    public void updateEcoLevel(EcoLevel ecoLevel) {
        this.ecoLevel = ecoLevel;
    }

    // 포인트 업데이트 (현재 보유만 관리, 총적립/총사용은 point_transactions에서 계산)
    public void updateCurrentPoints(Long points) {
        this.currentPoints += points;
    }

    public void updateCarbonSaved(Double carbonSaved) {
        this.totalCarbonSaved += carbonSaved;
        this.currentMonthCarbonSaved += carbonSaved;
    }

    public void incrementActivityCount() {
        this.totalActivitiesCount++;
        this.currentMonthActivitiesCount++;
    }


    public void resetCurrentMonthData() {
        this.currentMonthCarbonSaved = 0.0;
        this.currentMonthActivitiesCount = 0;
    }

    // 걷기 관련 메서드들
    public void updateWalkingConsent(Boolean consent) {
        this.walkingConsent = consent;
        if (consent) {
            this.walkingConsentedAt = LocalDateTime.now();
        } else {
            this.walkingConsentedAt = null;
        }
    }

    public void updateWalkingLastSync() {
        this.walkingLastSyncAt = LocalDateTime.now();
    }

    public void updateWalkingDailyGoal(Integer dailyGoalSteps) {
        this.walkingDailyGoalSteps = dailyGoalSteps != null ? dailyGoalSteps : 10000;
    }
}