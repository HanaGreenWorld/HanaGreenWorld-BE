package com.kopo.hanagreenworld.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamJoinRequest {
    @NotBlank(message = "초대코드는 필수입니다.")
    private String inviteCode;
    
    @Size(max = 100, message = "신청 메시지는 100자 이하여야 합니다.")
    private String message;
}