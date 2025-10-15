package com.kopo.hanagreenworld.member.domain;

public enum MemberStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    SUSPENDED("정지"),
    WITHDRAWN("탈퇴");

    private final String description;

    MemberStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

