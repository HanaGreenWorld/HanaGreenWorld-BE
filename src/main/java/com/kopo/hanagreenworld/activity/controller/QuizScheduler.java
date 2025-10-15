package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.repository.QuizRepository;
import com.kopo.hanagreenworld.activity.service.QuizGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {

    private final QuizGeneratorService quizGeneratorService;
    private final QuizRepository quizRepository;
    
    @Scheduled(cron = "0 50 23 * * *")
    @Transactional
    public void generateDailyQuiz() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            
            // 내일 퀴즈가 이미 존재하는지 확인
            if (quizRepository.existsByQuizDate(tomorrow)) {
                log.info("Daily quiz for {} already exists, skipping generation", tomorrow);
                return;
            }
            
            log.info("Generating new daily quiz for {}", tomorrow);
            
            // 랜덤 주제 선택
            String[] topics = {
                "친환경 금융", "녹색 금융", "ESG 투자", "탄소중립", "친환경 생활",
                "재생에너지", "그린뉴딜", "친환경 정책", "기후변화 대응", "지속가능한 발전"
            };
            String randomTopic = topics[(int) (Math.random() * topics.length)];
            log.info("선택된 랜덤 주제: {}", randomTopic);
            
            // 새 퀴즈 생성 (날짜와 주제 정보 포함)
            Quiz newQuiz = quizGeneratorService.generateEnvironmentQuiz(tomorrow, randomTopic);
            
            Quiz savedQuiz = quizRepository.save(newQuiz);
            
            log.info("New daily quiz generated successfully for {}. Quiz ID: {}", 
                    tomorrow, savedQuiz.getId());
        } catch (Exception e) {
            log.error("Failed to generate daily quiz", e);
            // 실패 시 백업 퀴즈 사용 또는 알림 발송 로직 추가 가능
        }
    }

    @Transactional
    public void generateDailyQuizManually() {
        generateDailyQuiz();
    }

    public String checkSchedulerStatus() {
        String currentTime = java.time.LocalDateTime.now().toString();
        String message = "스케줄러 활성화됨! 현재 시간: " + currentTime;
        log.info(message);
        System.out.println(message);
        return message;
    }
}