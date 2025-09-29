package com.kopo.hanagreenworld.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamInviteCodeResponse {
    private String inviteCode;
    
    public TeamInviteCodeResponse(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}

