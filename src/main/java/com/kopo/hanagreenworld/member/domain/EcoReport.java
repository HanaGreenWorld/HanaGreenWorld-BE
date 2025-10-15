package com.kopo.hanagreenworld.member.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "eco_reports",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_month", columnNames = {"member_id", "report_month"})
    },
    indexes = {
        @Index(name = "idx_eco_report_member", columnList = "member_id"),
        @Index(name = "idx_eco_report_month", columnList = "report_month")
    }
)
@Getter
@NoArgsConstructor
public class EcoReport extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "report_month", length = 7, nullable = false)
    private String reportMonth; // YYYY-MM 형식

    // 월간 통계
    @Column(name = "total_seeds", nullable = false)
    private Long totalSeeds = 0L;

    @Column(name = "total_carbon_kg", precision = 7, scale = 2, nullable = false)
    private BigDecimal totalCarbonKg = BigDecimal.ZERO;

    @Column(name = "total_activities", nullable = false)
    private Integer totalActivities = 0;

    // 활동별 비율 (JSON 형태로 저장)
    @Column(name = "activities_data", columnDefinition = "TEXT")
    private String activitiesData;

    @Column(name = "top_activity", length = 50)
    private String topActivity;

    @Column(name = "current_level", length = 20)
    private String currentLevel;

    @Column(name = "next_level", length = 20)
    private String nextLevel;

    @Column(name = "level_progress", precision = 5, scale = 2)
    private BigDecimal levelProgress; // 다음 레벨까지 진행률 (%)

    @Column(name = "points_to_next_level")
    private Long pointsToNextLevel;

    // 금융 혜택 (JSON 형태로 저장)
    @Column(name = "financial_benefit", columnDefinition = "TEXT")
    private String financialBenefit;

    // 사용자 랭킹 정보 (JSON 형태로 저장)
    @Column(name = "user_ranking", columnDefinition = "TEXT")
    private String userRanking;

    // 환경 가치 환산 (JSON 형태로 저장)
    @Column(name = "environmental_impact", columnDefinition = "TEXT")
    private String environmentalImpact;

    // 차트 기본 표시 타입
    @Column(name = "data_view_type", length = 10)
    private String dataViewType = "COUNT"; // "COUNT" or "POINTS"

    @Builder
    public EcoReport(Member member, String reportMonth, Long totalSeeds,
                    BigDecimal totalCarbonKg, Integer totalActivities,
                    String activitiesData, String topActivity,
                    String currentLevel, String nextLevel, BigDecimal levelProgress,
                    Long pointsToNextLevel, String financialBenefit,
                    String userRanking, String environmentalImpact,
                    String dataViewType) {
        this.member = member;
        this.reportMonth = reportMonth;
        this.totalSeeds = totalSeeds == null ? 0L : totalSeeds;
        this.totalCarbonKg = totalCarbonKg == null ? BigDecimal.ZERO : totalCarbonKg;
        this.totalActivities = totalActivities == null ? 0 : totalActivities;
        this.activitiesData = activitiesData;
        this.topActivity = topActivity;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.levelProgress = levelProgress;
        this.pointsToNextLevel = pointsToNextLevel;
        this.financialBenefit = financialBenefit;
        this.userRanking = userRanking;
        this.environmentalImpact = environmentalImpact;
        this.dataViewType = dataViewType == null ? "COUNT" : dataViewType;
    }

    public void updateStats(Long seeds, BigDecimal carbonKg, Integer activities) {
        this.totalSeeds = seeds;
        this.totalCarbonKg = carbonKg;
        this.totalActivities = activities;
    }

    public void updateActivitiesData(String activitiesData) {
        this.activitiesData = activitiesData;
    }

    public void updateTopActivity(String topActivity) {
        this.topActivity = topActivity;
    }

    public void updateLevelInfo(String currentLevel, String nextLevel, BigDecimal levelProgress, Long pointsToNextLevel) {
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.levelProgress = levelProgress;
        this.pointsToNextLevel = pointsToNextLevel;
    }

    public void updateFinancialBenefit(String financialBenefit) {
        this.financialBenefit = financialBenefit;
    }

    public void updateUserRanking(String userRanking) {
        this.userRanking = userRanking;
    }

    public void updateEnvironmentalImpact(String environmentalImpact) {
        this.environmentalImpact = environmentalImpact;
    }

    public void updateDataViewType(String dataViewType) {
        this.dataViewType = dataViewType;
    }
}