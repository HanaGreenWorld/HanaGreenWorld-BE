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
    private String activitiesData; // [{"label":"걷기","value":45,"color":"#10B981"}, ...]

    @Column(name = "top_activity", length = 50)
    private String topActivity; // 가장 많이 한 활동

    // 추천 상품 (JSON 형태로 저장)
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations; // [{"id":"rec1","title":"하나green세상 적금",...}, ...]

    @Builder
    public EcoReport(Member member, String reportMonth, Long totalSeeds,
                    BigDecimal totalCarbonKg, Integer totalActivities,
                    String activitiesData, String topActivity, String recommendations) {
        this.member = member;
        this.reportMonth = reportMonth;
        this.totalSeeds = totalSeeds == null ? 0L : totalSeeds;
        this.totalCarbonKg = totalCarbonKg == null ? BigDecimal.ZERO : totalCarbonKg;
        this.totalActivities = totalActivities == null ? 0 : totalActivities;
        this.activitiesData = activitiesData;
        this.topActivity = topActivity;
        this.recommendations = recommendations;
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

    public void updateRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
}