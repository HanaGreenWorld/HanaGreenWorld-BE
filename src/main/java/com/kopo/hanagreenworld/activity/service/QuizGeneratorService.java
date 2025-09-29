package com.kopo.hanagreenworld.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.dto.QuizDataDto;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGeneratorService {

    private final ObjectMapper objectMapper;
    private final AIService aiService;

    public Quiz generateEnvironmentQuiz() {
        try {
            // AI 서버를 통해 퀴즈 생성
            QuizDataDto quizData = aiService.generateEnvironmentQuiz();

            // Quiz 엔티티 생성
            return Quiz.builder()
                .question(quizData.question())
                .options(objectMapper.writeValueAsString(quizData.options()))
                .correctAnswer(quizData.correctAnswer())
                .explanation(quizData.explanation())
                .pointsReward(quizData.pointsReward())
                .build();

        } catch (Exception e) {
            log.error("Failed to generate quiz using AI Server", e);
            // AI 서버 실패 시 예외 발생 (기본값 제거)
            throw new BusinessException(ErrorCode.QUIZ_GENERATION_FAILED);
        }
    }
}