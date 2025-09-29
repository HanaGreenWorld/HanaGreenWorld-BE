package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.dto.ChallengeListResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationRequest;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationResponse;
import com.kopo.hanagreenworld.activity.repository.ChallengeRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final MemberRepository memberRepository;
    private final EcoSeedService ecoSeedService;

    @Transactional(readOnly = true)
    public List<ChallengeListResponse> getActiveChallenges() {
        Long memberId = getCurrentMemberId();
        List<Challenge> challenges = challengeRepository.findByIsActiveTrue();
        
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

    @Transactional
    public ChallengeParticipationResponse participateInChallenge(Long memberId, Long challengeId, ChallengeParticipationRequest request) {
        Challenge challenge = getChallengeById(challengeId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 오늘 이미 참여했는지 확인 (일일 제한)
        if (checkTodayParticipation(memberId, challengeId)) {
            throw new BusinessException(ErrorCode.CHALLENGE_ALREADY_PARTICIPATED_TODAY);
        }

        // 챌린지 기록 생성
        ChallengeRecord record = ChallengeRecord.builder()
                .challenge(challenge)
                .member(member)
                .teamId(request.getTeamId())
                .imageUrl(request.getImageUrl())
                .stepCount(request.getStepCount())
                .verificationStatus("PENDING")
                .build();

        ChallengeRecord savedRecord = challengeRecordRepository.save(record);

        // 포인트 적립 (POINTS 정책인 경우)
        Integer pointsAwarded = null;
        if (challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.POINTS) {
            pointsAwarded = challenge.getPoints();
            // 원큐씨앗 적립
            EcoSeedEarnRequest earnRequest = EcoSeedEarnRequest.builder()
                    .category(PointCategory.ECO_CHALLENGE)
                    .pointsAmount(pointsAwarded)
                    .description(challenge.getTitle() + " 챌린지 완료로 원큐씨앗 적립")
                    .build();
            ecoSeedService.earnEcoSeeds(earnRequest);
            
            // 인증 상태를 VERIFIED로 변경하고 포인트 저장 (이미지 챌린지는 자동 승인)
            record.approve(pointsAwarded, null, LocalDateTime.now());
        }

        return ChallengeParticipationResponse.builder()
                .challengeRecordId(savedRecord.getId())
                .challengeTitle(challenge.getTitle())
                .verificationStatus(savedRecord.getVerificationStatus())
                .message(pointsAwarded != null ? 
                    "챌린지 참여가 완료되었습니다! " + pointsAwarded + "개의 원큐씨앗을 획득했습니다." :
                    "챌린지 참여가 완료되었습니다.")
                .pointsAwarded(pointsAwarded)
                .teamScoreAwarded(challenge.getRewardPolicy() == Challenge.ChallengeRewardPolicy.TEAM_SCORE ? challenge.getTeamScore() : null)
                .build();
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
        // SecurityUtil에서 현재 사용자 ID를 가져오는 로직
        // 실제 구현에서는 SecurityContext에서 가져와야 함
        return 1L; // 임시로 1L 반환
          }
  }