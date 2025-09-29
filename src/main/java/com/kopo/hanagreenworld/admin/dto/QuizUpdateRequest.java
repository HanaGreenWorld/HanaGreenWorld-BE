package com.kopo.hanagreenworld.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizUpdateRequest {
    
    @NotBlank(message = "질문은 필수입니다.")
    private String question;
    
    @NotBlank(message = "보기는 필수입니다.")
    private String options;
    
    @NotNull(message = "정답 번호는 필수입니다.")
    private Integer correctAnswer;
    
    @NotBlank(message = "해설은 필수입니다.")
    private String explanation;
    
    @NotNull(message = "포인트 보상은 필수입니다.")
    @Min(value = 1, message = "포인트 보상은 1점 이상이어야 합니다.")
    private Integer pointsReward;
}
