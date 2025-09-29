package com.kopo.hanagreenworld.point.service;

import com.kopo.hanagreenworld.common.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
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
import com.kopo.hanagreenworld.integration.service.GroupIntegrationTokenService;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EcoSeedService {

    private final PointTransactionRepository pointTransactionRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberRepository memberRepository;
    private final HanamoneyCardService hanamoneyCardService;
    private final GroupIntegrationTokenService groupIntegrationTokenService;
    private final RestTemplate restTemplate;

    @Value("${integration.card.url}")
    private String hanacardApiBaseUrl;

    /**
     * 현재 사용자의 원큐씨앗 정보 조회
     */
    @Transactional
    public EcoSeedResponse getEcoSeedInfo() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        log.info("🔍 EcoSeedService.getEcoSeedInfo() - memberId: {}", memberId);
        
        if (memberId == null) {
            log.error("❌ memberId가 null입니다! 인증이 필요합니다.");
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

    /**
     * 원큐씨앗 적립 (트랜잭션으로 데이터 정합성 보장)
     */
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
            
            log.info("원큐씨앗 적립 완료: {} - {}개", memberId, request.getPointsAmount());
            
            return getEcoSeedInfo();
        } catch (Exception e) {
            log.error("원큐씨앗 적립 실패: {} - {}", memberId, e.getMessage());
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

    /**
     * 원큐씨앗 거래 내역 조회
     */
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

    /**
     * 특정 카테고리 거래 내역 조회
     */
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

    /**
     * 회원 프로필 생성 또는 조회
     */
    private MemberProfile getOrCreateMemberProfile(Long memberId) {
        log.info("🔍 getOrCreateMemberProfile 호출 - memberId: {}", memberId);
        
        if (memberId == null) {
            log.error("❌ memberId가 null입니다!");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return memberProfileRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> {
                    log.info("🔍 MemberProfile이 없음 - 새로 생성 시작");
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> {
                                log.error("❌ Member를 찾을 수 없습니다 - memberId: {}", memberId);
                                return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                            });
                    
                    MemberProfile profile = MemberProfile.builder()
                            .member(member)
                            .nickname(member.getName())
                            .build();
                    
                    return memberProfileRepository.save(profile);
                });
    }

    /**
     * 회원 프로필 정보 조회 (실시간 계산)
     */
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

    /**
     * 하나카드 서버에서 하나머니 잔액 조회
     */
    private Long getHanaMoneyFromCardServer(Long memberId) {
        try {
            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            
            // 하나카드 서버 API 호출을 위한 토큰 생성
            String groupCustomerToken = groupIntegrationTokenService.getGroupTokenByPhone(member.getPhoneNumber())
                    .orElseGet(() -> groupIntegrationTokenService.createGroupCustomerToken(
                            "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode(),
                            member.getName(),
                            member.getPhoneNumber(),
                            member.getEmail(),
                            "19900315" // Placeholder
                    ));
            String customerInfoToken = groupIntegrationTokenService.generateCustomerInfoToken(groupCustomerToken);
            String internalServiceToken = groupIntegrationTokenService.generateInternalServiceToken();
            
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

    /**
     * 사용자 통계 정보 조회 (레벨, 탄소 절약량 등)
     */
    @Transactional
    public Map<String, Object> getUserStats() {
        Long memberId = SecurityUtil.getCurrentMemberId();
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
        currentLevelInfo.put("name", currentLevel.getDisplayName());
        currentLevelInfo.put("description", getLevelDescription(currentLevel));
        currentLevelInfo.put("requiredPoints", currentLevel.getRequiredPoints());
        currentLevelInfo.put("icon", getLevelIcon(currentLevel));
        currentLevelInfo.put("color", getLevelColor(currentLevel));
        response.put("currentLevel", currentLevelInfo);
        
        // 다음 레벨 정보
        if (nextLevel != null) {
            Map<String, Object> nextLevelInfo = new HashMap<>();
            nextLevelInfo.put("id", nextLevel.name().toLowerCase());
            nextLevelInfo.put("name", nextLevel.getDisplayName());
            nextLevelInfo.put("description", getLevelDescription(nextLevel));
            nextLevelInfo.put("requiredPoints", nextLevel.getRequiredPoints());
            nextLevelInfo.put("icon", getLevelIcon(nextLevel));
            nextLevelInfo.put("color", getLevelColor(nextLevel));
            response.put("nextLevel", nextLevelInfo);
        } else {
            // 최고 레벨인 경우
            Map<String, Object> nextLevelInfo = new HashMap<>();
            nextLevelInfo.put("id", currentLevel.name().toLowerCase());
            nextLevelInfo.put("name", currentLevel.getDisplayName());
            nextLevelInfo.put("description", "최고 레벨에 도달했습니다! 🌟");
            nextLevelInfo.put("requiredPoints", currentLevel.getRequiredPoints());
            nextLevelInfo.put("icon", getLevelIcon(currentLevel));
            nextLevelInfo.put("color", getLevelColor(currentLevel));
            response.put("nextLevel", nextLevelInfo);
        }
        
        response.put("progressToNextLevel", progressToNextLevel);
        response.put("pointsToNextLevel", nextLevel != null ? Math.max(0, nextLevel.getMinPoints() - currentPoints) : 0L);
        
        return response;
    }
    
    /**
     * 포인트에 따른 현재 레벨 계산
     */
    private MemberProfile.EcoLevel calculateCurrentLevel(long points) {
        if (points >= MemberProfile.EcoLevel.EXPERT.getMinPoints()) {
            return MemberProfile.EcoLevel.EXPERT;
        } else if (points >= MemberProfile.EcoLevel.INTERMEDIATE.getMinPoints()) {
            return MemberProfile.EcoLevel.INTERMEDIATE;
        } else {
            return MemberProfile.EcoLevel.BEGINNER;
        }
    }
    
    /**
     * 다음 레벨 계산
     */
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
    
    /**
     * 레벨별 설명 반환
     */
    private String getLevelDescription(MemberProfile.EcoLevel level) {
        switch (level) {
            case BEGINNER:
                return "🌱 환경 보호 여정을 시작했어요!";
            case INTERMEDIATE:
                return "🌿 환경 보호를 실천하고 있어요!";
            case EXPERT:
                return "🌳 환경 보호의 전문가가 되었어요!";
            default:
                return "🌱 환경 보호 여정을 시작했어요!";
        }
    }
    
    /**
     * 레벨별 아이콘 반환
     */
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
    
    /**
     * 레벨별 색상 반환
     */
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
}