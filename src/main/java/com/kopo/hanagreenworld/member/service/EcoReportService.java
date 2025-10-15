package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.member.domain.EcoReport;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.EcoReportRepository;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.activity.domain.WalkingRecord;
import com.kopo.hanagreenworld.activity.domain.QuizRecord;
import com.kopo.hanagreenworld.activity.domain.ElectronicReceiptRecord;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.repository.WalkingRecordRepository;
import com.kopo.hanagreenworld.activity.repository.QuizRecordRepository;
import com.kopo.hanagreenworld.activity.repository.ElectronicReceiptRecordRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import com.kopo.hanagreenworld.product.service.BenefitCalculationService;
import com.kopo.hanagreenworld.member.service.RankingService;
import com.kopo.hanagreenworld.activity.service.EnvironmentalImpactService;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EcoReportService {

    private final EcoReportRepository ecoReportRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final WalkingRecordRepository walkingRecordRepository;
    private final QuizRecordRepository quizRecordRepository;
    private final ElectronicReceiptRecordRepository electronicReceiptRecordRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final BenefitCalculationService benefitCalculationService;
    private final RankingService rankingService;
    private final EnvironmentalImpactService environmentalImpactService;
    private final EcoSeedService ecoSeedService;
    private final ObjectMapper objectMapper;

    @Transactional
    public EcoReport generateMonthlyReport(Long memberId, String reportMonth) {
        log.info("월간 리포트 생성 시작 - memberId: {}, reportMonth: {}", memberId, reportMonth);
        
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            Optional<EcoReport> existingReport = ecoReportRepository
                    .findByMember_MemberIdAndReportMonth(memberId, reportMonth);
            if (existingReport.isPresent()) {
                log.info("기존 리포트가 존재합니다. 업데이트합니다.");
                return updateExistingReport(existingReport.get(), memberId, reportMonth);
            }
            
            // 활동 데이터 집계
            ActivityData activityData = calculateActivityData(memberId, reportMonth);
            
            // 레벨 정보 조회
            LevelInfo levelInfo = getLevelInfo(memberId);
            
            // 금융 혜택 계산
            String financialBenefit = benefitCalculationService.calculateFinancialBenefit(memberId, levelInfo.currentLevel);

            // 랭킹 계산
            String userRanking = calculateRealRanking(memberId, activityData.totalSeeds, reportMonth);
            
            // 환경 가치 환산
            String environmentalImpact = environmentalImpactService.calculateEnvironmentalImpact(activityData.totalCarbonKg);

            EcoReport ecoReport = EcoReport.builder()
                    .member(member)
                    .reportMonth(reportMonth)
                    .totalSeeds(activityData.totalSeeds)
                    .totalCarbonKg(activityData.totalCarbonKg)
                    .totalActivities(activityData.totalActivities)
                    .activitiesData(activityData.activitiesDataJson)
                    .topActivity(activityData.topActivity)
                    .currentLevel(levelInfo.currentLevel)
                    .nextLevel(levelInfo.nextLevel)
                    .levelProgress(levelInfo.levelProgress)
                    .pointsToNextLevel(levelInfo.pointsToNextLevel)
                    .financialBenefit(financialBenefit)
                    .userRanking(userRanking)
                    .environmentalImpact(environmentalImpact)
                    .dataViewType("COUNT")
                    .build();
            
            EcoReport savedReport = ecoReportRepository.save(ecoReport);
            log.info("월간 리포트 생성 완료 - reportId: {}", savedReport.getId());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("월간 리포트 생성 실패 - memberId: {}, reportMonth: {}, error: {}", 
                     memberId, reportMonth, e.getMessage(), e);
            throw new RuntimeException("월간 리포트 생성에 실패했습니다.", e);
        }
    }

    @Transactional
    public EcoReport updateExistingReport(EcoReport existingReport, Long memberId, String reportMonth) {
        log.info("기존 리포트 업데이트 시작 - reportId: {}", existingReport.getId());
        
        // 활동 데이터 재집계
        ActivityData activityData = calculateActivityData(memberId, reportMonth);
        LevelInfo levelInfo = getLevelInfo(memberId);
        
        // 기존 리포트 업데이트
        existingReport.updateStats(activityData.totalSeeds, activityData.totalCarbonKg, activityData.totalActivities);
        existingReport.updateActivitiesData(activityData.activitiesDataJson);
        existingReport.updateTopActivity(activityData.topActivity);
        existingReport.updateLevelInfo(levelInfo.currentLevel, levelInfo.nextLevel, 
                                    levelInfo.levelProgress, levelInfo.pointsToNextLevel);
        existingReport.updateFinancialBenefit(benefitCalculationService.calculateFinancialBenefit(memberId, levelInfo.currentLevel));
        existingReport.updateUserRanking(calculateRealRanking(memberId, activityData.totalSeeds, reportMonth));
        existingReport.updateEnvironmentalImpact(environmentalImpactService.calculateEnvironmentalImpact(activityData.totalCarbonKg));
        
        return ecoReportRepository.save(existingReport);
    }

    public List<EcoReport> getReportsByMemberId(Long memberId) {
        return ecoReportRepository.findByMember_MemberIdOrderByReportMonthDesc(memberId);
    }

    public Optional<EcoReport> getReportByMemberIdAndMonth(Long memberId, String reportMonth) {
        return ecoReportRepository.findByMember_MemberIdAndReportMonth(memberId, reportMonth);
    }

    private ActivityData calculateActivityData(Long memberId, String reportMonth) {
        log.info("활동 데이터 집계 시작 - memberId: {}, reportMonth: {}", memberId, reportMonth);
        
        // 해당 월의 시작일과 종료일 계산
        LocalDateTime startDate = LocalDateTime.parse(reportMonth + "-01T00:00:00");
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        
        // 걷기 활동 데이터
        List<WalkingRecord> walkingRecords = walkingRecordRepository
                .findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(memberId, startDate, endDate);
        
        // 퀴즈 활동 데이터
        List<QuizRecord> quizRecords = quizRecordRepository
                .findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(memberId, startDate, endDate);
        
        // 전자영수증 활동 데이터
        List<ElectronicReceiptRecord> receiptRecords = electronicReceiptRecordRepository
                .findByMemberAndDateRange(memberId, startDate, endDate);
        
        // 챌린지 활동 데이터
        List<ChallengeRecord> challengeRecords = challengeRecordRepository
                .findByMember_MemberIdAndActivityDateBetween(memberId, startDate, endDate);

        // 활동 별 집계
        Map<String, ActivityStats> activityStatsMap = new HashMap<>();

        // 걷기 활동 집계
        long walkingCount = walkingRecords.size();
        long walkingPoints = walkingRecords.stream().mapToLong(WalkingRecord::getPointsAwarded).sum();
        BigDecimal walkingCarbon = walkingRecords.stream()
                .map(WalkingRecord::getCarbonSaved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (walkingCount > 0) {
            activityStatsMap.put("걷기", new ActivityStats("걷기", walkingCount, walkingPoints, "#10B981"));
        }
        
        // 퀴즈 활동 집계
        long quizCount = quizRecords.size();
        long quizPoints = quizRecords.stream().mapToLong(QuizRecord::getPointsAwarded).sum();
        
        if (quizCount > 0) {
            activityStatsMap.put("퀴즈", new ActivityStats("퀴즈", quizCount, quizPoints, "#3B82F6"));
        }
        
        // 전자영수증 활동 집계
        long receiptCount = receiptRecords.size();
        long receiptPoints = receiptRecords.stream().mapToLong(ElectronicReceiptRecord::getPointsEarned).sum();
        BigDecimal receiptCarbon = BigDecimal.ZERO; // 전자영수증은 탄소 절약량이 없음
        
        if (receiptCount > 0) {
            activityStatsMap.put("전자영수증", new ActivityStats("전자영수증", receiptCount, receiptPoints, "#F59E0B"));
        }
        
        // 챌린지 활동 집계
        long challengeCount = challengeRecords.size();
        long challengePoints = challengeRecords.stream()
                .mapToLong(record -> record.getPointsAwarded() != null ? record.getPointsAwarded() : 0L)
                .sum();
        
        log.info("챌린지 활동 집계 - count: {}, points: {}", challengeCount, challengePoints);
        
        // 총합 계산
        long totalSeeds = walkingPoints + quizPoints + receiptPoints + 
                         (challengeCount > 0 ? challengePoints : 50L);
        int totalActivities = (int) (walkingCount + quizCount + receiptCount + 
                                   (challengeCount > 0 ? challengeCount : 1L));
        BigDecimal totalCarbon = walkingCarbon.add(receiptCarbon);
        
        // 활동별 비율 계산 및 JSON 생성
        String activitiesDataJson = generateActivitiesDataJson(activityStatsMap, totalSeeds, totalActivities);
        
        // 가장 많이 한 활동 선정 (횟수 기준)
        String topActivity = activityStatsMap.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.count, b.count)))
                .map(Map.Entry::getKey)
                .orElse("없음");
        
        return new ActivityData(totalSeeds, totalCarbon, totalActivities, activitiesDataJson, topActivity);
    }

    private LevelInfo getLevelInfo(Long memberId) {
        Map<String, Object> userStats = ecoSeedService.getUserStats(memberId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> currentLevelInfo = (Map<String, Object>) userStats.get("currentLevel");
        @SuppressWarnings("unchecked")
        Map<String, Object> nextLevelInfo = (Map<String, Object>) userStats.get("nextLevel");
        
        String currentLevel = (String) currentLevelInfo.get("name");
        String nextLevel = nextLevelInfo != null ? (String) nextLevelInfo.get("name") : null;
        BigDecimal levelProgress = BigDecimal.valueOf((Double) userStats.get("progressToNextLevel") * 100);
        Long pointsToNextLevel = ((Number) userStats.get("pointsToNextLevel")).longValue();
        
        return new LevelInfo(currentLevel, nextLevel, levelProgress, pointsToNextLevel);
    }

    private String generateActivitiesDataJson(Map<String, ActivityStats> activityStatsMap, long totalSeeds, int totalActivities) {
        try {
            List<Map<String, Object>> activitiesList = new ArrayList<>();
            
            for (ActivityStats stats : activityStatsMap.values()) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("label", stats.label);
                activity.put("count", stats.count);
                activity.put("points", stats.points);
                activity.put("countPercentage", totalActivities > 0 ? Math.round(stats.count * 100.0 / totalActivities) : 0);
                activity.put("pointsPercentage", totalSeeds > 0 ? Math.round(stats.points * 100.0 / totalSeeds) : 0);
                activity.put("color", stats.color);
                
                activitiesList.add(activity);
            }
            
            return objectMapper.writeValueAsString(activitiesList);
        } catch (Exception e) {
            log.error("활동 데이터 JSON 생성 실패: {}", e.getMessage());
            return "[]";
        }
    }

    private static class ActivityData {
        final long totalSeeds;
        final BigDecimal totalCarbonKg;
        final int totalActivities;
        final String activitiesDataJson;
        final String topActivity;

        ActivityData(long totalSeeds, BigDecimal totalCarbonKg, int totalActivities, 
                    String activitiesDataJson, String topActivity) {
            this.totalSeeds = totalSeeds;
            this.totalCarbonKg = totalCarbonKg;
            this.totalActivities = totalActivities;
            this.activitiesDataJson = activitiesDataJson;
            this.topActivity = topActivity;
        }
    }

    private static class LevelInfo {
        final String currentLevel;
        final String nextLevel;
        final BigDecimal levelProgress;
        final Long pointsToNextLevel;

        LevelInfo(String currentLevel, String nextLevel, BigDecimal levelProgress, Long pointsToNextLevel) {
            this.currentLevel = currentLevel;
            this.nextLevel = nextLevel;
            this.levelProgress = levelProgress;
            this.pointsToNextLevel = pointsToNextLevel;
        }
    }

    private static class ActivityStats {
        final String label;
        final long count;
        final long points;
        final String color;

        ActivityStats(String label, long count, long points, String color) {
            this.label = label;
            this.count = count;
            this.points = points;
            this.color = color;
        }
    }

    private String calculateRealRanking(Long memberId, Long totalSeeds, String reportMonth) {
        try {
            Map<String, Object> userRanking = new HashMap<>();
            
            // 실제 사용자 수 조회
            long totalUsers = memberProfileRepository.count();
            
            if (totalUsers <= 1) {
                // 사용자가 1명 이하면 랭킹 의미 없음
                userRanking.put("percentile", 100);
                userRanking.put("totalUsers", totalUsers);
                userRanking.put("rank", 1L);
                userRanking.put("userPoints", totalSeeds);
                userRanking.put("averagePoints", totalSeeds);
                userRanking.put("maxPoints", totalSeeds);
                
                String result = objectMapper.writeValueAsString(userRanking);
                log.info("사용자 1명 이하 - 랭킹 계산 완료: {}", result);
                return result;
            }
            
            // 해당 월의 모든 사용자 포인트 조회
            Map<Long, Long> monthlyUserPoints = calculateMonthlyUserPoints(reportMonth);
            log.info("월간 사용자 포인트 데이터: {}", monthlyUserPoints);
            
            // 현재 사용자의 랭킹 계산
            long betterUsers = 0;
            long maxPoints = 0;
            long totalPoints = 0;
            
            for (Map.Entry<Long, Long> entry : monthlyUserPoints.entrySet()) {
                Long userId = entry.getKey();
                Long userPoints = entry.getValue();
                
                if (userPoints > totalSeeds) {
                    betterUsers++;
                }
                if (userPoints > maxPoints) {
                    maxPoints = userPoints;
                }
                totalPoints += userPoints;
            }
            
            // 랭킹 계산
            long rank = betterUsers + 1; // 현재 사용자보다 높은 사용자 수 + 1
            long averagePoints = totalUsers > 0 ? totalPoints / totalUsers : 0;
            int percentile = totalUsers > 0 ? (int) Math.round((double) betterUsers / totalUsers * 100) : 0;
            
            userRanking.put("percentile", percentile);
            userRanking.put("totalUsers", totalUsers);
            userRanking.put("rank", rank);
            userRanking.put("userPoints", totalSeeds);
            userRanking.put("averagePoints", averagePoints);
            userRanking.put("maxPoints", maxPoints);
            
            String result = objectMapper.writeValueAsString(userRanking);
            log.info("랭킹 계산 완료 - percentile: {}%, rank: {}/{}, totalUsers: {}",
                    percentile, rank, totalUsers, totalUsers);
            
            return result;
            
        } catch (Exception e) {
            log.error("랭킹 계산 실패: {}", e.getMessage(), e);
            // 오류 시 기본값 (실제 사용자 수 기반)
            long totalUsers = memberProfileRepository.count();
            return "{\"percentile\":50,\"totalUsers\":" + totalUsers + ",\"rank\":" + (totalUsers/2) + 
                   ",\"userPoints\":" + totalSeeds + ",\"averagePoints\":500,\"maxPoints\":1000}";
        }
    }

    private Map<Long, Long> calculateMonthlyUserPoints(String reportMonth) {
        try {
            // 해당 월의 시작일과 종료일 계산
            LocalDateTime startDate = LocalDateTime.parse(reportMonth + "-01T00:00:00");
            LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
            
            log.info("월간 사용자 포인트 계산 시작 - reportMonth: {}, startDate: {}, endDate: {}", 
                    reportMonth, startDate, endDate);
            
            // 모든 사용자 조회 (성능 최적화: ID만 조회)
            List<Long> allMemberIds = memberRepository.findAll().stream()
                    .map(Member::getMemberId)
                    .collect(Collectors.toList());
            
            log.info("총 사용자 수: {}", allMemberIds.size());
            
            Map<Long, Long> monthlyPoints = new HashMap<>();
            
            // 배치 처리로 성능 최적화
            int batchSize = 10; // 한 번에 10명씩 처리
            for (int i = 0; i < allMemberIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allMemberIds.size());
                List<Long> batchMemberIds = allMemberIds.subList(i, endIndex);
                
                for (Long memberId : batchMemberIds) {
                    try {
                        Long monthlyPointsForUser = calculateUserMonthlyPoints(memberId, startDate, endDate);
                        monthlyPoints.put(memberId, monthlyPointsForUser);
                    } catch (Exception e) {
                        log.warn("사용자 {} 포인트 계산 실패: {}", memberId, e.getMessage());
                        monthlyPoints.put(memberId, 0L);
                    }
                }
                
                // 배치 간 짧은 대기 (DB 부하 방지)
                if (endIndex < allMemberIds.size()) {
                    try {
                        Thread.sleep(10); // 10ms 대기
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            return monthlyPoints;
            
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Long calculateUserMonthlyPoints(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // 걷기 포인트
            List<WalkingRecord> walkingRecords = walkingRecordRepository
                    .findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(memberId, startDate, endDate);
            long walkingPoints = walkingRecords.stream().mapToLong(WalkingRecord::getPointsAwarded).sum();
            
            // 퀴즈 포인트
            List<QuizRecord> quizRecords = quizRecordRepository
                    .findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(memberId, startDate, endDate);
            long quizPoints = quizRecords.stream().mapToLong(QuizRecord::getPointsAwarded).sum();
            
            // 전자영수증 포인트
            List<ElectronicReceiptRecord> receiptRecords = electronicReceiptRecordRepository
                    .findByMemberAndDateRange(memberId, startDate, endDate);
            long receiptPoints = receiptRecords.stream().mapToLong(ElectronicReceiptRecord::getPointsEarned).sum();
            
            // 챌린지 포인트
            List<ChallengeRecord> challengeRecords = challengeRecordRepository
                    .findByMember_MemberIdAndActivityDateBetween(memberId, startDate, endDate);
            long challengePoints = challengeRecords.stream()
                    .mapToLong(record -> record.getPointsAwarded() != null ? record.getPointsAwarded() : 0L)
                    .sum();
            
            return walkingPoints + quizPoints + receiptPoints + challengePoints;
            
        } catch (Exception e) {
            log.error("사용자 월간 포인트 계산 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            return 0L;
        }
    }

    public String getUserCurrentLevel(Long userId) {
        try {
            // 사용자의 총 원큐씨앗 조회
            Long totalSeeds = pointTransactionRepository.findTotalSeedsByUserId(userId);
            
            // 레벨별 기준점
            if (totalSeeds >= 10000) {
                return "EXPERT";      // 10,000 이상: 전문가
            } else if (totalSeeds >= 5000) {
                return "INTERMEDIATE"; // 5,000 이상: 실천가
            } else {
                return "BEGINNER";     // 5,000 미만: 입문자
            }
            
        } catch (Exception e) {
            log.error("사용자 레벨 조회 실패 - 사용자ID: {}, 에러: {}", userId, e.getMessage(), e);
            return "BEGINNER"; // 기본값
        }
    }

}
