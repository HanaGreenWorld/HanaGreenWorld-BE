package com.kopo.hanagreenworld.activity.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eco_activities")
@Getter
@NoArgsConstructor
public class EcoActivity extends DateTimeEntity {

    @Id
    @Column(name = "activity_code", length = 50, nullable = false)
    private ActivityCode code;

    public enum ActivityCode {
        DAILY_QUIZ("일일 퀴즈"),
        WALKING("걷기"),
        ELECTRONIC_RECEIPT("전자영수증");
        
        private final String displayName;
        
        ActivityCode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl; // 활동별 아이콘 이미지

    @Column(name = "icon_emoji", length = 10)
    private String iconEmoji; // 이모지 아이콘

    // 보상 정책: FIXED(고정), RANGE(범위 랜덤), FORMULA(수식 기반)
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_policy", length = 20, nullable = false)
    private RewardPolicy rewardPolicy;

    public enum RewardPolicy {
        FIXED,     // 고정 포인트 (fixedPoints)
        RANGE,     // 범위 랜덤 (minPoints~maxPoints, 필요시 distributionJson 가중치)
        FORMULA    // 수식/규칙 기반 (distributionJson 또는 별도 규칙 엔진)
    }

    // FIXED일 때 사용 (예: 퀴즈 5P)
    @Column(name = "fixed_points")
    private Integer fixedPoints;

    // RANGE일 때 사용 (예: 5~20 랜덤)
    @Column(name = "min_points")
    private Integer minPoints;

    @Column(name = "max_points")
    private Integer maxPoints;

    // FORMULA/RANGE 가중치 등 자유도 높은 정의를 위한 JSON(선택)
    // 예: {"weights":[{"points":5,"p":0.9},{"points":100,"p":0.09},{"points":1000,"p":0.01}]}
    @Column(name = "distribution_json", columnDefinition = "TEXT")
    private String distributionJson;

    // 탄소 절약률(예: 0.21 kg/km 등) – 활동량과 단위에 맞춰 계산
    @Column(name = "carbon_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal carbonRate;

    // 측정 단위 (STEP, KM, COUNT 등)
    @Column(name = "measure_unit", length = 20, nullable = false)
    private String measureUnit;

    // 일일 인정 한도(활동량 기준)
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public EcoActivity(ActivityCode code, String description,
                       RewardPolicy rewardPolicy,
                       Integer fixedPoints, Integer minPoints, Integer maxPoints, String distributionJson,
                       BigDecimal carbonRate, String measureUnit, Integer dailyLimit, Boolean isActive) {
        this.code = code;
        this.description = description;
        this.rewardPolicy = rewardPolicy;
        this.fixedPoints = fixedPoints;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.distributionJson = distributionJson;
        this.carbonRate = carbonRate;
        this.measureUnit = measureUnit;
        this.dailyLimit = dailyLimit;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }
}