package com.kopo.hanagreenworld.activity.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor
public class Challenge extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_code", length = 50, nullable = false, unique = true)
    private ChallengeCode code;

    public enum ChallengeCode {
        REUSABLE_BAG("장바구니 사용"),
        REUSABLE_BAG_EXTENDED("친환경 장바구니 챌린지"),
        PLUGGING("플로깅"),
        PLUGGING_MARATHON("플로깅 마라톤"),
        TEAM_PLUGGING("팀 플로깅 대회"),
        WEEKLY_STEPS("주간 걸음수"),
        DAILY_STEPS("만보 달성 챌린지"),
        TEAM_WALKING("팀 걸음 수 경쟁"),
        NO_PLASTIC("일회용품 줄이기"),
        TUMBLER_CHALLENGE("텀블러 사용 인증"),
        RECYCLE("분리수거");
        
        private final String displayName;

        ChallengeCode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 보상 정책
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_policy", length = 20, nullable = false)
    private ChallengeRewardPolicy rewardPolicy;

    public enum ChallengeRewardPolicy {
        POINTS,           // 성공 시 포인트 (장바구니, 텀블러, 분리수거)
        TEAM_SCORE       // 팀 점수만 부여, 개별 보상 없음 (플로깅, 주간 걸음수)
    }

    // POINTS일 때 사용
 	@Column(name = "points")
	private Integer points;

    // TEAM_SCORE일 때 사용 (팀 점수)
    @Column(name = "team_score")
    private Integer teamScore;

    @Column(name = "is_team_challenge", nullable = false)
    private Boolean isTeamChallenge = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Challenge(ChallengeCode code, String title, String description,
                     ChallengeRewardPolicy rewardPolicy,
                     Integer points, Integer teamScore,
                     Boolean isTeamChallenge, Boolean isActive) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.rewardPolicy = rewardPolicy;
        this.points = points;
        this.teamScore = teamScore;
        this.isTeamChallenge = isTeamChallenge == null ? false : isTeamChallenge;
        this.isActive = isActive == null ? true : isActive;
    }

    public void deactivate() { this.isActive = false; }
}