package com.kopo.hanagreenworld.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopo.hanagreenworld.activity.dto.QuizDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final Random random = new Random();
    
    // 퀴즈 주제 목록
    private static final List<String> QUIZ_TOPICS = Arrays.asList(
        "친환경 금융",
        "녹색 금융", 
        "ESG 투자",
        "탄소중립",
        "친환경 생활",
        "재생에너지",
        "그린뉴딜",
        "친환경 정책",
        "기후변화 대응",
        "지속가능한 발전"
    );

    private String getRandomQuizTopic() {
        return QUIZ_TOPICS.get(random.nextInt(QUIZ_TOPICS.size()));
    }

    public QuizDataDto generateEnvironmentQuiz() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();

            // 랜덤 주제 선택
            String randomTopic = getRandomQuizTopic();
            log.info("Selected random quiz topic: {}", randomTopic);
            
            // 새로운 LangChain RAG API 호출
            String requestBody = String.format("{\"topic\": \"%s\", \"difficulty\": \"easy\"}", randomTopic);
            String response = webClient
                .post()
                .uri("/api/eco/quiz/daily/")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("AI Server quiz response: {}", response);
            
            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            log.info("Parsed response map: {}", responseMap);
            
            if (!(Boolean) responseMap.get("success")) {
                log.error("AI Server quiz generation failed: {}", responseMap.get("error"));
                throw new RuntimeException("AI Server quiz generation failed: " + responseMap.get("error"));
            }

            // LangChain RAG 응답에서 퀴즈 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            
            if (dataMap == null) {
                log.error("AI Server response does not contain data. Response: {}", responseMap);
                throw new RuntimeException("AI Server response does not contain data");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> quizList = (List<Map<String, Object>>) dataMap.get("quiz");
            
            if (quizList == null || quizList.isEmpty()) {
                log.error("AI Server response does not contain quiz data. Response: {}", responseMap);
                throw new RuntimeException("AI Server response does not contain quiz data");
            }
            
            // 첫 번째 퀴즈 데이터 추출
            Map<String, Object> quizMap = quizList.get(0);
            
            // 필수 필드들 null 체크
            String question = (String) quizMap.get("question");
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) quizMap.get("options");
            Integer correctAnswer = (Integer) quizMap.get("correct_answer");
            String explanation = (String) quizMap.get("explanation");
            
            if (question == null || options == null || correctAnswer == null || explanation == null) {
                log.error("Quiz data is incomplete. Question: {}, Options: {}, CorrectAnswer: {}, Explanation: {}", 
                         question, options, correctAnswer, explanation);
                throw new RuntimeException("Quiz data is incomplete");
            }
            
            return new QuizDataDto(
                question,
                options,
                correctAnswer,
                explanation,
                10 // 기본 포인트
            );

        } catch (Exception e) {
            log.error("Failed to generate quiz using AI Server", e);
            throw new RuntimeException("Failed to generate quiz using AI Server: " + e.getMessage());
        }
    }

    public String generateEcoRecommendation(Map<String, Object> userData) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();

            // AI 서버에 추천 생성 요청
            Map<String, Object> requestBody = Map.of(
                "user_data", userData
            );

            // API 호출
            String response = webClient
                .post()
                .uri("/api/eco/recommendation/")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.debug("AI Server recommendation response: {}", response);
            
            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            if (!(Boolean) responseMap.get("success")) {
                log.error("AI Server recommendation generation failed: {}", responseMap.get("error"));
                throw new RuntimeException("AI Server recommendation generation failed: " + responseMap.get("error"));
            }

            return (String) responseMap.get("recommendation");

        } catch (Exception e) {
            log.error("Failed to generate recommendation using AI Server", e);
            throw new RuntimeException("Failed to generate recommendation using AI Server: " + e.getMessage());
        }
    }

    public String generateMotivationMessage(Map<String, Object> userStats) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();

            // AI 서버에 동기부여 메시지 생성 요청
            Map<String, Object> requestBody = Map.of(
                "user_stats", userStats
            );

            // API 호출
            String response = webClient
                .post()
                .uri("/api/eco/motivation/")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.debug("AI Server motivation response: {}", response);
            
            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            if (!(Boolean) responseMap.get("success")) {
                log.error("AI Server motivation generation failed: {}", responseMap.get("error"));
                throw new RuntimeException("AI Server motivation generation failed: " + responseMap.get("error"));
            }

            return (String) responseMap.get("message");

        } catch (Exception e) {
            log.error("Failed to generate motivation message using AI Server", e);
            throw new RuntimeException("Failed to generate motivation message using AI Server: " + e.getMessage());
        }
    }

    public String generateEcoTips(String category, String userLevel) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();

            // AI 서버에 친환경 팁 생성 요청
            Map<String, Object> requestBody = Map.of(
                "category", category,
                "user_level", userLevel
            );

            // API 호출
            String response = webClient
                .post()
                .uri("/api/eco/tips/")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.debug("AI Server tips response: {}", response);
            
            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            if (!(Boolean) responseMap.get("success")) {
                log.error("AI Server tips generation failed: {}", responseMap.get("error"));
                throw new RuntimeException("AI Server tips generation failed: " + responseMap.get("error"));
            }

            return (String) responseMap.get("tips");

        } catch (Exception e) {
            log.error("Failed to generate eco tips using AI Server", e);
            throw new RuntimeException("Failed to generate eco tips using AI Server: " + e.getMessage());
        }
    }

    public QuizDataDto generateEnvironmentQuizWithOpenAI() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServerUrl).build();

            // 랜덤 주제 선택
            String randomTopic = getRandomQuizTopic();
            log.info("Selected random quiz topic for OpenAI: {}", randomTopic);
            
            // 새로운 LangChain RAG API 호출 (OpenAI 기반)
            String requestBody = String.format("{\"topic\": \"%s\", \"difficulty\": \"easy\"}", randomTopic);
            String response = webClient
                .post()
                .uri("/api/eco/quiz/daily/")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            log.info("AI Server OpenAI quiz response: {}", response);
            
            // JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            log.info("Parsed OpenAI response map: {}", responseMap);
            
            if (!(Boolean) responseMap.get("success")) {
                log.error("AI Server OpenAI quiz generation failed: {}", responseMap.get("error"));
                throw new RuntimeException("AI Server OpenAI quiz generation failed: " + responseMap.get("error"));
            }

            // LangChain RAG 응답에서 퀴즈 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            
            if (dataMap == null) {
                log.error("AI Server response does not contain data. Response: {}", responseMap);
                throw new RuntimeException("AI Server response does not contain data");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> quizList = (List<Map<String, Object>>) dataMap.get("quiz");
            
            if (quizList == null || quizList.isEmpty()) {
                log.error("AI Server response does not contain quiz data. Response: {}", responseMap);
                throw new RuntimeException("AI Server response does not contain quiz data");
            }
            
            // 첫 번째 퀴즈 데이터 추출
            Map<String, Object> quizMap = quizList.get(0);
            
            // 필수 필드들 null 체크
            String question = (String) quizMap.get("question");
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) quizMap.get("options");
            Integer correctAnswer = (Integer) quizMap.get("correct_answer");
            String explanation = (String) quizMap.get("explanation");
            
            if (question == null || options == null || correctAnswer == null || explanation == null) {
                log.error("OpenAI Quiz data is incomplete. Question: {}, Options: {}, CorrectAnswer: {}, Explanation: {}", 
                         question, options, correctAnswer, explanation);
                throw new RuntimeException("OpenAI Quiz data is incomplete");
            }
            
            return new QuizDataDto(
                question,
                options,
                correctAnswer,
                explanation,
                10 // 기본 포인트
            );

        } catch (Exception e) {
            log.error("Failed to generate quiz using OpenAI", e);
            throw new RuntimeException("Failed to generate quiz using OpenAI: " + e.getMessage());
        }
    }
}

