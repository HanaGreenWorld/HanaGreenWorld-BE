package com.kopo.hanagreenworld.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.dto.QuizDataDto;
import java.time.LocalDate;
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
        return generateEnvironmentQuiz(null, null);
    }
    
    public Quiz generateEnvironmentQuiz(LocalDate quizDate, String topic) {
        try {
            QuizDataDto quizData = aiService.generateEnvironmentQuizWithOpenAI();

            // Quiz 엔티티 생성
            return Quiz.builder()
                .question(quizData.question())
                .options(objectMapper.writeValueAsString(quizData.options()))
                .correctAnswer(quizData.correctAnswer())
                .explanation(quizData.explanation())
                .pointsReward(quizData.pointsReward())
                .quizDate(quizDate)
                .topic(topic)
                .difficulty("easy")
                .build();

        } catch (Exception e) {
            log.error("Failed to generate quiz using OpenAI, falling back to Gemini", e);
            // OpenAI 실패 시 Gemini로 폴백
            try {
                QuizDataDto quizData = aiService.generateEnvironmentQuiz();
                return Quiz.builder()
                    .question(quizData.question())
                    .options(objectMapper.writeValueAsString(quizData.options()))
                    .correctAnswer(quizData.correctAnswer())
                    .explanation(quizData.explanation())
                    .pointsReward(quizData.pointsReward())
                    .quizDate(quizDate)
                    .topic(topic)
                    .difficulty("easy")
                    .build();
            } catch (Exception fallbackException) {
                log.error("Both OpenAI and Gemini failed", fallbackException);
                throw new BusinessException(ErrorCode.QUIZ_GENERATION_FAILED);
            }
        }
    }

    public Quiz generateQuizWithOpenAI() {
        try {
            // OpenAI를 통해 퀴즈 생성
            QuizDataDto quizData = aiService.generateEnvironmentQuizWithOpenAI();

            // Quiz 엔티티 생성
            return Quiz.builder()
                .question(quizData.question())
                .options(objectMapper.writeValueAsString(quizData.options()))
                .correctAnswer(quizData.correctAnswer())
                .explanation(quizData.explanation())
                .pointsReward(quizData.pointsReward())
                .build();

        } catch (Exception e) {
            log.error("Failed to generate quiz using OpenAI", e);
            // OpenAI 실패 시 예외 발생
            throw new BusinessException(ErrorCode.QUIZ_GENERATION_FAILED);
        }
    }
}