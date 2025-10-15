package com.kopo.hanagreenworld.activity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVerificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public AiVerificationResult verifyChallengeImage(String imageUrl, String challengeTitle, String challengeCode) {
        try {
            log.info("AI 검증 시작 - URL: {}, 챌린지: {} ({})", imageUrl, challengeTitle, challengeCode);

            // 이미지 다운로드
            byte[] imageBytes = downloadImage(imageUrl);
            
            if (imageBytes == null || imageBytes.length == 0) {
                log.error("이미지 다운로드 실패: {}", imageUrl);
                return AiVerificationResult.builder()
                        .success(false)
                        .verificationResult("NEEDS_REVIEW")
                        .confidence(0.0)
                        .explanation("이미지를 다운로드할 수 없어 검증을 진행할 수 없습니다.")
                        .build();
            }

            // Multipart 요청 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 이미지 파일 추가
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "challenge_image.jpg";
                }
            };
            body.add("image", imageResource);
            body.add("challengeTitle", challengeTitle);
            body.add("challengeCode", challengeCode);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // AI 서버 호출
            String url = aiServerUrl + "/api/eco/verify-challenge-image/";
            log.info("AI 서버 호출: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                boolean success = jsonNode.path("success").asBoolean(false);
                if (!success) {
                    String errorMsg = jsonNode.path("error").asText("알 수 없는 오류");
                    log.error("AI 검증 실패: {}", errorMsg);
                    return AiVerificationResult.builder()
                            .success(false)
                            .verificationResult("NEEDS_REVIEW")
                            .confidence(0.0)
                            .explanation("AI 검증 중 오류가 발생했습니다: " + errorMsg)
                            .build();
                }

                String verificationResult = jsonNode.path("verification_result").asText("NEEDS_REVIEW");
                double confidence = jsonNode.path("confidence").asDouble(0.0);
                String explanation = jsonNode.path("explanation").asText("");
                String detectedItems = jsonNode.path("detected_items").toString();

                // 결과별 상세 로그
                if ("APPROVED".equals(verificationResult)) {
                    log.info("챌린지 인증 성공! 신뢰도 {:.1f}%로 자동 승인", confidence * 100);
                } else if ("NEEDS_REVIEW".equals(verificationResult)) {
                    log.info("관리자 검토 필요 - 신뢰도 {:.1f}%", confidence * 100);
                } else if ("REJECTED".equals(verificationResult)) {
                    log.info("챌린지 인증 실패 - 신뢰도 {:.1f}%", confidence * 100);
                }

                return AiVerificationResult.builder()
                        .success(true)
                        .verificationResult(verificationResult)
                        .confidence(confidence)
                        .explanation(explanation)
                        .detectedItems(detectedItems)
                        .build();
            } else {
                log.error("AI 서버 응답 오류: {}", response.getStatusCode());
                return AiVerificationResult.builder()
                        .success(false)
                        .verificationResult("NEEDS_REVIEW")
                        .confidence(0.0)
                        .explanation("AI 서버 응답 오류")
                        .build();
            }

        } catch (Exception e) {
            log.error("AI 검증 중 예외 발생: {}", e.getMessage(), e);
            return AiVerificationResult.builder()
                    .success(false)
                    .verificationResult("NEEDS_REVIEW")
                    .confidence(0.0)
                    .explanation("AI 검증 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            // URL에서 로컬 파일 경로 추출
            String localPath = extractLocalPath(imageUrl);
            if (localPath != null) {
                // 로컬 파일에서 직접 읽기
                Path filePath = Paths.get(localPath);
                if (Files.exists(filePath)) {
                    log.info("로컬 파일에서 이미지 읽기: {}", localPath);
                    return Files.readAllBytes(filePath);
                }
            }
            
            // 로컬 파일이 없으면 URL로 다운로드 시도
            log.info("URL에서 이미지 다운로드 시도: {}", imageUrl);
            URL url = new URL(imageUrl);
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", imageUrl, e);
            return null;
        }
    }
    

    private String extractLocalPath(String imageUrl) {
        try {
            if (imageUrl.contains("/challenge_images/")) {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/challenge_images/") + "/challenge_images/".length());
                return "challenge_images/" + fileName;
            }
            return null;
        } catch (Exception e) {
            log.warn("로컬 경로 추출 실패: {}", imageUrl);
            return null;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class AiVerificationResult {
        private boolean success;
        private String verificationResult; // APPROVED, NEEDS_REVIEW, REJECTED
        private double confidence;
        private String explanation;
        private String detectedItems;
    }
}

