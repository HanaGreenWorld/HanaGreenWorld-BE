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

    @Enumerated(EnumType.STRING)
    @Column(name = "eco_level", length = 20)
    private EcoLevel ecoLevel = EcoLevel.BEGINNER;

    @Column(name = "current_points")
    private Long currentPoints = 0L;

    @Column(name = "total_carbon_saved")
    private Double totalCarbonSaved = 0.0;

    @Column(name = "total_activities_count")
    private Integer totalActivitiesCount = 0;
    
    @Column(name = "current_month_carbon_saved")
    private Double currentMonthCarbonSaved = 0.0;
    
    @Column(name = "current_month_activities_count")
    private Integer currentMonthActivitiesCount = 0;

    @Column(name = "current_month_points")
    private Long currentMonthPoints = 0L;

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
        BEGINNER("친환경 새내기", 0L, 5000L),
        INTERMEDIATE("친환경 실천가", 5000L, 10000L),
        EXPERT("친환경 전문가", 10000L, null);

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
        public Long getRequiredPoints() {
            return maxPoints != null ? maxPoints : 0L; 
        }

        public int getLevelNumber() {
            return switch (this) {
                case BEGINNER -> 1;
                case INTERMEDIATE -> 2;
                case EXPERT -> 3;
            };
        }

        public String getFormattedDisplayName() {
            return String.format("Lv%d. %s", getLevelNumber(), displayName);
        }
    }

    @Builder
    public MemberProfile(Member member, String nickname, EcoLevel ecoLevel) {
        this.member = member;
        this.nickname = nickname;
        this.ecoLevel = ecoLevel != null ? ecoLevel : EcoLevel.BEGINNER;
    }

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

    public void updateCurrentPoints(Long points) {
        this.currentPoints += points;
    }

    public void updateCurrentMonthPoints(Long points) {
        this.currentMonthPoints += points;
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
        this.currentMonthPoints = 0L;
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