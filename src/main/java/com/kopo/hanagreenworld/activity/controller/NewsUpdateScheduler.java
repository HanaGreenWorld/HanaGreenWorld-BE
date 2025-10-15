package com.kopo.hanagreenworld.activity.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsUpdateScheduler {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updateNewsData() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();
            
            // 네이버 뉴스 수집
            String collectResponse = webClient
                .post()
                .uri("/api/eco/news/naver/")
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // RAG 시스템에 뉴스 데이터 추가
            String updateResponse = webClient
                .post()
                .uri("/api/eco/news/update-langchain/")
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
        } catch (Exception e) {
            log.error("뉴스 데이터 업데이트 실패", e);
        }
    }

    @Transactional
    public void updateNewsDataManually() {
        updateNewsData();
    }

    public String checkNewsSchedulerStatus() {
        String currentTime = java.time.LocalDateTime.now().toString();
        String message = "뉴스 업데이트 스케줄러 활성화됨! 현재 시간: " + currentTime;
        log.info(message);
        System.out.println(message);
        return message;
    }
}
