package com.kopo.hanagreenworld.point.domain;

public enum PointCategory {
    // 적립
    DAILY_QUIZ("일일 퀴즈", "/assets/hana3dIcon/hanaIcon3d_3_103.png"),
    WALKING("걷기", "/assets/hana3dIcon/hanaIcon3d_123.png"),
    ELECTRONIC_RECEIPT("전자확인증", "/assets/hana3dIcon/hanaIcon3d_4_13.png"),
    ECO_CHALLENGE("에코 챌린지", "/assets/hana3dIcon/hanaIcon3d_103.png"),
    ECO_MERCHANT("친환경 가맹점", "/assets/hana3dIcon/hanaIcon3d_85.png"),

    // 사용
    HANA_MONEY_CONVERSION("하나머니 전환", "/assets/hana3dIcon/hanaIcon3d_3_15.png"),
    ENVIRONMENT_DONATION("환경 기부", "/assets/sprout.png");

    private final String displayName;
    private final String imagePath;
    
    PointCategory(String displayName, String imagePath) {
        this.displayName = displayName;
        this.imagePath = imagePath;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public String getImageUrl() {
        return imagePath; // 상대 경로만 반환
    }
    
    // 서버 URL과 함께 전체 이미지 URL 생성 (기존 호환성을 위해 유지)
    public String getImageUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return imagePath; // 상대 경로만 반환
        }
        return serverUrl + imagePath;
    }
}