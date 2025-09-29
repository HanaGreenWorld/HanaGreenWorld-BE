package com.kopo.hanagreenworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyJoinRequestResponse {
    
    private Long requestId;
    private Long teamId;
    private String teamName;
    private String teamSlogan;
    private String inviteCode;
    private String message;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime requestDate;
    private LocalDateTime processedAt;
    private String processedBy; // 처리한 사람 이름
}
