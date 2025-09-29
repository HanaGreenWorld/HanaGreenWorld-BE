package com.kopo.hanagreenworld.point.domain;

public enum PointCategory {
    // 적립
    DAILY_QUIZ("일일 퀴즈", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_3_103.png"),
    WALKING("걷기", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_123.png"),
    ELECTRONIC_RECEIPT("전자확인증", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_4_13.png"),
    ECO_CHALLENGE("에코 챌린지", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_103.png"),
    ECO_MERCHANT("친환경 가맹점", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_85.png"),

    // 사용
    HANA_MONEY_CONVERSION("하나머니 전환", "http://localhost:8080/assets/hana3dIcon/hanaIcon3d_3_15.png"),
    ENVIRONMENT_DONATION("환경 기부", "http://localhost:8080/assets/sprout.png");

    private final String displayName;
    private final String imageUrl;
    
    PointCategory(String displayName, String imageUrl) {
        this.displayName = displayName;
        this.imageUrl = imageUrl;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    public String getImageUrl() {
        return imageUrl;
    }
}