package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.dto.UserStatsResponse;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.repository.EcoReportRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.domain.EcoReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final ChallengeRecordRepository challengeRecordRepository;
    private final EcoReportRepository ecoReportRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    @SuppressWarnings("deprecation")
    public UserStatsResponse getUserStats(Long memberId) {
        try {
            log.info("getUserStats 시작 - memberId: {}", memberId);
            
            // 실제 가입일 조회
            String registrationDate = getRegistrationDate(memberId);
            log.info("가입일: {}", registrationDate);
            
            // 실천일 계산
            LocalDate regDate = LocalDate.parse(registrationDate);
            LocalDate now = LocalDate.now();
            int practiceDays = (int) ChronoUnit.DAYS.between(regDate, now);
            log.info("실천일: {}일", practiceDays);
            
            // 실제 상위 퍼센트 계산 (기존 호환성을 위해 deprecated 메서드 사용)
            @SuppressWarnings("deprecation")
            double topPercentage = calculateTopPercentage(memberId);
            log.info("상위 퍼센트: {}%", topPercentage);
            
            // 실제 월간 탄소절약량 증감률 계산
            Double monthlyGrowthRate = calculateMonthlyGrowthRate(memberId);
            log.info("월간 탄소절약량 증감률: {}", monthlyGrowthRate);
            
            // 실제 월간 원큐씨앗 증감률 계산
            Double ecoSeedsGrowthRate = calculateEcoSeedsGrowthRate(memberId);
            log.info("월간 원큐씨앗 증감률: {}", ecoSeedsGrowthRate);
            
            // 실제 순위 정보 계산
            int[] rankingInfo = calculateUserRanking(memberId);
            int userRanking = rankingInfo[0];
            int totalUsers = rankingInfo[1];
            log.info("사용자 순위: {}/{}", userRanking, totalUsers);
            
            // 4가지 평균 대비 계산
            double monthlyCarbonComparison = calculateMonthlyTopPercentageByCarbon(memberId);
            double totalCarbonComparison = calculateTotalTopPercentageByCarbon(memberId);
            double monthlyPointsComparison = calculateMonthlyTopPercentageByPoints(memberId);
            double totalPointsComparison = calculateTotalTopPercentageByPoints(memberId);
            
            log.info("월간 탄소 상위 퍼센트: {}%, 전체 탄소 상위 퍼센트: {}%", monthlyCarbonComparison, totalCarbonComparison);
            log.info("월간 포인트 상위 퍼센트: {}%, 전체 포인트 상위 퍼센트: {}%", monthlyPointsComparison, totalPointsComparison);
            
            UserStatsResponse response = UserStatsResponse.builder()
                    .registrationDate(registrationDate)
                    .practiceDays(practiceDays)
                    .monthlyCarbonComparison(monthlyCarbonComparison)
                    .totalCarbonComparison(totalCarbonComparison)
                    .monthlyPointsComparison(monthlyPointsComparison)
                    .totalPointsComparison(totalPointsComparison)
                    .monthlyGrowthRate(monthlyGrowthRate)
                    .ecoSeedsGrowthRate(ecoSeedsGrowthRate)
                    .averageComparison(topPercentage) // 기존 호환성을 위한 deprecated 필드
                    .comparisonDescription(String.format("상위 %.1f%% 사용자", (double)userRanking / totalUsers * 100))
                    .userRanking(userRanking)
                    .totalUsers(totalUsers)
                    .build();
            
            log.info("getUserStats 완료 - memberId: {}", memberId);
            return response;
            
        } catch (Exception e) {
            log.error("getUserStats 실패 - memberId: {}, error: {}", memberId, e.getMessage(), e);
            throw e;
        }
    }

    public String getRegistrationDate(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + memberId));
            return member.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            // 오류 시 기본값 반환
            return "2025-10-24";
        }
    }

    // 이번달 탄소절감 기준 상위 퍼센트
    private double calculateMonthlyTopPercentageByCarbon(Long memberId) {
        try {
            double userMonthlyCarbon = getUserMonthlyCarbonSaved(memberId);
            int betterUsers = getUsersWithHigherMonthlyCarbon(userMonthlyCarbon, memberId);
            int totalUsers = getTotalUserCount(memberId);
            
            if (totalUsers <= 1) return 1.0;
            
            // 상위 퍼센트 계산: 1등일 때는 상위 1%, 그 외에는 정상 계산
            if (betterUsers == 0) {
                return 1.0; // 1등은 상위 1%
            }
            // 상위 퍼센트 계산: 1등일 때는 상위 1%, 그 외에는 정상 계산
            if (betterUsers == 0) {
                return 1.0; // 1등은 상위 1%
            }
            double percentage = ((double)(totalUsers - betterUsers) / totalUsers) * 100;
            return Math.max(1.0, Math.min(99.0, percentage));
        } catch (Exception e) {
            return 30.0;
        }
    }
    
    // 전체 탄소절감 기준 상위 퍼센트
    private double calculateTotalTopPercentageByCarbon(Long memberId) {
        try {
            double userTotalCarbon = getUserTotalCarbonSaved(memberId);
            int betterUsers = getUsersWithHigherTotalCarbon(userTotalCarbon, memberId);
            int totalUsers = getTotalUserCount(memberId);
            
            if (totalUsers <= 1) return 1.0;
            
            // 상위 퍼센트 계산: 1등일 때는 상위 1%, 그 외에는 정상 계산
            if (betterUsers == 0) {
                return 1.0; // 1등은 상위 1%
            }
            double percentage = ((double)(totalUsers - betterUsers) / totalUsers) * 100;
            return Math.max(1.0, Math.min(99.0, percentage));
        } catch (Exception e) {
            return 30.0;
        }
    }
    
    // 이번달 원큐씨앗 기준 상위 퍼센트
    private double calculateMonthlyTopPercentageByPoints(Long memberId) {
        try {
            double userMonthlyPoints = getUserMonthlyPoints(memberId);
            int betterUsers = getUsersWithHigherMonthlyPoints(userMonthlyPoints, memberId);
            int totalUsers = getTotalUserCount(memberId);
            
            if (totalUsers <= 1) return 1.0;
            
            // 상위 퍼센트 계산: 1등일 때는 상위 1%, 그 외에는 정상 계산
            if (betterUsers == 0) {
                return 1.0; // 1등은 상위 1%
            }
            double percentage = ((double)(totalUsers - betterUsers) / totalUsers) * 100;
            return Math.max(1.0, Math.min(99.0, percentage));
        } catch (Exception e) {
            return 30.0;
        }
    }
    
    // 전체 원큐씨앗 기준 상위 퍼센트
    private double calculateTotalTopPercentageByPoints(Long memberId) {
        try {
            double userTotalPoints = getUserTotalPoints(memberId);
            int betterUsers = getUsersWithHigherTotalPoints(userTotalPoints, memberId);
            int totalUsers = getTotalUserCount(memberId);
            
            if (totalUsers <= 1) return 1.0;
            
            // 상위 퍼센트 계산: 1등일 때는 상위 1%, 그 외에는 정상 계산
            if (betterUsers == 0) {
                return 1.0; // 1등은 상위 1%
            }
            double percentage = ((double)(totalUsers - betterUsers) / totalUsers) * 100;
            return Math.max(1.0, Math.min(99.0, percentage));
        } catch (Exception e) {
            return 30.0;
        }
    }

    // 기존 메서드 (deprecated)
    @Deprecated
    private double calculateTopPercentage(Long memberId) {
        try {
            // 1. 현재 사용자의 총 탄소절약량 조회
            double userCarbonSaved = getUserTotalCarbonSaved(memberId);
            
            // 2. 전체 사용자 중 현재 사용자보다 높은 탄소절약량을 가진 사용자 수
            int betterUsers = getUsersWithHigherCarbonSaved(userCarbonSaved, memberId);
            
            // 3. 전체 사용자 수
            int totalUsers = getTotalUserCount(memberId);
            
            // 4. 상위 퍼센트 계산 (최소 1%, 최대 99%)
            if (totalUsers <= 1) {
                return 1.0; // 사용자가 1명 이하면 상위 1%
            }
            
            double percentage = ((double) betterUsers / totalUsers) * 100;
            
            // 0% 이하인 경우 최소 1%로 설정
            if (percentage <= 0) {
                return 1.0;
            }
            
            // 99% 이상인 경우 최대 99%로 설정
            if (percentage >= 100) {
                return 99.0;
            }
            
            return percentage;
        } catch (Exception e) {
            // 오류 시 기본값 반환
            return 30.0;
        }
    }

    private Double calculateMonthlyGrowthRate(Long memberId) {
        try {
            // 이번 달 탄소절약량 (member_profile에서 조회)
            double currentMonth = getCurrentMonthCarbonFromProfile(memberId);

            // 지난 달 탄소절약량 (eco_reports에서 조회)
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            
            double lastMonthCarbon = getLastMonthCarbonFromEcoReport(memberId, lastMonth.getYear(), lastMonth.getMonthValue());
           
            // 지난달 리포트가 없으면 null 반환
            if (lastMonthCarbon == -1.0) {
                log.debug("지난달 리포트가 없음 - null 반환");
                return null;
            }

            if (lastMonthCarbon == 0) {
                double result = currentMonth > 0 ? 100.0 : 0.0;
                log.debug("지난달 탄소절약량이 0 - 증감률: {}", result);
                return result;
            }
            
            double growthRate = ((currentMonth - lastMonthCarbon) / lastMonthCarbon) * 100;
            log.debug("계산된 증감률: {}", growthRate);
            return growthRate;
        } catch (Exception e) {
            log.error("calculateMonthlyGrowthRate 에러: {}", e.getMessage(), e);
            return null; // 오류 시 null 반환
        }
    }

    private Double calculateEcoSeedsGrowthRate(Long memberId) {
        try {
            // 이번 달 원큐씨앗 (member_profile에서 조회)
            double currentSeeds = getUserMonthlyPoints(memberId);
            
            // 지난 달 원큐씨앗 (eco_reports에서 조회)
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            double lastMonthSeeds = getLastMonthEcoSeeds(memberId, lastMonth.getYear(), lastMonth.getMonthValue());

            // 지난달 리포트가 없으면 null 반환
            if (lastMonthSeeds == -1.0) {
                return null;
            }

            if (lastMonthSeeds == 0) {
                return currentSeeds > 0 ? 100.0 : 0.0;
            }
            
            double growthRate = ((currentSeeds - lastMonthSeeds) / lastMonthSeeds) * 100;
            return growthRate;
        } catch (Exception e) {
            return null; // 오류 시 null 반환
        }
    }

    private double getCurrentEcoSeeds(Long memberId) {
        try {
            var profile = memberProfileRepository.findByMember_MemberId(memberId);
            if (profile.isPresent()) {
                Long currentPoints = profile.get().getCurrentPoints();
                return currentPoints != null ? currentPoints.doubleValue() : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getLastMonthEcoSeeds(Long memberId, int year, int month) {
        try {
            String reportMonth = String.format("%04d-%02d", year, month);

            Optional<EcoReport> ecoReport = ecoReportRepository
                .findByMember_MemberIdAndReportMonth(memberId, reportMonth);

            if (ecoReport.isPresent()) {
                Long totalSeeds = ecoReport.get().getTotalSeeds();
                return totalSeeds != null ? totalSeeds.doubleValue() : 0.0;
            } else {
                return -1.0; // 지난달 리포트가 없으면 -1 반환
            }
        } catch (Exception e) {
            return -1.0; // 오류 시 -1 반환
        }
    }

    private int[] calculateUserRanking(Long memberId) {
        try {
            // 1. 현재 사용자의 총 탄소절약량
            double userCarbonSaved = getUserTotalCarbonSaved(memberId);
            
            // 2. 실제 전체 사용자 수 조회
            int totalUsers = getTotalUserCount(memberId);
            
            // 3. 현재 사용자보다 높은 탄소절약량을 가진 사용자 수 계산
            int betterUsers = getUsersWithHigherCarbonSaved(userCarbonSaved, memberId);
            
            return new int[]{betterUsers, totalUsers};
        } catch (Exception e) {
            // 오류 시 기본값 반환
            return new int[]{30, 1000};
        }
    }

    private double getUserTotalCarbonSaved(Long memberId) {
        try {
            
            var profile = memberProfileRepository.findByMember_MemberId(memberId);
            log.debug("MemberProfile 조회 결과: {}", profile.isPresent() ? "존재함" : "없음");
            
            if (profile.isPresent()) {
                Double totalCarbon = profile.get().getTotalCarbonSaved();
                log.debug("totalCarbonSaved 값: {}", totalCarbon);
                return totalCarbon != null ? totalCarbon : 0.0;
            } else {
                log.debug("MemberProfile이 없음 - 0.0 반환");
                return 0.0;
            }
        } catch (Exception e) {
            log.error("getUserTotalCarbonSaved 에러: {}", e.getMessage(), e);
            return 18.2; // 기본값
        }
    }

    private int getUsersWithHigherCarbonSaved(double userCarbonSaved, Long currentMemberId) {
        try {
            log.debug("getUsersWithHigherCarbonSaved 호출 - userCarbonSaved: {}, currentMemberId: {}", userCarbonSaved, currentMemberId);
            
            // member_profiles에서 현재 사용자보다 높은 탄소절약량을 가진 사용자 수 조회
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            int betterUsers = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getTotalCarbonSaved() != null && 
                    profile.getTotalCarbonSaved() > userCarbonSaved) {
                    betterUsers++;
                }
            }
            
            log.debug("실제 DB에서 조회한 더 높은 탄소절약량 사용자 수: {}", betterUsers);
            return betterUsers;
        } catch (Exception e) {
            log.error("getUsersWithHigherCarbonSaved 에러: {}", e.getMessage(), e);
            return 300;
        }
    }


    private int getTotalUserCount(Long currentMemberId) {
        try {
            // member_profiles에서 실제 사용자 수 조회
            long totalUsers = memberProfileRepository.count();
            return (int) totalUsers;
        } catch (Exception e) {
            return 1000;
        }
    }

    private double getAverageCarbonSaved(Long currentMemberId) {
        try {
            log.debug("getAverageCarbonSaved 호출 - currentMemberId: {}", currentMemberId);
            
            // member_profiles에서 모든 사용자의 탄소절약량 조회하여 평균 계산
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            double totalCarbon = 0.0;
            int validProfiles = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getTotalCarbonSaved() != null) {
                    totalCarbon += profile.getTotalCarbonSaved();
                    validProfiles++;
                }
            }
            
            double average = validProfiles > 0 ? totalCarbon / validProfiles : 0.0;
            log.debug("실제 DB에서 조회한 평균 탄소절약량: {}", average);
            return average;
        } catch (Exception e) {
            log.error("getAverageCarbonSaved 에러: {}", e.getMessage(), e);
            return 12.5;
        }
    }

    private double getMonthlyCarbonSaved(Long memberId, int year, int month) {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalDate targetDate = LocalDate.of(year, month, 1);
            
            // 이번달인 경우: member_profiles에서 조회
            if (targetDate.getYear() == currentDate.getYear() && 
                targetDate.getMonthValue() == currentDate.getMonthValue()) {
                return getCurrentMonthCarbonFromProfile(memberId);
            } else {
                // 지난달인 경우: EcoReport에서 조회
                return getLastMonthCarbonFromEcoReport(memberId, year, month);
            }
        } catch (Exception e) {
            return 15.0;
        }
    }

    private double getCurrentMonthCarbonFromProfile(Long memberId) {
        try {
            var profile = memberProfileRepository.findByMember_MemberId(memberId);

            if (profile.isPresent()) {
                Double currentMonthCarbon = profile.get().getCurrentMonthCarbonSaved();
                return currentMonthCarbon != null ? currentMonthCarbon : 0.0;
            } else {
                return 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getLastMonthCarbonFromEcoReport(Long memberId, int year, int month) {
        try {
            String reportMonth = String.format("%04d-%02d", year, month);

            Optional<EcoReport> ecoReport = ecoReportRepository
                .findByMember_MemberIdAndReportMonth(memberId, reportMonth);

            if (ecoReport.isPresent()) {
                Double carbonKg = ecoReport.get().getTotalCarbonKg().doubleValue();
                return carbonKg;
            } else {
                return -1.0;
            }
        } catch (Exception e) {
            return -1.0;
        }
    }

    // 새로운 헬퍼 메서드들
    private double getUserMonthlyCarbonSaved(Long memberId) {
        try {
            var profile = memberProfileRepository.findByMember_MemberId(memberId);
            if (profile.isPresent()) {
                Double currentMonthCarbon = profile.get().getCurrentMonthCarbonSaved();
                return currentMonthCarbon != null ? currentMonthCarbon : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getUserMonthlyPoints(Long memberId) {
        try {
            var profile = memberProfileRepository.findByMember_MemberId(memberId);
            if (profile.isPresent()) {
                Long currentMonthPoints = profile.get().getCurrentMonthPoints();
                return currentMonthPoints != null ? currentMonthPoints.doubleValue() : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getUserTotalPoints(Long memberId) {
        try {
            var profile = memberProfileRepository.findByMember_MemberId(memberId);
            if (profile.isPresent()) {
                Long currentPoints = profile.get().getCurrentPoints();
                return currentPoints != null ? currentPoints.doubleValue() : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getUsersWithHigherMonthlyCarbon(double userMonthlyCarbon, Long currentMemberId) {
        try {
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            int betterUsers = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getCurrentMonthCarbonSaved() != null && 
                    profile.getCurrentMonthCarbonSaved() > userMonthlyCarbon) {
                    betterUsers++;
                }
            }
            return betterUsers;
        } catch (Exception e) {
            return 300;
        }
    }

    private int getUsersWithHigherTotalCarbon(double userTotalCarbon, Long currentMemberId) {
        try {
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            int betterUsers = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getTotalCarbonSaved() != null && 
                    profile.getTotalCarbonSaved() > userTotalCarbon) {
                    betterUsers++;
                }
            }
            return betterUsers;
        } catch (Exception e) {
            return 300;
        }
    }

    private int getUsersWithHigherMonthlyPoints(double userMonthlyPoints, Long currentMemberId) {
        try {
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            int betterUsers = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getCurrentMonthPoints() != null && 
                    profile.getCurrentMonthPoints() > userMonthlyPoints) {
                    betterUsers++;
                }
            }
            return betterUsers;
        } catch (Exception e) {
            return 300;
        }
    }

    private int getUsersWithHigherTotalPoints(double userTotalPoints, Long currentMemberId) {
        try {
            List<MemberProfile> allProfiles = memberProfileRepository.findAll();
            int betterUsers = 0;
            
            for (MemberProfile profile : allProfiles) {
                if (profile.getCurrentPoints() != null && 
                    profile.getCurrentPoints() > userTotalPoints) {
                    betterUsers++;
                }
            }
            return betterUsers;
        } catch (Exception e) {
            return 300;
        }
    }



}
