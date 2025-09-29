package com.kopo.hanagreenworld.member.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "achievements")
@Getter
@NoArgsConstructor
public class Achievement extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "achievement_id")
    private Long id;

    @Column(name = "achievement_code", length = 50, nullable = false, unique = true)
    private String achievementCode; // first_step, weekly_streak, carbon_10kg 등

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "icon", length = 10)
    private String icon; // ��, 🔥, ��, 🌱 등

    @Column(name = "category", length = 50)
    private String category; // ACTIVITY, STREAK, CARBON, POINTS

    @Column(name = "required_value")
    private Long requiredValue; // 달성 조건 값

    @Column(name = "points_reward")
    private Integer pointsReward; // 달성 시 보상 포인트

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Achievement(String achievementCode, String name, String description,
                      String imageUrl, String icon, String category,
                      Long requiredValue, Integer pointsReward, Boolean isActive) {
        this.achievementCode = achievementCode;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.icon = icon;
        this.category = category;
        this.requiredValue = requiredValue;
        this.pointsReward = pointsReward;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }
}