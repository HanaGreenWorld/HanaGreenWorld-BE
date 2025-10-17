package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.WalkingRecord;
import com.kopo.hanagreenworld.activity.dto.WalkingConsentRequest;
import com.kopo.hanagreenworld.activity.dto.WalkingConsentResponse;
import com.kopo.hanagreenworld.activity.dto.WalkingStepsRequest;
import com.kopo.hanagreenworld.activity.dto.WalkingResponse;
import com.kopo.hanagreenworld.activity.repository.WalkingRecordRepository;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingService {

    private final WalkingRecordRepository walkingRecordRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileService memberProfileService;
    private final EcoSeedService ecoSeedService;

    // 걷기 측정 동의 상태 조회
    @Transactional(readOnly = true)
    public WalkingConsentResponse getWalkingConsent(Long memberId) {
        MemberProfile profile = memberProfileRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return WalkingConsentResponse.builder()
                .isConsented(profile.getWalkingConsent())
                .consentedAt(profile.getWalkingConsentedAt() != null ? profile.getWalkingConsentedAt() : null)
                .lastSyncAt(profile.getWalkingLastSyncAt() != null ? profile.getWalkingLastSyncAt() : null)
                .dailyGoalSteps(profile.getWalkingDailyGoalSteps())
                .message("걷기 측정 동의 상태를 조회했습니다.")
                .build();
    }

    // 걷기 측정 동의 상태 업데이트
    @Transactional
    public WalkingConsentResponse updateWalkingConsent(Long memberId, WalkingConsentRequest request) {
        MemberProfile profile = memberProfileRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        profile.updateWalkingConsent(request.getIsConsented());
        if (request.getDailyGoalSteps() != null) {
            profile.updateWalkingDailyGoal(request.getDailyGoalSteps());
        }

        MemberProfile savedProfile = memberProfileRepository.save(profile);

        return WalkingConsentResponse.builder()
                .isConsented(savedProfile.getWalkingConsent())
                .consentedAt(savedProfile.getWalkingConsentedAt() != null ? savedProfile.getWalkingConsentedAt() : null)
                .lastSyncAt(savedProfile.getWalkingLastSyncAt() != null ? savedProfile.getWalkingLastSyncAt() : null)
                .dailyGoalSteps(savedProfile.getWalkingDailyGoalSteps())
                .message(request.getIsConsented() ? "걷기 측정에 동의했습니다." : "걷기 측정 동의를 취소했습니다.")
                .build();
    }

    // 걸음수 제출 및 포인트 적립
    @Transactional(rollbackFor = Exception.class)
    public WalkingResponse submitWalkingSteps(Long memberId, WalkingStepsRequest request) {
        try {
            // 동의 상태 확인
            MemberProfile profile = memberProfileRepository.findByMember_MemberId(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            if (!profile.getWalkingConsent()) {
                throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
            }

            // 날짜 파싱 (기본값: 오늘)
            LocalDate targetDate = request.getDate() != null ? 
                    LocalDate.parse(request.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
                    LocalDate.now();

            // 오늘 이미 제출했는지 확인
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

            if (walkingRecordRepository.existsByMember_MemberIdAndActivityDateBetween(memberId, startOfDay, endOfDay)) {
                throw new BusinessException(ErrorCode.ALREADY_SUBMITTED);
            }

            // 걸음수 유효성 검사
            if (request.getSteps() == null || request.getSteps() < 0) {
                throw new BusinessException(ErrorCode.INVALID_STEPS);
            }

            // 거리 계산 (평균 보폭 0.78m)
            BigDecimal distanceKm = BigDecimal.valueOf(request.getSteps() * 0.78 / 1000);
            
            // 탄소 절약량 계산 (1km당 0.21kg CO2 절약)
            BigDecimal carbonSaved = distanceKm.multiply(BigDecimal.valueOf(0.21));

            // 포인트 계산 (1000걸음당 1포인트, 최소 1포인트)
            int points = Math.max(1, request.getSteps() / 1000);

            // 1단계: WalkingRecord 생성 및 저장
            WalkingRecord walkingRecord = WalkingRecord.builder()
                    .member(profile.getMember())
                    .activityAmount((long) request.getSteps())
                    .carbonSaved(carbonSaved)
                    .pointsAwarded(points)
                    .activityDate(startOfDay)
                    .distanceKm(distanceKm)
                    .build();

            WalkingRecord savedRecord = walkingRecordRepository.save(walkingRecord);

            // 2단계: 포인트 적립 (실패 시 전체 롤백)
            EcoSeedEarnRequest pointRequest = EcoSeedEarnRequest.builder()
                    .category(PointCategory.WALKING)
                    .pointsAmount(points)
                    .description("일일 걷기 완료")
                    .build();

            ecoSeedService.earnEcoSeeds(pointRequest);

            // 3단계: MemberProfile에 탄소절감량과 활동횟수 업데이트
            memberProfileService.updateMemberActivityWithCarbon(memberId, carbonSaved.doubleValue());

            // 4단계: 동의 상태의 마지막 동기화 시간 업데이트
            profile.updateWalkingLastSync();
            memberProfileRepository.save(profile);

            // 모든 작업이 성공적으로 완료됨
            return WalkingResponse.builder()
                    .walkingId(savedRecord.getId())
                    .steps(request.getSteps())
                    .distanceKm(distanceKm)
                    .carbonSaved(carbonSaved)
                    .pointsAwarded(points)
                    .activityDate(startOfDay)
                    .message("걸음수가 성공적으로 제출되었습니다.")
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("걷기 기록 제출 중 예외 발생: memberId={}, steps={}", memberId, request.getSteps(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 오늘의 걷기 기록 조회
    @Transactional(readOnly = true)
    public WalkingResponse getTodayWalkingRecord(Long memberId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();

        WalkingRecord record = walkingRecordRepository
                .findByMember_MemberIdAndActivityDateBetween(memberId, startOfDay, endOfDay)
                .orElse(null);

        if (record == null) {
            return WalkingResponse.builder()
                    .message("오늘의 걷기 기록이 없습니다.")
                    .build();
        }

        return WalkingResponse.builder()
                .walkingId(record.getId())
                .steps(record.getActivityAmount().intValue())
                .distanceKm(record.getDistanceKm())
                .carbonSaved(record.getCarbonSaved())
                .pointsAwarded(record.getPointsAwarded())
                .activityDate(record.getActivityDate())
                .message("오늘의 걷기 기록을 조회했습니다.")
                .build();
    }

    // 월간 걷기 통계 조회
    @Transactional(readOnly = true)
    public Object[] getMonthlyWalkingStats(Long memberId, int year, int month) {
        LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.of(year, month + 1, 1).atStartOfDay();

        return walkingRecordRepository.getMonthlyStats(memberId, startOfMonth, endOfMonth);
    }

    // 연속 걷기 일수 조회
    @Transactional(readOnly = true)
    public Integer getWalkingStreak(Long memberId) {
        return walkingRecordRepository.getCurrentStreak(memberId);
    }

    // 최근 걷기 기록 조회 (최대 5개)
    @Transactional(readOnly = true)
    public List<WalkingResponse> getRecentWalkingRecords(Long memberId, int limit) {
        List<WalkingRecord> records = walkingRecordRepository
                .findTop5ByMember_MemberIdOrderByActivityDateDesc(memberId);

        return records.stream()
                .map(record -> WalkingResponse.builder()
                        .walkingId(record.getId())
                        .steps(record.getActivityAmount().intValue())
                        .distanceKm(record.getDistanceKm())
                        .carbonSaved(record.getCarbonSaved())
                        .pointsAwarded(record.getPointsAwarded())
                        .activityDate(record.getActivityDate())
                        .message("최근 걷기 기록을 조회했습니다.")
                        .build())
                .collect(Collectors.toList());
    }
}
