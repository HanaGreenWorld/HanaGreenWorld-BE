package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.dto.EnvironmentalImpactResponse;
import com.kopo.hanagreenworld.activity.repository.WalkingRecordRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.activity.repository.QuizRecordRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 환경 임팩트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentalImpactService {
    
    private final ObjectMapper objectMapper;

    private final WalkingRecordRepository walkingRecordRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final QuizRecordRepository quizRecordRepository;
    private final MemberProfileRepository memberProfileRepository;


    public EnvironmentalImpactResponse getEnvironmentalImpact(Long userId) {
        try {
            log.info("사용자 {}의 전체 환경 임팩트 조회 시작", userId);
            
            // 걷기 활동
            BigDecimal walkingCarbonSaved = calculateWalkingCarbonSaved(userId);
            
            // 챌린지 참여로
            Map<String, BigDecimal> challengeImpacts = calculateChallengeImpacts(userId);
            
            // 퀴즈 참여
            Map<String, BigDecimal> quizImpacts = calculateQuizImpacts(userId);

            Double totalCarbonSaved = memberProfileRepository.findByMember_MemberId(userId)
                .map(profile -> profile.getTotalCarbonSaved())
                .orElse(0.0);
            
            Double currentMonthCarbonSaved = memberProfileRepository.findByMember_MemberId(userId)
                .map(profile -> profile.getCurrentMonthCarbonSaved())
                .orElse(0.0);
            

            String environmentalGrade = calculateEnvironmentalGrade(BigDecimal.valueOf(totalCarbonSaved));
            Integer environmentalScore = calculateEnvironmentalScore(BigDecimal.valueOf(totalCarbonSaved));
            String impactLevel = calculateImpactLevel(BigDecimal.valueOf(totalCarbonSaved));

            String impactDescription = generateImpactDescription(BigDecimal.valueOf(totalCarbonSaved), environmentalGrade);

            List<Map<String, Object>> categoryImpacts = new ArrayList<>();
            Map<String, Object> carbonImpact = new HashMap<>();
            carbonImpact.put("category", "탄소 절약");
            carbonImpact.put("carbonSaved", totalCarbonSaved);
            carbonImpact.put("description", "전체 탄소 절약량");
            categoryImpacts.add(carbonImpact);

            Integer ranking = calculateRanking(BigDecimal.valueOf(totalCarbonSaved));
            String rankingDescription = generateRankingDescription(ranking);

            List<String> achievements = generateAchievements(BigDecimal.valueOf(totalCarbonSaved));

            Map<String, Object> goals = generateGoals(BigDecimal.valueOf(totalCarbonSaved), environmentalGrade);

            List<String> recommendations = generateRecommendations(BigDecimal.valueOf(totalCarbonSaved), environmentalGrade);
            
            EnvironmentalImpactResponse response = EnvironmentalImpactResponse.builder()
                    .totalCarbonSaved(BigDecimal.valueOf(totalCarbonSaved))
                    .monthlyCarbonSaved(BigDecimal.valueOf(currentMonthCarbonSaved))
                    .environmentalGrade(environmentalGrade)
                    .environmentalScore(environmentalScore)
                    .impactLevel(impactLevel)
                    .impactDescription(impactDescription)
                    .impactIcon(getImpactIcon(impactLevel))
                    .impactColor(getImpactColor(impactLevel))
                    .categoryImpacts(categoryImpacts)
                    .ranking(ranking)
                    .rankingDescription(rankingDescription)
                    .achievements(achievements)
                    .goals(goals)
                    .recommendations(recommendations)
                    .analysisDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .build();

            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("환경 임팩트 조회에 실패했습니다: " + e.getMessage());
        }
    }

    public EnvironmentalImpactResponse getMonthlyEnvironmentalImpact(Long userId) {
        try {

            Double currentMonthCarbonSaved = memberProfileRepository.findByMember_MemberId(userId)
                .map(profile -> profile.getCurrentMonthCarbonSaved())
                .orElse(0.0);

            // 이번 달 총 환경 임팩트 계산 (탄소만)
            BigDecimal monthlyCarbonSaved = BigDecimal.valueOf(currentMonthCarbonSaved);
            BigDecimal monthlyWaterSaved = BigDecimal.ZERO;
            BigDecimal monthlyEnergySaved = BigDecimal.ZERO;
            BigDecimal monthlyRecycled = BigDecimal.ZERO;

            String monthlyEnvironmentalGrade = calculateEnvironmentalGrade(monthlyCarbonSaved);
            Integer monthlyEnvironmentalScore = calculateEnvironmentalScore(monthlyCarbonSaved);
            String monthlyImpactLevel = calculateImpactLevel(monthlyCarbonSaved);

            String monthlyImpactDescription = generateImpactDescription(monthlyCarbonSaved, monthlyEnvironmentalGrade);

            List<Map<String, Object>> monthlyCategoryImpacts = new ArrayList<>();
            Map<String, Object> carbonImpact = new HashMap<>();
            carbonImpact.put("category", "탄소 절약");
            carbonImpact.put("carbonSaved", monthlyCarbonSaved);
            carbonImpact.put("description", "이번 달 탄소 절약량");
            monthlyCategoryImpacts.add(carbonImpact);

            List<Map<String, Object>> monthlyImpactTrends = new ArrayList<>();

            Integer monthlyRanking = 1;
            String monthlyRankingDescription = "이번 달 환경 임팩트";

            List<String> monthlyAchievements = new ArrayList<>();
            monthlyAchievements.add("이번 달 탄소 절약: " + monthlyCarbonSaved + "kg");

            Map<String, Object> monthlyGoals = new HashMap<>();
            monthlyGoals.put("nextGrade", "다음 등급");
            monthlyGoals.put("remainingCarbon", 10.0);
            monthlyGoals.put("progressPercentage", 50);
            monthlyGoals.put("description", "다음 등급까지 10kg의 탄소를 더 절약하세요!");

            List<String> monthlyRecommendations = new ArrayList<>();
            monthlyRecommendations.add("걷기 챌린지에 참여해보세요!");
            monthlyRecommendations.add("친환경 퀴즈를 풀어보세요!");
            
            EnvironmentalImpactResponse response = EnvironmentalImpactResponse.builder()
                    .totalCarbonSaved(monthlyCarbonSaved)
                    .monthlyCarbonSaved(monthlyCarbonSaved)
                    .totalWaterSaved(monthlyWaterSaved)
                    .monthlyWaterSaved(monthlyWaterSaved)
                    .totalEnergySaved(monthlyEnergySaved)
                    .monthlyEnergySaved(monthlyEnergySaved)
                    .totalRecycled(monthlyRecycled)
                    .monthlyRecycled(monthlyRecycled)
                    .environmentalGrade(monthlyEnvironmentalGrade)
                    .environmentalScore(monthlyEnvironmentalScore)
                    .impactLevel(monthlyImpactLevel)
                    .impactDescription(monthlyImpactDescription)
                    .impactIcon(getImpactIcon(monthlyImpactLevel))
                    .impactColor(getImpactColor(monthlyImpactLevel))
                    .categoryImpacts(monthlyCategoryImpacts)
                    .impactTrends(monthlyImpactTrends)
                    .ranking(monthlyRanking)
                    .rankingDescription(monthlyRankingDescription)
                    .achievements(monthlyAchievements)
                    .goals(monthlyGoals)
                    .recommendations(monthlyRecommendations)
                    .analysisDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .build();
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("월간 환경 임팩트 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private BigDecimal calculateWalkingCarbonSaved(Long userId) {
        try {
            // 걷기 기록에서 총 탄소 절약량 조회
            BigDecimal totalCarbonSaved = walkingRecordRepository.findTotalCarbonSavedByMemberId(userId);
            return totalCarbonSaved != null ? totalCarbonSaved : BigDecimal.valueOf(15.5); // 기본값
        } catch (Exception e) {
            log.error("걷기 활동 탄소 절약량 계산 실패: userId={}, error={}", userId, e.getMessage());
            return BigDecimal.valueOf(15.5); // 기본값
        }
    }

    private Map<String, BigDecimal> calculateChallengeImpacts(Long userId) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 챌린지 참여 기록에서 환경 임팩트 계산
            Map<String, BigDecimal> challengeImpacts = calculateChallengeImpactsFromRecords(userId);
            impacts.putAll(challengeImpacts);
        } catch (Exception e) {
            log.error("챌린지 환경 임팩트 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
            impacts.put("waterSaved", BigDecimal.ZERO);
            impacts.put("energySaved", BigDecimal.ZERO);
            impacts.put("recycled", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    private Map<String, BigDecimal> calculateQuizImpacts(Long userId) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 퀴즈 참여 기록에서 환경 임팩트 계산
            // 실제 구현에서는 퀴즈 타입별로 환경 임팩트를 계산해야 함
            impacts.put("carbonSaved", BigDecimal.valueOf(3.2)); // 예시 값
            impacts.put("waterSaved", BigDecimal.valueOf(25.0)); // 예시 값
            impacts.put("energySaved", BigDecimal.valueOf(5.1)); // 예시 값
            impacts.put("recycled", BigDecimal.valueOf(2.3)); // 예시 값
        } catch (Exception e) {
            log.error("퀴즈 환경 임팩트 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
            impacts.put("waterSaved", BigDecimal.ZERO);
            impacts.put("energySaved", BigDecimal.ZERO);
            impacts.put("recycled", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    private BigDecimal calculateMonthlyWalkingCarbonSaved(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            BigDecimal monthlyCarbonSaved = walkingRecordRepository.findTotalCarbonSavedByMemberIdAndDateRange(
                    userId, startDateTime, endDateTime);
            return monthlyCarbonSaved != null ? monthlyCarbonSaved : BigDecimal.valueOf(8.2); // 기본값
        } catch (Exception e) {
            log.error("월간 걷기 활동 탄소 절약량 계산 실패: userId={}, error={}", userId, e.getMessage());
            return BigDecimal.valueOf(8.2); // 기본값
        }
    }

    private Map<String, BigDecimal> calculateMonthlyChallengeImpacts(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 실제 월간 챌린지 기록에서 환경 임팩트 계산
            Map<String, BigDecimal> challengeImpacts = calculateChallengeImpactsFromRecords(userId, startDate, endDate);
            impacts.putAll(challengeImpacts);
        } catch (Exception e) {
            log.error("월간 챌린지 환경 임팩트 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    private Map<String, BigDecimal> calculateMonthlyQuizImpacts(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 월간 퀴즈 참여 기록에서 환경 임팩트 계산
            // 실제 구현에서는 퀴즈 타입별로 환경 임팩트를 계산해야 함
            impacts.put("carbonSaved", BigDecimal.valueOf(1.8)); // 예시 값
            impacts.put("waterSaved", BigDecimal.valueOf(15.0)); // 예시 값
            impacts.put("energySaved", BigDecimal.valueOf(3.2)); // 예시 값
            impacts.put("recycled", BigDecimal.valueOf(1.2)); // 예시 값
        } catch (Exception e) {
            log.error("월간 퀴즈 환경 임팩트 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
            impacts.put("waterSaved", BigDecimal.ZERO);
            impacts.put("energySaved", BigDecimal.ZERO);
            impacts.put("recycled", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    private String calculateEnvironmentalGrade(BigDecimal carbonSaved) {
        if (carbonSaved.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "S급";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "A급";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return "B급";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "C급";
        } else {
            return "D급";
        }
    }

    private Integer calculateEnvironmentalScore(BigDecimal carbonSaved) {
        // 탄소 절약량을 기반으로 점수 계산 (최대 100점)
        int score = carbonSaved.multiply(BigDecimal.valueOf(2)).intValue();
        return Math.min(score, 100);
    }

    private String calculateImpactLevel(BigDecimal carbonSaved) {
        if (carbonSaved.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "환경 지킴이";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "환경 실천가";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return "환경 관심자";
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "환경 초보자";
        } else {
            return "환경 새내기";
        }
    }

    private String generateImpactDescription(BigDecimal carbonSaved, String grade) {
        return String.format("당신은 %s 환경 보호자입니다! 지금까지 %.1fkg의 탄소를 절약했습니다.", grade, carbonSaved);
    }

    private List<Map<String, Object>> generateCategoryImpacts(BigDecimal walkingCarbon, 
                                                             Map<String, BigDecimal> challengeImpacts, 
                                                             Map<String, BigDecimal> quizImpacts) {
        List<Map<String, Object>> categoryImpacts = new ArrayList<>();
        
        // 걷기 활동
        Map<String, Object> walkingImpact = new HashMap<>();
        walkingImpact.put("category", "걷기 활동");
        walkingImpact.put("carbonSaved", walkingCarbon);
        walkingImpact.put("description", "걷기 활동으로 절약한 탄소");
        categoryImpacts.add(walkingImpact);
        
        // 챌린지 활동
        Map<String, Object> challengeImpact = new HashMap<>();
        challengeImpact.put("category", "챌린지 활동");
        challengeImpact.put("carbonSaved", challengeImpacts.getOrDefault("carbonSaved", BigDecimal.ZERO));
        challengeImpact.put("description", "챌린지 참여로 절약한 탄소");
        categoryImpacts.add(challengeImpact);
        
        // 퀴즈 활동
        Map<String, Object> quizImpact = new HashMap<>();
        quizImpact.put("category", "퀴즈 활동");
        quizImpact.put("carbonSaved", quizImpacts.getOrDefault("carbonSaved", BigDecimal.ZERO));
        quizImpact.put("description", "퀴즈 참여로 절약한 탄소");
        categoryImpacts.add(quizImpact);
        
        return categoryImpacts;
    }


    private Integer calculateRanking(BigDecimal carbonSaved) {
        // 실제로는 전체 사용자와 비교하여 랭킹을 계산해야 함
        if (carbonSaved.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return 1;
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return 5;
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return 15;
        } else if (carbonSaved.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return 30;
        } else {
            return 50;
        }
    }

    private String generateRankingDescription(Integer ranking) {
        if (ranking <= 5) {
            return "상위 5% 환경 보호자입니다!";
        } else if (ranking <= 15) {
            return "상위 15% 환경 보호자입니다!";
        } else if (ranking <= 30) {
            return "상위 30% 환경 보호자입니다!";
        } else {
            return "환경 보호에 참여하고 있습니다!";
        }
    }

    private List<String> generateAchievements(BigDecimal carbonSaved) {
        List<String> achievements = new ArrayList<>();
        
        if (carbonSaved.compareTo(BigDecimal.valueOf(50)) >= 0) {
            achievements.add("탄소 절약 마스터");
        }
        if (carbonSaved.compareTo(BigDecimal.valueOf(100)) >= 0) {
            achievements.add("환경 보호 영웅");
        }
        
        return achievements;
    }

    private Map<String, Object> generateGoals(BigDecimal currentCarbon, String currentGrade) {
        Map<String, Object> goals = new HashMap<>();
        
        // 다음 등급까지 필요한 탄소 절약량 계산
        BigDecimal nextGradeThreshold = getNextGradeThreshold(currentGrade);
        BigDecimal remainingCarbon = nextGradeThreshold.subtract(currentCarbon);
        
        goals.put("nextGrade", getNextGrade(currentGrade));
        goals.put("remainingCarbon", remainingCarbon.max(BigDecimal.ZERO));
        goals.put("progressPercentage", calculateProgressPercentage(currentCarbon, nextGradeThreshold));
        goals.put("description", String.format("다음 등급까지 %.1fkg의 탄소를 더 절약하세요!", remainingCarbon.max(BigDecimal.ZERO)));
        
        return goals;
    }

    private List<String> generateRecommendations(BigDecimal carbonSaved, String grade) {
        List<String> recommendations = new ArrayList<>();
        
        if (grade.equals("D급")) {
            recommendations.add("걷기 챌린지에 참여해보세요!");
            recommendations.add("친환경 퀴즈를 풀어보세요!");
            recommendations.add("재활용 챌린지에 도전해보세요!");
        } else if (grade.equals("C급")) {
            recommendations.add("더 많은 챌린지에 참여해보세요!");
            recommendations.add("팀 챌린지에 참여해보세요!");
            recommendations.add("친환경 소비를 늘려보세요!");
        } else if (grade.equals("B급")) {
            recommendations.add("고급 챌린지에 도전해보세요!");
            recommendations.add("환경 교육 콘텐츠를 활용해보세요!");
            recommendations.add("친구들과 함께 참여해보세요!");
        } else {
            recommendations.add("환경 보호 리더가 되어보세요!");
            recommendations.add("새로운 친환경 활동을 찾아보세요!");
            recommendations.add("환경 보호 지식을 공유해보세요!");
        }
        
        return recommendations;
    }

    private String getImpactIcon(String impactLevel) {
        switch (impactLevel) {
            case "환경 지킴이": return "🌍";
            case "환경 실천가": return "🌿";
            case "환경 관심자": return "🌱";
            case "환경 초보자": return "🌱";
            default: return "🌱";
        }
    }

    private String getImpactColor(String impactLevel) {
        switch (impactLevel) {
            case "환경 지킴이": return "#10B981";
            case "환경 실천가": return "#059669";
            case "환경 관심자": return "#34D399";
            case "환경 초보자": return "#6EE7B7";
            default: return "#A7F3D0";
        }
    }

    private BigDecimal getNextGradeThreshold(String currentGrade) {
        switch (currentGrade) {
            case "D급": return BigDecimal.valueOf(10);
            case "C급": return BigDecimal.valueOf(25);
            case "B급": return BigDecimal.valueOf(50);
            case "A급": return BigDecimal.valueOf(100);
            default: return BigDecimal.valueOf(100);
        }
    }

    private String getNextGrade(String currentGrade) {
        switch (currentGrade) {
            case "D급": return "C급";
            case "C급": return "B급";
            case "B급": return "A급";
            case "A급": return "S급";
            default: return "S급";
        }
    }

    private BigDecimal calculateProgressPercentage(BigDecimal current, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return current.divide(target, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private Map<String, BigDecimal> calculateChallengeImpactsFromRecords(Long userId) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 사용자의 승인된 챌린지 기록 조회
            List<ChallengeRecord> approvedRecords = challengeRecordRepository
                .findByMemberIdAndVerificationStatus(userId, "APPROVED");
            
            BigDecimal totalCarbonSaved = BigDecimal.ZERO;
            
            // 각 챌린지 기록의 탄소절약량 합계
            for (ChallengeRecord record : approvedRecords) {
                Challenge challenge = record.getChallenge();
                if (challenge.getCarbonSaved() != null) {
                    totalCarbonSaved = totalCarbonSaved.add(BigDecimal.valueOf(challenge.getCarbonSaved()));
                }
            }
            
            impacts.put("carbonSaved", totalCarbonSaved);
            
        } catch (Exception e) {
            log.error("챌린지 기록에서 탄소절약량 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    private Map<String, BigDecimal> calculateChallengeImpactsFromRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> impacts = new HashMap<>();
        
        try {
            // 해당 기간에 승인된 챌린지 기록에서 환경 임팩트 계산
            List<ChallengeRecord> approvedRecords = challengeRecordRepository
                .findByMember_MemberIdAndVerificationStatusAndActivityDateBetween(userId, "APPROVED", 
                    startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
            
            BigDecimal totalCarbonSaved = BigDecimal.ZERO;
            for (ChallengeRecord record : approvedRecords) {
                if (record.getChallenge().getCarbonSaved() != null) {
                    totalCarbonSaved = totalCarbonSaved.add(BigDecimal.valueOf(record.getChallenge().getCarbonSaved()));
                }
            }
            
            impacts.put("carbonSaved", totalCarbonSaved);
        } catch (Exception e) {
            log.error("월간 챌린지 기록 환경 임팩트 계산 실패: userId={}, error={}", userId, e.getMessage());
            impacts.put("carbonSaved", BigDecimal.ZERO);
        }
        
        return impacts;
    }

    public String calculateEnvironmentalImpact(BigDecimal carbonKg) {
        log.info("환경 가치 환산 계산 시작 - carbonKg: {}", carbonKg);
        
        try {
            Map<String, Object> impact = new HashMap<>();
            
            // 탄소 절감량
            impact.put("carbonKg", carbonKg.doubleValue());
            
            // 환경 가치 환산
            // 탄소 1kg = 소나무 0.102그루 심기 효과 산림청 기준 (소나무 30년생 1그루 당 연간 약 6.6kg ~ 9.8kg CO₂ 흡수) 중 최대치 9.8kg 사용
            double trees = carbonKg.doubleValue() / 9.8;
            impact.put("trees", Math.round(trees * 10.0) / 10.0);

            // 탄소 1kg = 물 7,073L 절약 (K-water 전력원단위 0.3407kWh/m³, 배출계수 0.415kg/kWh)
            double waterLiters = carbonKg.doubleValue() * 7073.0;
            impact.put("waterLiters", Math.round(waterLiters * 10.0) / 10.0);
            
            // 탄소 1kg = 전기 2.41kWh 절약 (GIR 2024 국가 전력배출계수 0.415kg/kWh)
            double energyKwh = carbonKg.doubleValue() * (1.0 / 0.415);
            impact.put("energyKwh", Math.round(energyKwh * 10.0) / 10.0);
            
            String result = objectMapper.writeValueAsString(impact);
            log.info("환경 가치 환산 계산 완료 - impact: {}", result);
            
            return result;
            
        } catch (Exception e) {
            log.error("환경 가치 환산 계산 실패 - carbonKg: {}, error: {}", 
                     carbonKg, e.getMessage(), e);
            return "{\"carbonKg\":0,\"trees\":0,\"waterLiters\":0,\"energyKwh\":0}";
        }
    }

}
