package com.kopo.hanagreenworld.activity.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "is_leader_only", nullable = false)
    private Boolean isLeaderOnly = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 챌린지 기간 관리
    @Column(name = "start_date")
    private LocalDateTime startDate; // 챌린지 시작 날짜

    @Column(name = "end_date")
    private LocalDateTime endDate; // 챌린지 종료 날짜

    // 환경 임팩트 관련 필드
    @Column(name = "carbon_saved")
    private Double carbonSaved; // 탄소절약량 (kg)

    @Builder
    public Challenge(ChallengeCode code, String title, String description,
                     ChallengeRewardPolicy rewardPolicy,
                     Integer points, Integer teamScore,
                     Boolean isTeamChallenge, Boolean isLeaderOnly, Boolean isActive,
                     LocalDateTime startDate, LocalDateTime endDate,
                     Double carbonSaved) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.rewardPolicy = rewardPolicy;
        this.points = points;
        this.teamScore = teamScore;
        this.isTeamChallenge = isTeamChallenge == null ? false : isTeamChallenge;
        this.isLeaderOnly = isLeaderOnly == null ? false : isLeaderOnly;
        this.isActive = isActive == null ? true : isActive;
        this.startDate = startDate;
        this.endDate = endDate;
        this.carbonSaved = carbonSaved;
    }

    public void deactivate() { this.isActive = false; }

    public boolean isLeaderOnlyChallenge() {
        return this.isLeaderOnly;
    }

    public boolean isTeamChallenge() {
        return this.isTeamChallenge;
    }

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        
        // 시작 날짜가 설정되지 않은 경우 항상 활성
        if (startDate == null && endDate == null) {
            return true;
        }
        
        // 시작 날짜만 설정된 경우
        if (startDate != null && endDate == null) {
            return now.isAfter(startDate) || now.isEqual(startDate);
        }
        
        // 종료 날짜만 설정된 경우
        if (startDate == null && endDate != null) {
            return now.isBefore(endDate) || now.isEqual(endDate);
        }
        
        // 시작 날짜와 종료 날짜가 모두 설정된 경우
        return (now.isAfter(startDate) || now.isEqual(startDate)) && 
               (now.isBefore(endDate) || now.isEqual(endDate));
    }

    public boolean hasStarted() {
        if (startDate == null) {
            return true; // 시작 날짜가 설정되지 않은 경우 항상 시작된 것으로 간주
        }
        return LocalDateTime.now().isAfter(startDate) || LocalDateTime.now().isEqual(startDate);
    }

    public boolean hasEnded() {
        if (endDate == null) {
            return false; // 종료 날짜가 설정되지 않은 경우 종료되지 않은 것으로 간주
        }
        return LocalDateTime.now().isAfter(endDate);
    }

    public void validateAndUpdateActiveStatus() {
        boolean shouldBeActive = isCurrentlyActive();
        if (this.isActive != shouldBeActive) {
            this.isActive = shouldBeActive;
        }
    }
}