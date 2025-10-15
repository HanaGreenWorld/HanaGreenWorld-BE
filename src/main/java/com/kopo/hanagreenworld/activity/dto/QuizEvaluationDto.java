package com.kopo.hanagreenworld.activity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizEvaluationDto {
    
    // 기본 정보
    private String model;
    private String question;
    private List<String> options;
    private Integer correctAnswer;
    private String explanation;
    
    // 자동 평가 메트릭
    private Boolean jsonParsingSuccess;
    private Long generationTimeMs;
    private Integer tokenUsage;
    private String topicCategory;
    
    // 수동 평가 점수 (1-5점)
    private Integer questionQuality;      // 문제 품질: 명확성, 난이도 적절성
    private Integer optionsQuality;       // 선택지 품질: 오답의 그럴듯함, 함정 적절성
    private Integer explanationQuality;   // 해설 품질: 교육적 가치, 실용성
    private Integer topicRelevance;       // 주제 적합성: 환경/녹색금융 연관성
    
    // 평가 메타데이터
    private LocalDateTime evaluationTime;
    private String evaluator;
    private String notes;
    
    // 전체 점수 계산
    public Double getOverallScore() {
        if (questionQuality == null || optionsQuality == null || 
            explanationQuality == null || topicRelevance == null) {
            return null;
        }
        return (questionQuality + optionsQuality + explanationQuality + topicRelevance) / 4.0;
    }
    
    // 자동 평가 점수 계산 (JSON 파싱 성공률, 응답 시간 등)
    public Double getAutomaticScore() {
        double score = 0.0;
        
        // JSON 파싱 성공 (40점)
        if (Boolean.TRUE.equals(jsonParsingSuccess)) {
            score += 40.0;
        }
        
        // 응답 시간 점수 (30점) - 5초 이내면 만점
        if (generationTimeMs != null) {
            if (generationTimeMs <= 5000) {
                score += 30.0;
            } else if (generationTimeMs <= 10000) {
                score += 20.0;
            } else {
                score += 10.0;
            }
        }
        
        // 토큰 효율성 (30점) - 적절한 토큰 사용량
        if (tokenUsage != null) {
            if (tokenUsage <= 1000) {
                score += 30.0;
            } else if (tokenUsage <= 2000) {
                score += 20.0;
            } else {
                score += 10.0;
            }
        }
        
        return score;
    }
}
