package com.kopo.hanagreenworld.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(500, "C_001", "서버에 문제가 발생하였습니다."),
    METHOD_NOT_ALLOWED(405, "C_002", "API는 열려있으나 메소드는 사용 불가합니다."),
    INVALID_INPUT_VALUE(400, "C_003", "적절하지 않은 요청 값입니다."),
    INVALID_TYPE_VALUE(400, "C_004", "요청 값의 타입이 잘못되었습니다."),
    ENTITY_NOT_FOUND(400, "C_005", "지정한 Entity를 찾을 수 없습니다."),

    MEMBER_NOT_FOUND(400, "ME_001", "사용자를 찾을 수 없습니다."),
    WRONG_PASSWORD(400, "ME_002","비밀번호가 일치하지 않습니다."),
    NO_REPORT_DATA(400, "ME_003", "지난 일주일 간 데이터를 분석 중입니다."),

    DUPLICATED_USERNAME(400, "AU_001", "이미 존재하는 닉네임입니다."),
    DUPLICATED_EMAIL(400, "AU_002", "이미 존재하는 E-mail입니다."),
    UNAUTHORIZED_REDIRECT_URI(400, "AU_003", "인증되지 않은 REDIRECT_URI입니다."),
    BAD_LOGIN(400, "AU_004", "잘못된 아이디 또는 패스워드입니다."),
    INVALID_PASSWORD(400, "AU_005", "잘못된 패스워드입니다."),
    INACTIVE_ACCOUNT(400, "AU_006", "비활성화된 계정입니다."),
    
    // 원큐씨앗 관련 에러
    INSUFFICIENT_ECO_SEEDS(400, "ES_001", "보유한 원큐씨앗이 부족합니다."),
    INVALID_CONVERSION_AMOUNT(400, "ES_002", "전환 가능한 원큐씨앗 범위를 벗어났습니다."),
    DAILY_CONVERSION_LIMIT_EXCEEDED(400, "ES_003", "일일 전환 한도를 초과했습니다."),

    // 퀴즈 에러
    QUIZ_GENERATION_FAILED(400, "QZ_001", "퀴즈를 생성하는 것에 실패하였습니다."),
    QUIZ_NOT_FOUND(400, "QZ_002", "요청한 퀴즈를 찾을 수 없습니다."),
    QUIZ_ALREADY_ATTEMPTED(400, "QZ_003", "오늘 이미 퀴즈에 참여하였습니다."),
    QUIZ_NOT_ATTEMPTED(400, "QZ_004", "오늘 아직 퀴즈에 참여하지 않았습니다."),

    // 걸음수 에러
    CONSENT_REQUIRED(403, "W_001", "걷기 측정에 동의하지 않았습니다."),
    ALREADY_SUBMITTED(409, "W_002", "해당 날짜에 이미 걸음수를 제출했습니다."),
    INVALID_STEPS(400, "W_003", "유효하지 않은 걸음수입니다."),

    // 챌린지 에러
    CHALLENGE_NOT_FOUND(400, "CH_001", "요청한 챌린지를 찾을 수 없습니다."),
    CHALLENGE_ALREADY_PARTICIPATED(400, "CH_002", "이미 참여한 챌린지입니다."),
    CHALLENGE_ALREADY_PARTICIPATED_TODAY(400, "CH_003", "오늘 이미 참여한 챌린지입니다."),
    INVALID_STATUS(400, "CH_004", "유효하지 않은 검증 상태입니다."),
    LEADER_ONLY_CHALLENGE(403, "CH_005", "팀장만 참여 가능한 챌린지입니다."),

    // 팀 관련 에러
    TEAM_NOT_FOUND(400, "TM_001", "팀을 찾을 수 없습니다."),
    ALREADY_IN_TEAM(400, "TM_002", "이미 팀에 속해있습니다."),
    LEADER_CANNOT_LEAVE(400, "TM_003", "팀장은 팀을 탈퇴할 수 없습니다."),
    INVALID_INVITE_CODE(400, "TM_004", "유효하지 않은 초대 코드입니다."),
    TEAM_NOT_ACTIVE(400, "TM_005", "비활성화된 팀입니다."),
    TEAM_FULL(400, "TM_006", "팀원 수가 가득 찼습니다."),
    TEAM_NAME_DUPLICATED(400, "TM_007", "이미 사용 중인 팀 이름입니다."),
    ALREADY_REQUESTED(400, "TM_008", "이미 가입 신청을 했습니다."),
    ACCESS_DENIED(403, "TM_009", "접근 권한이 없습니다."),
    JOIN_REQUEST_NOT_FOUND(400, "TM_010", "가입 신청을 찾을 수 없습니다."),
    MEMBER_NOT_IN_TEAM(400, "TM_011", "해당 멤버는 팀에 속해있지 않습니다."),
    CANNOT_KICK_LEADER(400, "TM_012", "팀장을 강퇴할 수 없습니다."),
    NOT_IN_TEAM(400, "TM_013", "팀에 속해있지 않습니다."),
    LEADER_CANNOT_LEAVE_WITH_MEMBERS(400, "TM_014", "팀원이 있는 상태에서는 팀장이 탈퇴할 수 없습니다."),
    ALREADY_TEAM_MEMBER(400, "TM_015", "이미 팀에 가입된 사용자입니다."),
    UNAUTHORIZED(401, "AU_007", "인증이 필요합니다."),

    // 채팅 관련 에러
    TEAM_CHAT_DISABLED(400, "CH_001", "팀 채팅이 비활성화되어 있습니다."),
    MESSAGE_NOT_FOUND(400, "CH_002", "메시지를 찾을 수 없습니다."),
    MESSAGE_TOO_LONG(400, "CH_003", "메시지가 너무 깁니다."),
    DAILY_MESSAGE_LIMIT_EXCEEDED(400, "CH_004", "일일 메시지 한도를 초과했습니다."),

    // 챌린지 관련 에러
    CHALLENGE_NOT_ACTIVE(400, "CL_002", "현재 활성화되지 않은 챌린지입니다."),
    CHALLENGE_NOT_STARTED(400, "CL_003", "아직 시작되지 않은 챌린지입니다."),
    CHALLENGE_ALREADY_ENDED(400, "CL_004", "이미 종료된 챌린지입니다."),
    DUPLICATE_IMAGE_SUBMISSION(400, "CL_005", "이미 사용된 이미지입니다. 다른 사진을 촬영하거나 갤러리에서 새로운 이미지를 선택해주세요."),
    ;

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.message = message;
        this.code = code;
    }

}