package com.kopo.hanagreenworld.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAttemptRequest {
    
    @NotNull(message = "선택한 답변은 필수입니다.")
    private Integer selectedAnswer;
}











