package com.kopo.hanagreenworld.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoReportResponse {

    @JsonProperty("reportId")
    private Long reportId;

    @JsonProperty("reportMonth")
    private String reportMonth;

    @JsonProperty("summary")
    private Summary summary;

    @JsonProperty("statistics")
    private Statistics statistics;

    @JsonProperty("activities")
    private List<Activity> activities;

    @JsonProperty("financialBenefit")
    private FinancialBenefit financialBenefit;

    @JsonProperty("ranking")
    private Ranking ranking;

    @JsonProperty("environmentalImpact")
    private EnvironmentalImpact environmentalImpact;

    @JsonProperty("recommendations")
    private List<Recommendation> recommendations;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        @JsonProperty("currentLevel")
        private String currentLevel;

        @JsonProperty("levelProgress")
        private Double levelProgress;

        @JsonProperty("pointsToNextLevel")
        private Long pointsToNextLevel;

        @JsonProperty("topActivity")
        private String topActivity;

        @JsonProperty("topActivityMessage")
        private String topActivityMessage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        @JsonProperty("totalSeeds")
        private Long totalSeeds;

        @JsonProperty("totalActivities")
        private Integer totalActivities;

        @JsonProperty("totalCarbonKg")
        private Double totalCarbonKg;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Activity {
        @JsonProperty("label")
        private String label;

        @JsonProperty("count")
        private Long count;

        @JsonProperty("points")
        private Long points;

        @JsonProperty("countPercentage")
        private Integer countPercentage;

        @JsonProperty("pointsPercentage")
        private Integer pointsPercentage;

        @JsonProperty("color")
        private String color;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialBenefit {
        @JsonProperty("savingsInterest")
        private Integer savingsInterest;

        @JsonProperty("cardDiscount")
        private Integer cardDiscount;

        @JsonProperty("loanBenefit")
        private Integer loanBenefit;

        @JsonProperty("total")
        private Integer total;

        @JsonProperty("nextLevelBenefit")
        private Integer nextLevelBenefit;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Ranking {
        @JsonProperty("percentile")
        private Integer percentile;

        @JsonProperty("totalUsers")
        private Long totalUsers;

        @JsonProperty("rank")
        private Long rank;

        @JsonProperty("userPoints")
        private Long userPoints;

        @JsonProperty("averagePoints")
        private Long averagePoints;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnvironmentalImpact {
        @JsonProperty("carbonKg")
        private Double carbonKg;

        @JsonProperty("trees")
        private Double trees;

        @JsonProperty("waterLiters")
        private Double waterLiters;

        @JsonProperty("plasticBags")
        private Double plasticBags;

        @JsonProperty("energyKwh")
        private Double energyKwh;

        @JsonProperty("carKm")
        private Double carKm;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recommendation {
        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;

        @JsonProperty("image")
        private String image;

        @JsonProperty("description")
        private String description;

        @JsonProperty("benefit")
        private String benefit;
    }
}

