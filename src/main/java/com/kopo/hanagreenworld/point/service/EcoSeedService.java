package com.kopo.hanagreenworld.point.service;

import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import lombok.extern.slf4j.Slf4j;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.domain.Team;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberTeamRepository;
import com.kopo.hanagreenworld.member.repository.TeamRepository;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.point.domain.PointTransactionType;
import com.kopo.hanagreenworld.point.dto.EcoSeedConvertRequest;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.dto.EcoSeedResponse;
import com.kopo.hanagreenworld.point.dto.EcoSeedTransactionResponse;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.integration.service.HanamoneyCardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EcoSeedService {

    private final PointTransactionRepository pointTransactionRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberRepository memberRepository;
    private final MemberTeamRepository memberTeamRepository;
    private final TeamRepository teamRepository;
    private final HanamoneyCardService hanamoneyCardService;
    private final RestTemplate restTemplate;
    private final GroupIntegrationService groupIntegrationService;

    @Value("${integration.card.url}")
    private String hanacardApiBaseUrl;

    @Transactional
    public EcoSeedResponse getEcoSeedInfo() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        // 거래 내역에서 합계 계산
        Long totalEarned = pointTransactionRepository.sumEarnedPointsByMemberId(memberId);
        Long totalUsed = pointTransactionRepository.sumUsedPointsByMemberId(memberId);
        Long totalConverted = pointTransactionRepository.sumConvertedPointsByMemberId(memberId);
        
        // totalUsed와 totalConverted는 음수로 저장되어 있으므로 절댓값을 사용
        Long actualTotalUsed = Math.abs(totalUsed) + Math.abs(totalConverted);
        
        return EcoSeedResponse.builder()
                .totalSeeds(totalEarned)
                .currentSeeds(profile.getCurrentPoints())
                .usedSeeds(actualTotalUsed)
                .convertedSeeds(Math.abs(totalConverted))
                .message("원큐씨앗 정보 조회 완료")
                .build();
    }

    @Transactional
    public EcoSeedResponse earnEcoSeeds(EcoSeedEarnRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        try {
            // 원큐씨앗 적립 (현재 보유량만 업데이트)
            profile.updateCurrentPoints(request.getPointsAmount().longValue());
            
            // 거래 내역 생성
            PointTransaction transaction = PointTransaction.builder()
                    .member(member)
                    .pointTransactionType(PointTransactionType.EARN)
                    .category(request.getCategory())
                    .description(request.getDescription() != null ? request.getDescription() : 
                               request.getCategory().getDisplayName() + "로 원큐씨앗 적립")
                    .pointsAmount(request.getPointsAmount())
                    .balanceAfter(profile.getCurrentPoints())
                    .build();
            
            // 한 트랜잭션으로 처리
            memberProfileRepository.save(profile);
            pointTransactionRepository.save(transaction);
            
            // 팀 포인트도 동기화 (EARN 타입일 때만)
            if (PointTransactionType.EARN.equals(transaction.getPointTransactionType())) {
                updateTeamPoints(memberId, request.getPointsAmount().longValue());
            }
            
            log.info("원큐씨앗 적립 완료: {} - {}개", memberId, request.getPointsAmount());
            
            return getEcoSeedInfo();
        } catch (Exception e) {
            log.error("원큐씨앗 적립 실패: {} - {}", memberId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void earnEcoSeedsForWebhook(Long memberId, EcoSeedEarnRequest request) {
        log.info("웹훅용 원큐씨앗 적립 시작: memberId={}, points={}", memberId, request.getPointsAmount());
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        try {
            // 원큐씨앗 적립 (현재 보유량만 업데이트)
            profile.updateCurrentPoints(request.getPointsAmount().longValue());
            
            // 거래 내역 생성
            PointTransaction transaction = PointTransaction.builder()
                    .member(member)
                    .pointTransactionType(PointTransactionType.EARN)
                    .category(request.getCategory())
                    .description(request.getDescription() != null ? request.getDescription() : 
                               request.getCategory().getDisplayName() + "로 원큐씨앗 적립")
                    .pointsAmount(request.getPointsAmount())
                    .balanceAfter(profile.getCurrentPoints())
                    .build();
            
            // 한 트랜잭션으로 처리
            memberProfileRepository.save(profile);
            pointTransactionRepository.save(transaction);
            
            // 팀 포인트도 동기화 (EARN 타입일 때만)
            if (PointTransactionType.EARN.equals(transaction.getPointTransactionType())) {
                updateTeamPoints(memberId, request.getPointsAmount().longValue());
            }
            
            log.info("웹훅용 원큐씨앗 적립 완료: {} - {}개", memberId, request.getPointsAmount());
            
        } catch (Exception e) {
            log.error("웹훅용 원큐씨앗 적립 실패: {} - {}", memberId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 원큐씨앗을 하나머니로 전환 (트랜잭션으로 데이터 정합성 보장)
     */
    @Transactional
    public EcoSeedResponse convertToHanaMoney(EcoSeedConvertRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        // 잔액 확인
        if (profile.getCurrentPoints() < request.getPointsAmount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ECO_SEEDS);
        }
        
        try {
            // 전환 전 잔액 로깅
            Long beforeEcoSeeds = profile.getCurrentPoints();
            
            log.info("하나머니 전환 시작: 회원ID={}, 전환금액={}, 전환전 원큐씨앗={}", 
                    memberId, request.getPointsAmount(), beforeEcoSeeds);
            
            // 1. 먼저 하나카드 서버에서 하나머니 적립 시도
            boolean hanamoneyEarnSuccess = hanamoneyCardService.earnHanamoney(
                    member, 
                    request.getPointsAmount().longValue(), 
                    "원큐씨앗 전환: " + request.getPointsAmount() + "개"
            );
            
            if (!hanamoneyEarnSuccess) {
                log.error("하나카드 서버에서 하나머니 적립 실패 - 회원ID: {}, 금액: {}", 
                        memberId, request.getPointsAmount());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            // 2. 하나머니 적립이 성공했으면 원큐씨앗 차감
            profile.updateCurrentPoints(-request.getPointsAmount().longValue());
            
            // 3. 거래 내역 생성 (CONVERT 타입 사용, 음수로 저장)
            PointTransaction transaction = PointTransaction.builder()
                    .member(member)
                    .pointTransactionType(PointTransactionType.CONVERT)
                    .category(PointCategory.HANA_MONEY_CONVERSION)
                    .description("하나머니로 전환: " + request.getPointsAmount() + "개")
                    .pointsAmount(-request.getPointsAmount()) // 음수로 저장
                    .balanceAfter(profile.getCurrentPoints())
                    .build();
            
            // 4. 원큐씨앗 차감과 거래 내역 저장
            memberProfileRepository.save(profile);
            pointTransactionRepository.save(transaction);
            
            // 전환 후 잔액 로깅
            Long afterEcoSeeds = profile.getCurrentPoints();
            
            log.info("하나머니 전환 완료: 회원ID={}, 전환금액={}, 전환후 원큐씨앗={}", 
                    memberId, request.getPointsAmount(), afterEcoSeeds);
            
            // 검증: 원큐씨앗 차감이 정확히 이루어졌는지 확인
            if ((beforeEcoSeeds - afterEcoSeeds) != request.getPointsAmount().longValue()) {
                log.error("원큐씨앗 차감 오류: 예상={}, 실제={}", request.getPointsAmount(), (beforeEcoSeeds - afterEcoSeeds));
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            return getEcoSeedInfo();
        } catch (Exception e) {
            log.error("하나머니 전환 실패: {} - {}", memberId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public Page<EcoSeedTransactionResponse> getTransactionHistory(Pageable pageable) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        Page<PointTransaction> transactions = pointTransactionRepository
                .findByMember_MemberIdOrderByOccurredAtDesc(memberId, pageable);
        
        return transactions.map(EcoSeedTransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EcoSeedTransactionResponse> getTransactionHistoryByCategory(PointCategory category) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        List<PointTransaction> transactions = pointTransactionRepository
                .findByMember_MemberIdAndCategoryOrderByOccurredAtDesc(memberId, category.name());
        
        return transactions.stream()
                .map(EcoSeedTransactionResponse::from)
                .collect(Collectors.toList());
    }


    private MemberProfile getOrCreateMemberProfile(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return memberProfileRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> {
                                return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                            });
                    
                    MemberProfile profile = MemberProfile.builder()
                            .member(member)
                            .nickname(member.getName())
                            .build();
                    
                    return memberProfileRepository.save(profile);
                });
    }

    @Transactional
    public Map<String, Object> getMemberProfile() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        // point_transactions에서 실시간 계산
        Long totalEarned = pointTransactionRepository.sumEarnedPointsByMemberId(memberId);
        Long currentMonthPoints = pointTransactionRepository.sumCurrentMonthEarnedPointsByMemberId(memberId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("currentPoints", profile.getCurrentPoints());
        response.put("totalPoints", totalEarned); // 실시간 계산된 총 적립
        response.put("currentMonthPoints", currentMonthPoints); // 실시간 계산된 이번 달 적립
        
        // 하나머니 정보는 하나카드 서버에서 조회
        try {
            Long hanaMoneyBalance = getHanaMoneyFromCardServer(memberId);
            response.put("hanaMoney", hanaMoneyBalance);
        } catch (Exception e) {
            log.warn("하나카드 서버에서 하나머니 정보 조회 실패: {}", e.getMessage());
            response.put("hanaMoney", 0L);
        }
        
        return response;
    }

    private Long getHanaMoneyFromCardServer(Long memberId) {
        try {
            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            
            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                // CI가 없으면 목데이터용 CI 생성
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
                log.info("CI가 없어서 목데이터용 CI 생성: {}", ci);
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String internalServiceToken = groupIntegrationService.generateInternalServiceToken();
            
            // 하나카드 서버 API 호출
            String url = hanacardApiBaseUrl + "/api/integration/hanamoney-info";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);
            
            Map<String, String> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "consentToken", "CONSENT_" + memberId,
                    "memberId", memberId.toString()
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("하나카드 서버 하나머니 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                Long balance = Long.valueOf(data.get("currentPoints").toString());
                log.info("하나카드 서버에서 하나머니 조회 성공 - 잔액: {}", balance);
                return balance;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return 0L;
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 하나머니 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return 0L;
        }
    }

    @Transactional
    public Map<String, Object> getUserStats() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return getUserStats(memberId);
    }

    @Transactional
    public Map<String, Object> getUserStats(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        MemberProfile profile = getOrCreateMemberProfile(memberId);
        
        // point_transactions에서 실시간 계산
        Long totalEarned = pointTransactionRepository.sumEarnedPointsByMemberId(memberId);
        Long currentMonthPoints = pointTransactionRepository.sumCurrentMonthEarnedPointsByMemberId(memberId);
        
        // 현재 레벨 계산 (포인트에 따라 동적으로 계산)
        long currentPoints = totalEarned != null ? totalEarned : 0L;
        MemberProfile.EcoLevel currentLevel = calculateCurrentLevel(currentPoints);
        MemberProfile.EcoLevel nextLevel = getNextLevel(currentLevel);
        
        // 다음 레벨까지의 진행도 계산
        double progressToNextLevel = 0.0;
        if (nextLevel != null) {
            long currentLevelMin = currentLevel.getMinPoints();
            long nextLevelMin = nextLevel.getMinPoints();
            long totalRange = nextLevelMin - currentLevelMin;
            if (totalRange > 0) {
                long userProgress = currentPoints - currentLevelMin;
                progressToNextLevel = Math.min(1.0, Math.max(0.0, (double) userProgress / totalRange));
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalPoints", totalEarned != null ? totalEarned : 0L);
        response.put("totalCarbonSaved", profile.getTotalCarbonSaved() != null ? profile.getTotalCarbonSaved() : 0.0);
        response.put("totalActivities", profile.getTotalActivitiesCount() != null ? profile.getTotalActivitiesCount() : 0);
        response.put("monthlyPoints", currentMonthPoints != null ? currentMonthPoints : 0L);
        response.put("monthlyCarbonSaved", profile.getCurrentMonthCarbonSaved() != null ? profile.getCurrentMonthCarbonSaved() : 0.0);
        response.put("monthlyActivities", profile.getCurrentMonthActivitiesCount() != null ? profile.getCurrentMonthActivitiesCount() : 0);
        
        // 현재 레벨 정보
        Map<String, Object> currentLevelInfo = new HashMap<>();
        currentLevelInfo.put("id", currentLevel.name().toLowerCase());
        currentLevelInfo.put("name", currentLevel.getFormattedDisplayName()); // Lv2. 친환경 실천가 형식
        currentLevelInfo.put("description", getLevelDescription(currentLevel));
        currentLevelInfo.put("requiredPoints", currentLevel.getRequiredPoints());
        currentLevelInfo.put("icon", getLevelIcon(currentLevel));
        currentLevelInfo.put("color", getLevelColor(currentLevel));
        response.put("currentLevel", currentLevelInfo);
        
        // 다음 레벨 정보
        if (nextLevel != null) {
            Map<String, Object> nextLevelInfo = new HashMap<>();
            nextLevelInfo.put("id", nextLevel.name().toLowerCase());
            nextLevelInfo.put("name", nextLevel.getFormattedDisplayName()); // Lv3. 친환경 전문가 형식
            nextLevelInfo.put("description", getLevelDescription(nextLevel));
            nextLevelInfo.put("requiredPoints", nextLevel.getMinPoints());
            nextLevelInfo.put("icon", getLevelIcon(nextLevel));
            nextLevelInfo.put("color", getLevelColor(nextLevel));
            response.put("nextLevel", nextLevelInfo);
        } else {
            // 최고 레벨인 경우
            Map<String, Object> nextLevelInfo = new HashMap<>();
            nextLevelInfo.put("id", currentLevel.name().toLowerCase());
            nextLevelInfo.put("name", currentLevel.getFormattedDisplayName()); // Lv3. 친환경 전문가 형식
            nextLevelInfo.put("description", "최고 레벨에 도달했습니다! 🌟");
            nextLevelInfo.put("requiredPoints", currentLevel.getMinPoints());
            nextLevelInfo.put("icon", getLevelIcon(currentLevel));
            nextLevelInfo.put("color", getLevelColor(currentLevel));
            response.put("nextLevel", nextLevelInfo);
        }
        
        response.put("progressToNextLevel", progressToNextLevel);
        response.put("pointsToNextLevel", nextLevel != null ? Math.max(0, nextLevel.getMinPoints() - currentPoints) : 0L);
        
        return response;
    }

    private MemberProfile.EcoLevel calculateCurrentLevel(long points) {
        if (points >= MemberProfile.EcoLevel.EXPERT.getMinPoints()) {
            return MemberProfile.EcoLevel.EXPERT;
        } else if (points >= MemberProfile.EcoLevel.INTERMEDIATE.getMinPoints()) {
            return MemberProfile.EcoLevel.INTERMEDIATE;
        } else {
            return MemberProfile.EcoLevel.BEGINNER;
        }
    }

    private MemberProfile.EcoLevel getNextLevel(MemberProfile.EcoLevel currentLevel) {
        switch (currentLevel) {
            case BEGINNER:
                return MemberProfile.EcoLevel.INTERMEDIATE;
            case INTERMEDIATE:
                return MemberProfile.EcoLevel.EXPERT;
            case EXPERT:
                return null; // 최고 레벨
            default:
                return MemberProfile.EcoLevel.INTERMEDIATE;
        }
    }

    private String getLevelDescription(MemberProfile.EcoLevel level) {
        switch (level) {
            case BEGINNER:
                return "환경 보호 여정을 시작했어요!";
            case INTERMEDIATE:
                return "환경 보호를 실천하고 있어요!";
            case EXPERT:
                return "환경 보호의 전문가가 되었어요!";
            default:
                return "환경 보호 여정을 시작했어요!";
        }
    }

    private String getLevelIcon(MemberProfile.EcoLevel level) {
        switch (level) {
            case BEGINNER:
                return "🌱";
            case INTERMEDIATE:
                return "🌿";
            case EXPERT:
                return "🌳";
            default:
                return "🌱";
        }
    }

    private String getLevelColor(MemberProfile.EcoLevel level) {
        switch (level) {
            case BEGINNER:
                return "#10B981";
            case INTERMEDIATE:
                return "#059669";
            case EXPERT:
                return "#047857";
            default:
                return "#10B981";
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCalendarData(int year, int month) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 해당 월의 시작일과 종료일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // 해당 월의 모든 거래 내역 조회 (EARN 타입만)
        List<PointTransaction> transactions = pointTransactionRepository
                .findByMember_MemberIdAndPointTransactionTypeAndOccurredAtBetween(
                        memberId, 
                        PointTransactionType.EARN, 
                        startDate.atStartOfDay(), 
                        endDate.atTime(23, 59, 59)
                );
        
        // 일자별 씨앗 획득량 계산
        Map<Integer, Long> dailyEarnings = new HashMap<>();
        for (PointTransaction transaction : transactions) {
            int day = transaction.getOccurredAt().getDayOfMonth();
            dailyEarnings.merge(day, transaction.getPointsAmount().longValue(), Long::sum);
        }
        
        // 해당 월의 총 획득 씨앗 계산
        Long totalMonthlyEarnings = dailyEarnings.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        
        // 달력 정보 구성
        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("month", month);
        response.put("totalEarnings", totalMonthlyEarnings);
        response.put("dailyEarnings", dailyEarnings);
        
        log.info("달력 데이터 조회 완료 - 총 획득 씨앗: {}, 일자별 데이터: {}", totalMonthlyEarnings, dailyEarnings);
        
        return response;
    }

    @Transactional(readOnly = true)
    public Long getUserTotalSeeds(Long userId) {
        try {
            Long totalEarned = pointTransactionRepository.sumEarnedPointsByMemberId(userId);
            return totalEarned != null ? totalEarned : 0L;
        } catch (Exception e) {
            log.error("사용자 총 원큐씨앗 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            return 0L;
        }
    }

    private void updateTeamPoints(Long memberId, Long points) {
        try {
            // 회원이 속한 팀 조회
            var memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(memberId);
            if (memberTeam.isPresent()) {
                Team team = memberTeam.get().getTeam();
                
                // 팀 포인트 업데이트
                team.addPoints(points);
                teamRepository.save(team);
                
                log.info("팀 포인트 동기화 완료: 팀ID={}, 추가포인트={}", team.getId(), points);
            }
        } catch (Exception e) {
            log.error("팀 포인트 동기화 실패: memberId={}, points={}, error={}", memberId, points, e.getMessage());
            // 팀 포인트 동기화 실패는 전체 트랜잭션을 롤백하지 않음 (회원 포인트는 정상 처리)
        }
    }

}