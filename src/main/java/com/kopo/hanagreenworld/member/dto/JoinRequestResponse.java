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
public class JoinRequestResponse {
    
    private Long requestId;
    private Long userId;
    private String userName;
    private Integer userLevel;
    private LocalDateTime requestDate;
    private String message;
    private String status; // PENDING, APPROVED, REJECTED
}



