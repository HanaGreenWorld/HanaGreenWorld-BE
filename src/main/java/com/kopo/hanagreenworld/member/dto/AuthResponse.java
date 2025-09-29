package com.kopo.hanagreenworld.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long memberId;
    private String email;
    private String name;
    private String message;
}