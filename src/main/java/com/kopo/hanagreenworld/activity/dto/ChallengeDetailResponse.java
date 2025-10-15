package com.kopo.hanagreenworld.activity.dto;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChallengeDetailResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    private String rewardPolicy;
    private Integer points;
    private Integer teamScore;
    private Boolean isTeamChallenge;
    private Boolean isLeaderOnly;
    private Boolean isActive;
    
    // 챌린지 기간 정보
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isCurrentlyActive;
    private String periodStatus; // "UPCOMING", "ACTIVE", "ENDED"
    private String periodDescription; // "2024.01.01 ~ 2024.01.31" 형태
    
    // 사용자 참여 정보
    private Boolean isParticipated;
    private String participationStatus;
    private LocalDateTime participationDate; // 실제 참여 완료 날짜 (APPROVED/REJECTED일 때만)
    private String participationMessage; // 참여 상태에 따른 메시지
    
    // 환경 임팩트 정보
    private Double carbonSaved;
    
    // UI 표시용 정보
    private String iconUrl;
    private String activity;
    private String[] aiGuide;
    private String[] process;
    private String rewardDesc;
    private String note;

    public static ChallengeDetailResponse from(Challenge challenge, Boolean isParticipated, 
                                             String participationStatus, LocalDateTime participationDate) {
        return ChallengeDetailResponse.builder()
                .id(challenge.getId())
                .code(challenge.getCode().name())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .rewardPolicy(challenge.getRewardPolicy().name())
                .points(challenge.getPoints())
                .teamScore(challenge.getTeamScore())
                .isTeamChallenge(challenge.getIsTeamChallenge())
                .isLeaderOnly(challenge.getIsLeaderOnly())
                .isActive(challenge.getIsActive())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .isCurrentlyActive(challenge.isCurrentlyActive())
                .periodStatus(getPeriodStatus(challenge))
                .periodDescription(getPeriodDescription(challenge))
                .isParticipated(isParticipated)
                .participationStatus(participationStatus)
                .participationDate(participationDate)
                .participationMessage(getParticipationMessage(participationStatus, participationDate))
                .carbonSaved(challenge.getCarbonSaved())
                .iconUrl(getIconUrl(challenge.getCode()))
                .activity(getActivity(challenge.getCode()))
                .aiGuide(getAiGuide(challenge.getCode()))
                .process(getProcess(challenge.getCode()))
                .rewardDesc(getRewardDesc(challenge))
                .note(getNote(challenge.getCode()))
                .build();
    }

    private static String getPeriodStatus(Challenge challenge) {
        if (challenge.getStartDate() == null && challenge.getEndDate() == null) {
            return "ACTIVE"; // 기간이 설정되지 않은 경우 항상 활성
        }
        
        if (!challenge.hasStarted()) {
            return "UPCOMING"; // 시작 전
        } else if (challenge.hasEnded()) {
            return "ENDED"; // 종료됨
        } else {
            return "ACTIVE"; // 진행 중
        }
    }

    private static String getPeriodDescription(Challenge challenge) {
        if (challenge.getStartDate() == null && challenge.getEndDate() == null) {
            return "상시 진행";
        }
        
        String startStr = challenge.getStartDate() != null ? 
                formatDate(challenge.getStartDate().toLocalDate()) : "미정";
        String endStr = challenge.getEndDate() != null ? 
                formatDate(challenge.getEndDate().toLocalDate()) : "미정";
        
        return startStr + "-" + endStr;
    }
    
    private static String formatDate(java.time.LocalDate date) {
        return String.format("%d.%02d.%02d", 
                date.getYear(), 
                date.getMonthValue(), 
                date.getDayOfMonth());
    }

    private static String getParticipationMessage(String status, LocalDateTime participationDate) {
        if (status == null) {
            return "아직 참여하지 않았습니다.";
        }
        
        switch (status) {
            case "PENDING":
                return "사진이 업로드되었습니다. '인증 완료' 버튼을 눌러 AI 검증을 시작하세요.";
            case "VERIFYING":
                return "AI가 사진을 검증하고 있습니다...";
            case "APPROVED":
                if (participationDate != null) {
                    return participationDate.toLocalDate() + "에 인증이 완료되었습니다!";
                }
                return "인증이 완료되었습니다!";
            case "REJECTED":
                if (participationDate != null) {
                    return participationDate.toLocalDate() + "에 인증이 거절되었습니다.";
                }
                return "인증이 거절되었습니다.";
            case "NEEDS_REVIEW":
                return "관리자 검토가 필요합니다.";
            default:
                return "참여 상태를 확인 중입니다.";
        }
    }

    // 기존 ChallengeListResponse의 메서드들을 복사
    private static String getIconUrl(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return "/icons/reusable-bag.png";
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return "/icons/plugging.png";
            case WEEKLY_STEPS:
            case DAILY_STEPS:
            case TEAM_WALKING:
                return "/icons/walking.png";
            case NO_PLASTIC:
                return "/icons/no-plastic.png";
            case TUMBLER_CHALLENGE:
                return "/icons/tumbler.png";
            case RECYCLE:
                return "/icons/recycle.png";
            default:
                return "/icons/default.png";
        }
    }

    private static String getActivity(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return "장바구니 사용";
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return "플로깅";
            case WEEKLY_STEPS:
            case DAILY_STEPS:
            case TEAM_WALKING:
                return "걷기";
            case NO_PLASTIC:
                return "일회용품 줄이기";
            case TUMBLER_CHALLENGE:
                return "텀블러 사용";
            case RECYCLE:
                return "분리수거";
            default:
                return "환경 활동";
        }
    }

    private static String[] getAiGuide(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return new String[]{"장바구니가 명확히 보이도록 촬영하세요", "쇼핑백 대신 장바구니를 사용하는 모습을 담아주세요"};
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return new String[]{"쓰레기를 주우는 모습이 명확히 보이도록 촬영하세요", "주운 쓰레기와 함께 촬영해주세요"};
            case TUMBLER_CHALLENGE:
                return new String[]{"텀블러가 명확히 보이도록 촬영하세요", "텀블러를 사용하는 모습을 담아주세요"};
            case RECYCLE:
                return new String[]{"분리수거하는 모습이 명확히 보이도록 촬영하세요", "분리수거함과 함께 촬영해주세요"};
            default:
                return new String[]{"활동하는 모습이 명확히 보이도록 촬영하세요"};
        }
    }

    private static String[] getProcess(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return new String[]{"장바구니 사용", "사진 촬영", "AI 인증", "보상 지급"};
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return new String[]{"플로깅 활동", "사진 촬영", "AI 인증", "팀 점수 부여"};
            case TUMBLER_CHALLENGE:
                return new String[]{"텀블러 사용", "사진 촬영", "AI 인증", "보상 지급"};
            case RECYCLE:
                return new String[]{"분리수거", "사진 촬영", "AI 인증", "보상 지급"};
            default:
                return new String[]{"활동 참여", "사진 촬영", "AI 인증", "보상 지급"};
        }
    }

    private static String getRewardDesc(Challenge challenge) {
        if (challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.POINTS) {
            return "+" + challenge.getPoints() + " 씨앗";
        } else {
            return "팀원 전원 +" + challenge.getTeamScore() + " 씨앗, 최다 수거 팀 추가 보상";
        }
    }

    private static String getNote(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return "난이도: 하";
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return "팀 챌린지 - 함께 참여하세요!";
            case WEEKLY_STEPS:
            case TEAM_WALKING:
                return "AI 인증보다 헬스케어 API 연동 권장";
            case DAILY_STEPS:
                return "건강과 환경을 동시에 챙기는 챌린지";
            case NO_PLASTIC:
                return "일상에서 실천하기 쉬운 챌린지";
            case TUMBLER_CHALLENGE:
                return "난이도: 하";
            case RECYCLE:
                return "물로 헹군 후 배출하면 인식률이 높아요";
            default:
                return "";
        }
    }
}
