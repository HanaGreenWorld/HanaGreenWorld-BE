package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.repository.QuizRepository;
import com.kopo.hanagreenworld.activity.service.QuizGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizRepository quizRepository;
    
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void generateDailyQuiz() {
        try {
            log.info("Generating new daily quiz...");
            
            // 새 퀴즈 생성
            Quiz newQuiz = quizGeneratorService.generateEnvironmentQuiz();
            
            // DB에 저장
            quizRepository.save(newQuiz);
            
            log.info("New daily quiz generated successfully. Quiz ID: {}", newQuiz.getId());
        } catch (Exception e) {
            log.error("Failed to generate daily quiz", e);
            // 실패 시 백업 퀴즈 사용 또는 알림 발송 로직 추가 가능
        }
    }
}