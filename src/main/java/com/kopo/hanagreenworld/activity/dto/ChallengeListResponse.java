package com.kopo.hanagreenworld.activity.dto;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeListResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    private String rewardPolicy;
    private Integer points;
    private Integer teamScore;
    private Boolean isTeamChallenge;
    private String iconUrl;
    private String activity;
    private String[] aiGuide;
    private String[] process;
    private String rewardDesc;
    private String note;
    private Boolean isParticipated;
    private String participationStatus;

    public static ChallengeListResponse from(Challenge challenge, Boolean isParticipated, String participationStatus) {
        return ChallengeListResponse.builder()
                .id(challenge.getId())
                .code(challenge.getCode().name())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .rewardPolicy(challenge.getRewardPolicy().name())
                .points(challenge.getPoints())
                .teamScore(challenge.getTeamScore())
                .isTeamChallenge(challenge.getIsTeamChallenge())
                .iconUrl(getIconUrl(challenge.getCode()))
                .activity(getActivity(challenge.getCode()))
                .aiGuide(getAiGuide(challenge.getCode()))
                .process(getProcess(challenge.getCode()))
                .rewardDesc(getRewardDesc(challenge))
                .note(getNote(challenge.getCode()))
                .isParticipated(isParticipated)
                .participationStatus(participationStatus)
                .build();
    }

    private static String getIconUrl(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return "/static/assets/hana3dIcon/hanaIcon3d_4_13.png";
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return "/static/assets/hana3dIcon/hanaIcon3d_4_17.png";
            case WEEKLY_STEPS:
            case DAILY_STEPS:
            case TEAM_WALKING:
                return "/static/assets/hana3dIcon/hanaIcon3d_4_33.png";
            case NO_PLASTIC:
            case TUMBLER_CHALLENGE:
                return "/static/assets/hana3dIcon/hanaIcon3d_4_31.png";
            case RECYCLE:
                return "/static/assets/hana3dIcon/hanaIcon3d_4_35.png";
            default:
                return "/static/assets/hana3dIcon/hanaIcon3d_103.png";
        }
    }

    private static String getActivity(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return "마트나 시장에서 비닐봉투 대신 장바구니 사용하기. 🛒👜";
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return "팀원들과 동네를 걸으며/뛰며 쓰레기 줍기. 🏃‍♀️🗑️";
            case WEEKLY_STEPS:
            case TEAM_WALKING:
                return "팀원들의 1주일 걸음 수를 합산해 가장 높은 팀에 보상. 👣🏆";
            case DAILY_STEPS:
                return "하루 만보 걷기로 건강과 환경을 동시에. 👣💚";
            case NO_PLASTIC:
                return "외출 시 개인 텀블러/리유저블 컵을 사용해요. ☕️🌍";
            case TUMBLER_CHALLENGE:
                return "개인 텀블러로 환경을 지키는 챌린지. ☕️♻️";
            case RECYCLE:
                return "플라스틱, 캔, 종이 등 재활용품을 깨끗이 비우고 라벨 제거 후 분리배출. ♻️";
            default:
                return "";
        }
    }

    private static String[] getAiGuide(Challenge.ChallengeCode code) {
        switch (code) {
            case REUSABLE_BAG:
            case REUSABLE_BAG_EXTENDED:
                return new String[]{
                    "계산대나 장 본 물건 옆에 본인의 장바구니를 두고 촬영해요.",
                    "AI는 천/부직포 소재의 '가방' 형태 객체를 인식해 인증해요."
                };
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return new String[]{
                    "사진 속 '쓰레기봉투' 개수와 '사람' 수를 인식하여 포인트를 차등 지급해요."
                };
            case WEEKLY_STEPS:
            case DAILY_STEPS:
            case TEAM_WALKING:
                return new String[]{};
            case NO_PLASTIC:
            case TUMBLER_CHALLENGE:
                return new String[]{
                    "카페 테이블 위 음료와 함께 본인의 텀블러가 보이도록 촬영해요.",
                    "AI가 금속/플라스틱 재사용 컵 형태를 인식해 인증해요."
                };
            case RECYCLE:
                return new String[]{
                    "분리수거함 앞에서 분류된 재활용품이 보이도록 촬영해요.",
                    "AI가 재활용품 종류(플라스틱/캔/종이 등)와 분류 상태를 인식해요."
                };
            default:
                return new String[]{};
        }
    }

    private static String[] getProcess(Challenge.ChallengeCode code) {
        switch (code) {
            case PLUGGING:
            case PLUGGING_MARATHON:
            case TEAM_PLUGGING:
                return new String[]{
                    "팀별 플로깅 날짜와 장소를 정해요.",
                    "활동 후, 모아놓은 쓰레기봉투와 팀원들이 함께 나오도록 인증샷을 찍어요."
                };
            case WEEKLY_STEPS:
            case TEAM_WALKING:
                return new String[]{
                    "스마트폰 건강 앱과 연동하여 걸음 수를 자동 집계해요.",
                    "매일 팀별 총 걸음 수와 순위를 보여줘요."
                };
            case DAILY_STEPS:
                return new String[]{
                    "스마트폰 건강 앱과 연동하여 걸음 수를 자동 집계해요.",
                    "하루 목표 만보 달성 시 포인트를 지급해요."
                };
            default:
                return new String[]{};
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
            case TUMBLER_CHALLENGE:
                return "난이도: 하";
            case RECYCLE:
                return "물로 헹군 후 배출하면 인식률이 높아요";
            default:
                return "";
        }
    }
}
