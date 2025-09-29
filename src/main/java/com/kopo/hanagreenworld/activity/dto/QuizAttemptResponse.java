package com.kopo.hanagreenworld.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptResponse {
    private Long quizId;
    private Integer selectedAnswer;
    private Boolean isCorrect;
    private Integer correctAnswer;
    private String explanation;
    private Integer pointsAwarded;
}













