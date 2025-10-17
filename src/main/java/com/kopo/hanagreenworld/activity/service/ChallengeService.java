package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.dto.ChallengeListResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeDetailResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationRequest;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationResponse;
import com.kopo.hanagreenworld.activity.repository.ChallengeRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.Team;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.TeamRepository;
import com.kopo.hanagreenworld.member.repository.MemberTeamRepository;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final MemberTeamRepository memberTeamRepository;
    private final EcoSeedService ecoSeedService;
    private final AiVerificationService aiVerificationService;
    private final ImageMetadataService imageMetadataService;
    private final ImageHashService imageHashService;
    private final MemberProfileService memberProfileService;

    @Transactional(readOnly = true)
    public List<ChallengeListResponse> getActiveChallenges() {
        Long memberId = getCurrentMemberId();
        List<Challenge> challenges = challengeRepository.findByIsActiveTrue();
        
        // 챌린지 기간 유효성 검증 및 isActive 상태 업데이트
        challenges.forEach(challenge -> {
            challenge.validateAndUpdateActiveStatus();
        });
        
        // 기간이 유효한 챌린지만 필터링
        challenges = challenges.stream()
                .filter(Challenge::isCurrentlyActive)
                .collect(Collectors.toList());
        
        return challenges.stream()
                .map(challenge -> {
                    Boolean isParticipated = checkParticipation(memberId, challenge.getId());
                    String participationStatus = getParticipationStatus(memberId, challenge.getId());
                    return ChallengeListResponse.from(challenge, isParticipated, participationStatus);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Challenge getChallengeById(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ChallengeDetailResponse getChallengeDetail(Long challengeId, Long memberId) {
        Challenge challenge = getChallengeById(challengeId);
        
        // 챌린지 기간 유효성 검증 및 isActive 상태 업데이트
        challenge.validateAndUpdateActiveStatus();
        
        // 사용자 참여 상태 확인
        Boolean isParticipated = checkParticipation(memberId, challengeId);
        String participationStatus = getParticipationStatus(memberId, challengeId);
        
        // 실제 참여 완료 날짜 조회 (APPROVED/REJECTED일 때만)
        LocalDateTime participationDate = null;
        if ("APPROVED".equals(participationStatus) || "REJECTED".equals(participationStatus)) {
            ChallengeRecord record = challengeRecordRepository
                    .findByMember_MemberIdAndChallenge_Id(memberId, challengeId)
                    .orElse(null);
            if (record != null) {
                participationDate = record.getVerifiedAt();
            }
        }
        
        return ChallengeDetailResponse.from(challenge, isParticipated, participationStatus, participationDate);
    }

    @Transactional
    public ChallengeParticipationResponse participateInChallenge(Long memberId, Long challengeId, ChallengeParticipationRequest request) {
        Challenge challenge = getChallengeById(challengeId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 챌린지 기간 유효성 검증
        challenge.validateAndUpdateActiveStatus();
        if (!challenge.isCurrentlyActive()) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }
        
        if (!challenge.hasStarted()) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_STARTED);
        }

        // 팀장 전용 챌린지 권한 검증
        if (challenge.isLeaderOnlyChallenge()) {
            validateLeaderPermission(memberId, request.getTeamId());
        }

        // 이미지 해시 중복 검사는 AI 검증 시점에서만 수행
        // 여기서는 이미지 업로드만 허용

        // 오늘 이미 참여했는지 확인
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        boolean hasTodayParticipation = challengeRecordRepository
                .existsByMember_MemberIdAndChallenge_IdAndCreatedAtBetween(
                    memberId, challengeId, startOfDay, endOfDay);

        ChallengeRecord savedRecord;
        
        if (hasTodayParticipation) {
            // 이미 참여한 경우 기존 기록 조회 후 업데이트
            Optional<ChallengeRecord> existingRecord = challengeRecordRepository
                    .findByMember_MemberIdAndChallenge_Id(memberId, challengeId);
            
            if (existingRecord.isPresent()) {
                ChallengeRecord record = existingRecord.get();
                record.updateImageUrl(request.getImageUrl());
                // 이미지가 업로드되면 상태를 PENDING으로 변경
                if (request.getImageUrl() != null) {
                    record.updateVerificationStatus("PENDING");
                }
                if (request.getStepCount() != null) {
                    record.updateStepCount(request.getStepCount());
                }
                savedRecord = challengeRecordRepository.save(record);
            } else {
                // 새로운 참여 기록 생성
                String initialStatus = request.getImageUrl() != null ? "PENDING" : "PARTICIPATED";
                ChallengeRecord record = ChallengeRecord.builder()
                        .challenge(challenge)
                        .member(member)
                        .teamId(request.getTeamId())
                        .activityDate(LocalDateTime.now())
                        .participationDate(LocalDateTime.now())
                        .imageUrl(request.getImageUrl())
                        .stepCount(request.getStepCount())
                        .verificationStatus(initialStatus)
                        .build();

                savedRecord = challengeRecordRepository.save(record);
            }
        } else {
            String initialStatus = request.getImageUrl() != null ? "PENDING" : "PARTICIPATED";
            ChallengeRecord record = ChallengeRecord.builder()
                    .challenge(challenge)
                    .member(member)
                    .teamId(request.getTeamId())
                    .activityDate(LocalDateTime.now())
                    .participationDate(LocalDateTime.now())
                    .imageUrl(request.getImageUrl())
                    .stepCount(request.getStepCount())
                    .verificationStatus(initialStatus)
                    .build();

            savedRecord = challengeRecordRepository.save(record);
            log.info("새로운 챌린지 참여 기록 생성: challengeId={}, memberId={}, status={}", challengeId, memberId, initialStatus);
        }

        // 사진 업로드만 완료된 상태로 반환 (AI 검증은 별도 API에서)
        ChallengeParticipationResponse.ChallengeParticipationData data = 
                ChallengeParticipationResponse.ChallengeParticipationData.builder()
                .challengeRecordId(savedRecord.getId())
                .challengeTitle(challenge.getTitle())
                .verificationStatus("PENDING")
                .message("사진이 업로드되었습니다. '인증 완료' 버튼을 눌러 AI 검증을 시작하세요.")
                .pointsAwarded(null)
                .teamScoreAwarded(null)
                .build();
                
        return ChallengeParticipationResponse.builder()
                .success(true)
                .message("사진이 업로드되었습니다. '인증 완료' 버튼을 눌러 AI 검증을 시작하세요.")
                .data(data)
                .build();
    }

    @Transactional
    public ChallengeParticipationResponse startAiVerification(Long memberId, Long recordId) {

        ChallengeRecord record = challengeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 권한 확인
        if (!record.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 상태 확인 (PENDING 또는 PARTICIPATED 상태에서 AI 검증 가능)
        if (!"PENDING".equals(record.getVerificationStatus()) && !"PARTICIPATED".equals(record.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        Challenge challenge = record.getChallenge();
        String verificationStatus = "VERIFYING";
        String message = "AI가 사진을 검증하고 있습니다...";
        AiVerificationService.AiVerificationResult aiResult = null; // AI 검증 결과 초기화

        // 다층 검증 시스템 시작 (이미지가 있는 모든 챌린지에 대해)
        if (record.getImageUrl() != null) {
            long startTime = System.currentTimeMillis();

            // 이미지 메타데이터 검증
            ImageMetadataService.ImageMetadataResult metadataResult = 
                    imageMetadataService.validateImageMetadata(record.getImageUrl(), record.getParticipationDate());
            
            if (!metadataResult.isValid()) {
                record.needsReview(metadataResult.getConfidence(), 
                        "메타데이터 검증 실패: " + metadataResult.getReason(), "[]");
                challengeRecordRepository.save(record);
                return ChallengeParticipationResponse.builder()
                        .success(true)
                        .data(ChallengeParticipationResponse.ChallengeParticipationData.builder()
                                .challengeRecordId(record.getId())
                                .challengeTitle(challenge.getTitle())
                                .verificationStatus("NEEDS_REVIEW")
                                .message(metadataResult.getReason() + " 관리자 검토가 필요합니다.")
                                .build())
                        .build();
            }
            
            // 이미지 해시 중복 검사
            ImageHashService.ImageHashResult hashResult = imageHashService.checkImageDuplicate(
                record.getImageUrl(), memberId, challenge.getId());
            
            if (hashResult.isDuplicate()) {
                // 모든 중복 이미지를 거부 (사진 돌려쓰기 방지)
                record.updateVerificationStatus("REJECTED");
                record.updateAiVerification("REJECTED", 0.0, hashResult.getReason(), null);
                challengeRecordRepository.save(record);
                
                return ChallengeParticipationResponse.builder()
                        .success(true)
                        .data(ChallengeParticipationResponse.ChallengeParticipationData.builder()
                                .challengeRecordId(record.getId())
                                .challengeTitle(challenge.getTitle())
                                .verificationStatus("REJECTED")
                                .message(hashResult.getReason())
                                .pointsAwarded(null)
                                .teamScoreAwarded(null)
                                .build())
                        .build();
            }
            

            //AI 이미지 검증
            aiResult = aiVerificationService.verifyChallengeImage(
                    record.getImageUrl(),
                    challenge.getTitle(),
                    challenge.getCode().name()
            );

            long processingTime = System.currentTimeMillis() - startTime;

            if (aiResult.isSuccess()) {
                verificationStatus = aiResult.getVerificationResult();
                
                // AI 검증 정보 업데이트
                record.updateAiVerification(
                        verificationStatus,
                        aiResult.getConfidence(),
                        aiResult.getExplanation(),
                        aiResult.getDetectedItems()
                );

                // APPROVED: 자동 승인 및 포인트/팀점수 적립
                if ("APPROVED".equals(verificationStatus)) {
                    Integer pointsAwarded = challenge.getPoints();
                    Integer teamScoreAwarded = challenge.getTeamScore();
                    
                    record.approve(pointsAwarded, teamScoreAwarded, LocalDateTime.now());
                    
                    // 개인 챌린지인 경우 원큐씨앗 적립
                    if (challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.POINTS && pointsAwarded != null) {
                        EcoSeedEarnRequest earnRequest = EcoSeedEarnRequest.builder()
                                .category(PointCategory.ECO_CHALLENGE)
                                .pointsAmount(pointsAwarded)
                                .description(challenge.getTitle() + " 챌린지 성공")
                                .build();
                        ecoSeedService.earnEcoSeeds(earnRequest);
                        
                        // MemberProfile에 탄소절감량과 활동횟수 업데이트
                        memberProfileService.updateMemberActivityWithCarbon(
                            record.getMember().getMemberId(), 
                            challenge.getCarbonSaved()
                        );
                    }
                    
                    // 팀 챌린지인 경우 팀 점수 적립
                    if (challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.TEAM_SCORE && teamScoreAwarded != null && record.getTeamId() != null) {
                        // 팀 점수 적립 로직 (추후 구현)
                        log.info("팀 점수 적립: 팀 ID {}, 점수 {}", record.getTeamId(), teamScoreAwarded);
                    }
                    
                    // AI 검증 성공 후 이미지 해시 저장
                    if (record.getImageUrl() != null) {
                        imageHashService.saveImageHashAfterVerification(record.getImageUrl(), memberId, challenge.getId());
                    }
                    
                    // 챌린지 탄소절약량을 member_profiles에 업데이트
                    updateMemberCarbonSaved(record.getMember().getMemberId(), challenge.getCarbonSaved());
                    
                    // 팀 탄소절감량도 업데이트
                    updateTeamCarbonSaved(record.getMember().getMemberId(), challenge.getCarbonSaved());
                    
                    if (challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.TEAM_SCORE) {
                        message = String.format("챌린지 인증 성공! (신뢰도: %.1f%%) 팀에 %d점을 획득했습니다.",
                                aiResult.getConfidence() * 100, teamScoreAwarded);
                    } else {
                        message = String.format("챌린지 인증 성공! (신뢰도: %.1f%%) %d개의 원큐씨앗을 획득했습니다.",
                                aiResult.getConfidence() * 100, pointsAwarded);
                    }
                    
                } else if ("NEEDS_REVIEW".equals(verificationStatus)) {
                    // NEEDS_REVIEW: 관리자 검토 필요
                    message = String.format("관리자 검토 필요 (신뢰도: %.1f%%) - 검토 후 포인트가 적립됩니다.",
                            aiResult.getConfidence() * 100);
                    
                } else if ("REJECTED".equals(verificationStatus)) {
                    // REJECTED: 거부
                    record.reject(LocalDateTime.now());
                    message = String.format("챌린지 인증 실패 (신뢰도: %.1f%%) - 적절한 인증 사진을 다시 제출해주세요.",
                            aiResult.getConfidence() * 100);
                }
                
                challengeRecordRepository.save(record);
            } else {
                // AI 검증 실패 시 검토 필요 상태로 설정
                record.needsReview(0.0, "AI 검증 실패", "[]");
                challengeRecordRepository.save(record);
                verificationStatus = "NEEDS_REVIEW";
                message = "⚠검증 중 문제가 발생했습니다. 관리자가 확인할 예정입니다.";
            }
        } else {
            // 이미지가 없는 경우 자동 승인
            verificationStatus = "APPROVED";
            record.updateVerificationStatus("APPROVED");
            challengeRecordRepository.save(record);
            message = "챌린지가 완료되었습니다!";
        }

        // AI 검증 상세 정보 포함하여 응답 생성
        ChallengeParticipationResponse.ChallengeParticipationData data = 
                ChallengeParticipationResponse.ChallengeParticipationData.builder()
                .challengeRecordId(record.getId())
                .challengeTitle(challenge.getTitle())
                .verificationStatus(verificationStatus)
                .message(message)
                .pointsAwarded(record.getPointsAwarded())
                .teamScoreAwarded(record.getTeamScoreAwarded())
                .build();
        
        ChallengeParticipationResponse.ChallengeParticipationResponseBuilder responseBuilder = 
                ChallengeParticipationResponse.builder()
                .success(true)
                .message(message)
                .data(data);
        
        // AI 검증이 수행된 경우 상세 정보 추가
        if (aiResult != null) {
            responseBuilder
                .confidence(aiResult.getConfidence())
                .explanation(aiResult.getExplanation())
                .detectedItems(aiResult.getDetectedItems() != null ? 
                    String.join(", ", aiResult.getDetectedItems()) : null)
                .verifiedAt(record.getVerifiedAt() != null ? 
                    record.getVerifiedAt().toString() : null);
        }
        
        return responseBuilder.build();
    }

    @Transactional(readOnly = true)
    public List<ChallengeRecord> getMemberChallengeParticipations(Long memberId) {
        return challengeRecordRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public ChallengeRecord getMemberChallengeParticipation(Long memberId, Long challengeId) {
        return challengeRecordRepository.findByMember_MemberIdAndChallenge_Id(memberId, challengeId)
                .orElse(null);
    }

    private Boolean checkParticipation(Long memberId, Long challengeId) {
        return challengeRecordRepository.existsByMember_MemberIdAndChallenge_Id(memberId, challengeId);
    }
    
    private Boolean checkTodayParticipation(Long memberId, Long challengeId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        return challengeRecordRepository.existsByMember_MemberIdAndChallenge_IdAndCreatedAtBetween(
            memberId, challengeId, startOfDay, endOfDay);
    }

    private String getParticipationStatus(Long memberId, Long challengeId) {
        Optional<ChallengeRecord> record = challengeRecordRepository.findByMember_MemberIdAndChallenge_Id(memberId, challengeId);
        if (record.isPresent()) {
            return record.get().getVerificationStatus();
        }
        return "NOT_PARTICIPATED";
    }

    private Long getCurrentMemberId() {
        return SecurityUtil.getCurrentMemberId();
    }

    private void updateMemberCarbonSaved(Long memberId, Double carbonSaved) {
        if (carbonSaved != null && carbonSaved > 0) {
            try {
                memberProfileService.updateMemberCarbonSaved(memberId, carbonSaved);
            } catch (Exception e) {
                log.error("탄소절약량 업데이트 실패: memberId={}, carbonSaved={}, error={}",
                    memberId, carbonSaved, e.getMessage());
            }
        }
    }

    @Transactional
    public void adminApproveChallenge(Long recordId) {
        ChallengeRecord record = challengeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!"NEEDS_REVIEW".equals(record.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        Challenge challenge = record.getChallenge();
        Integer points = challenge.getPoints();
        
        // 승인 처리
        record.approve(points, null, LocalDateTime.now());
        challengeRecordRepository.save(record);

        // 포인트 적립
        if (points != null && points > 0) {
            EcoSeedEarnRequest earnRequest = EcoSeedEarnRequest.builder()
                    .category(PointCategory.ECO_CHALLENGE)
                    .pointsAmount(points)
                    .description(challenge.getTitle() + " 챌린지 성공 (관리자 승인)")
                    .build();
            ecoSeedService.earnEcoSeeds(earnRequest);
        }
        
        // 챌린지 탄소절약량을 member_profiles에 업데이트
        updateMemberCarbonSaved(record.getMember().getMemberId(), challenge.getCarbonSaved());
        
        // 팀 탄소절감량도 업데이트
        updateTeamCarbonSaved(record.getMember().getMemberId(), challenge.getCarbonSaved());
    }

    @Transactional
    public void adminRejectChallenge(Long recordId, String reason) {
        ChallengeRecord record = challengeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!"NEEDS_REVIEW".equals(record.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        // 거부 처리
        record.reject(LocalDateTime.now());
        
        // 거부 사유 업데이트
        if (reason != null && !reason.isEmpty()) {
            record.updateAiVerification(
                    "REJECTED",
                    record.getAiConfidence(),
                    reason,
                    record.getAiDetectedItems()
            );
        }
        
        challengeRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ChallengeRecord> getNeedsReviewChallenges() {
        return challengeRecordRepository.findByVerificationStatus("NEEDS_REVIEW");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getImageHashStats(Long memberId) {
        try {
            ImageHashService.ImageHashStats stats = imageHashService.getUserImageHashStats(memberId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("memberId", stats.getMemberId());
            result.put("totalImages", stats.getTotalImages());
            result.put("recentImageCount", stats.getRecentImageCount());
            result.put("lastImageDate", stats.getLastImageDate());
            
            return result;
        } catch (Exception e) {
            log.error("이미지 해시 통계 조회 실패: {}", e.getMessage(), e);
            return Map.of(
                "memberId", memberId,
                "totalImages", 0,
                "recentImageCount", 0,
                "error", e.getMessage()
            );
        }
    }

    private void validateLeaderPermission(Long memberId, Long teamId) {
        if (teamId == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 팀장인지 확인
        if (!team.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 팀에 속해있는지 확인
        memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));
    }

    public List<ChallengeRecord> getTeamChallengeParticipations(Long teamId) {
        log.info("팀 챌린지 참여 상태 조회: teamId={}", teamId);
        
        // 해당 팀의 모든 챌린지 참여 기록 조회
        List<ChallengeRecord> teamParticipations = challengeRecordRepository.findByTeamId(teamId);
        
        log.info("팀 챌린지 참여 기록 개수: {}", teamParticipations.size());
        
        return teamParticipations;
    }

    private void updateTeamCarbonSaved(Long memberId, Double carbonSaved) {
        if (carbonSaved == null || carbonSaved <= 0) {
            return;
        }
        
        try {
            // 회원이 속한 팀 조회
            var memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(memberId);
            if (memberTeam.isPresent()) {
                Team team = memberTeam.get().getTeam();
                
                // 팀 탄소절감량 업데이트
                team.addCarbonSaved(carbonSaved);
                teamRepository.save(team);
                
                log.info("팀 탄소절감량 업데이트 완료: 팀ID={}, 추가탄소절감량={}kg", team.getId(), carbonSaved);
            }
        } catch (Exception e) {
            log.error("팀 탄소절감량 업데이트 실패: memberId={}, carbonSaved={}, error={}", memberId, carbonSaved, e.getMessage());
            // 팀 탄소절감량 업데이트 실패는 전체 트랜잭션을 롤백하지 않음
        }
    }
}