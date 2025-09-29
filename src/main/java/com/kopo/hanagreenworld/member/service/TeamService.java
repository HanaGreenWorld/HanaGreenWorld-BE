package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberTeam;
import com.kopo.hanagreenworld.member.domain.Team;
import com.kopo.hanagreenworld.member.dto.*;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberTeamRepository;
import com.kopo.hanagreenworld.member.repository.TeamRepository;
import com.kopo.hanagreenworld.member.repository.TeamJoinRequestRepository;
import com.kopo.hanagreenworld.point.domain.TeamPointTransaction;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.repository.ChallengeRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberTeamRepository memberTeamRepository;
    private final MemberRepository memberRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final TeamJoinRequestRepository teamJoinRequestRepository;

    /**
     * 현재 사용자의 팀 정보 조회
     */
    public TeamResponse getMyTeam() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MemberTeam memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        Team team = memberTeam.getTeam();
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(team.getId());
        
        // 팀장 정보 조회
        Member leader = memberRepository.findById(team.getLeaderId())
                .orElse(null);
        
        // 현재 진행 중인 챌린지 조회 (가장 최근 활성 챌린지)
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        // 완료된 챌린지 수 계산
        Integer completedChallenges = challengeRecordRepository.countByMember_MemberIdAndVerificationStatus(
                currentMember.getMemberId(), "VERIFIED");

        return TeamResponse.from(team, stats, emblems, leader, currentChallenge, completedChallenges);
    }

    /**
     * 초대코드로 팀 참여
     */
    @Transactional
    public TeamResponse joinTeamByInviteCode(String inviteCode) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 팀에 속해있는지 확인
        Optional<MemberTeam> existingTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId());
        if (existingTeam.isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 초대코드로 팀 조회 (GG-0001 형식)
        if (!inviteCode.startsWith("GG-")) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }
        
        String teamIdStr = inviteCode.substring(3);
        Long teamId;
        try {
            teamId = Long.parseLong(teamIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        if (!team.getIsActive()) {
            throw new BusinessException(ErrorCode.TEAM_NOT_ACTIVE);
        }

        // 팀원 수 확인
        long currentMemberCount = memberTeamRepository.countByTeam_IdAndIsActiveTrue(teamId);
        if (team.getMaxMembers() != null && currentMemberCount >= team.getMaxMembers()) {
            throw new BusinessException(ErrorCode.TEAM_FULL);
        }

        // 팀 참여
        MemberTeam memberTeam = MemberTeam.builder()
                .member(currentMember)
                .team(team)
                .role(MemberTeam.TeamRole.MEMBER)
                .build();

        memberTeamRepository.save(memberTeam);

        // 팀 정보 반환
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(team.getId());
        Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        Integer completedChallenges = challengeRecordRepository.countByMember_MemberIdAndVerificationStatus(
                currentMember.getMemberId(), "VERIFIED");

        return TeamResponse.from(team, stats, emblems, leader, currentChallenge, completedChallenges);
    }

    /**
     * 팀 랭킹 조회
     */
    public TeamRankingResponse getTeamRanking() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 상위 10개 팀 조회
        List<Team> topTeams = teamRepository.findTeamsByMonthlyRanking(currentMonth)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<TeamRankingResponse.TopTeamResponse> topTeamResponses = topTeams.stream()
                .map(this::convertToTopTeamResponse)
                .collect(Collectors.toList());

        // 내 팀 정보 조회
        MemberTeam myMemberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        TeamRankingResponse.TeamRankingInfo myTeamInfo = getMyTeamRankingInfo(myMemberTeam.getTeam(), currentMonth);

        // 전체 팀 수 조회
        Integer totalTeams = teamRepository.findByIsActiveTrue().size();

        return TeamRankingResponse.create(topTeamResponses, myTeamInfo, totalTeams);
    }

    /**
     * 팀 가입 (초대 코드로)
     */
    @Transactional
    public TeamResponse joinTeam(TeamJoinRequest request) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 팀에 속해있는지 확인
        if (memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 초대 코드로 팀 조회 (실제로는 초대 코드 테이블이 있어야 함)
        Long teamId = parseInviteCode(request.getInviteCode());
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 팀 가입
        MemberTeam memberTeam = MemberTeam.builder()
                .member(currentMember)
                .team(team)
                .role(MemberTeam.TeamRole.MEMBER)
                .build();

        memberTeamRepository.save(memberTeam);

        // 팀 가입 완료 (채팅은 팀이 활성화되면 자동으로 가능)

        // 팀 정보 반환
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(team.getId());

        // 팀장 정보 조회
        Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
        
        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        // 완료된 챌린지 수 계산
        Integer completedChallenges = challengeRecordRepository.countByMember_MemberIdAndVerificationStatus(
                currentMember.getMemberId(), "VERIFIED");

        return TeamResponse.from(team, stats, emblems, leader, currentChallenge, completedChallenges);
    }

    /**
     * 팀 탈퇴
     */
    @Transactional
    public void leaveTeam(Long teamId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MemberTeam memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        if (!memberTeam.getTeam().getId().equals(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        // 팀장은 탈퇴할 수 없음
        if (memberTeam.isLeader()) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        memberTeam.deactivate();
        memberTeamRepository.save(memberTeam);
    }

    /**
     * 팀 통계 조회
     */
    public TeamResponse.TeamStatsResponse getTeamStats(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 월간 점수 조회
        Long monthlyPoints = pointTransactionRepository.findMonthlyTeamPoints(teamId, currentMonth);
        
        // 총 점수 조회
        Long totalPoints = pointTransactionRepository.findTotalTeamPoints(teamId);
        
        // 월간 랭킹 조회
        Integer monthlyRank = teamRepository.findTeamRankByMonth(teamId, currentMonth);
        
        // 활성 멤버 수 조회
        Integer activeMembers = memberTeamRepository.countActiveMembersByTeamId(teamId);
        
        // 이번 달 완료된 챌린지 수 (TODO: 실제 챌린지 완료 수 조회)
        Integer completedChallengesThisMonth = 0;
        
        // 탄소 절감량 (TODO: 실제 탄소 절감량 계산)
        Long carbonSavedKg = totalPoints / 100L; // 임시 계산

        return TeamResponse.TeamStatsResponse.builder()
                .monthlyPoints(monthlyPoints != null ? monthlyPoints : 0L)
                .totalPoints(totalPoints != null ? totalPoints : 0L)
                .monthlyRank(monthlyRank != null ? monthlyRank : 999)
                .totalRank(monthlyRank) // TODO: 전체 랭킹 계산
                .carbonSavedKg(carbonSavedKg)
                .activeMembers(activeMembers)
                .completedChallengesThisMonth(completedChallengesThisMonth)
                .build();
    }

    /**
     * 팀 엠블럼 조회
     */
    private List<TeamResponse.EmblemResponse> getTeamEmblems(Long teamId) {
        // TODO: 실제 엠블럼 시스템 구현
        List<TeamResponse.EmblemResponse> emblems = new ArrayList<>();
        
        // 임시 엠블럼 데이터
        emblems.add(TeamResponse.EmblemResponse.builder()
                .id("emblem_1")
                .name("그린 스타터")
                .description("첫 번째 챌린지 완료")
                .iconUrl("/assets/emblems/green_starter.png")
                .isEarned(true)
                .earnedAt(java.time.LocalDateTime.now().minusDays(30))
                .build());
                
        emblems.add(TeamResponse.EmblemResponse.builder()
                .id("emblem_2")
                .name("걷기 마스터")
                .description("걷기 챌린지 10회 완료")
                .iconUrl("/assets/emblems/walking_master.png")
                .isEarned(true)
                .earnedAt(java.time.LocalDateTime.now().minusDays(15))
                .build());

        return emblems;
    }

    /**
     * 상위 팀 응답 변환
     */
    private TeamRankingResponse.TopTeamResponse convertToTopTeamResponse(Team team) {
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        
        return TeamRankingResponse.TopTeamResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .slogan(team.getDescription())
                .rank(stats.getMonthlyRank())
                .totalPoints(stats.getTotalPoints())
                .members(stats.getActiveMembers())
                .leaderName("그린리더") // TODO: 실제 팀장 이름 조회
                .emblemUrl("/assets/emblems/default.png")
                .build();
    }

    /**
     * 내 팀 랭킹 정보 조회
     */
    private TeamRankingResponse.TeamRankingInfo getMyTeamRankingInfo(Team team, String currentMonth) {
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        
        // 이전 달 랭킹 조회 (TODO: 실제 이전 달 랭킹 조회)
        Integer previousRank = stats.getMonthlyRank() + 1;
        String trend = "same";
        Integer rankChange = 0;
        
        if (previousRank < stats.getMonthlyRank()) {
            trend = "up";
            rankChange = previousRank - stats.getMonthlyRank();
        } else if (previousRank > stats.getMonthlyRank()) {
            trend = "down";
            rankChange = previousRank - stats.getMonthlyRank();
        }

        return TeamRankingResponse.TeamRankingInfo.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .currentRank(stats.getMonthlyRank())
                .previousRank(previousRank)
                .monthlyPoints(stats.getMonthlyPoints())
                .totalPoints(stats.getTotalPoints())
                .members(stats.getActiveMembers())
                .trend(trend)
                .rankChange(rankChange)
                .build();
    }

    /**
     * 초대코드 검증
     */
    public TeamResponse validateInviteCode(String inviteCode) {
        // 초대코드 파싱 (실제로는 초대코드 테이블에서 조회해야 함)
        Long teamId = parseInviteCode(inviteCode);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (!team.getIsActive()) {
            throw new BusinessException(ErrorCode.TEAM_NOT_ACTIVE);
        }

        // 팀 정보 반환 (가입 전 미리보기)
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(team.getId());
        
        // 팀장 정보 조회
        Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
        
        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        return TeamResponse.from(team, stats, emblems, leader, currentChallenge, 0);
    }

    /**
     * 팀 목록 조회
     */
    public List<TeamResponse> getTeamList() {
        List<Team> teams = teamRepository.findByIsActiveTrueOrderByTotalTeamPointsDesc();
        
        return teams.stream().map(team -> {
            TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
            List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(team.getId());
            
            // 팀장 정보 조회
            Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
            
            // 현재 진행 중인 챌린지 조회
            Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                    .findFirst()
                    .orElse(null);
            
            return TeamResponse.from(team, stats, emblems, leader, currentChallenge, 0);
        }).collect(Collectors.toList());
    }

    /**
     * 팀 생성
     */
    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 팀에 속해있는지 확인
        if (memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 팀 이름 중복 확인
        if (teamRepository.findByTeamName(request.getTeamName()).isPresent()) {
            throw new BusinessException(ErrorCode.TEAM_NAME_DUPLICATED);
        }

        // 팀 생성
        Team team = Team.builder()
                .teamName(request.getTeamName())
                .description(request.getDescription())
                .leaderId(currentMember.getMemberId())
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 20)
                .isActive(true)
                .build();

        Team savedTeam = teamRepository.save(team);

        // 팀장을 팀에 추가
        MemberTeam memberTeam = MemberTeam.builder()
                .member(currentMember)
                .team(savedTeam)
                .role(MemberTeam.TeamRole.LEADER)
                .build();

        memberTeamRepository.save(memberTeam);

        // 팀 생성 완료 (채팅은 팀이 활성화되면 자동으로 가능)

        // 팀 정보 반환
        TeamResponse.TeamStatsResponse stats = getTeamStats(savedTeam.getId());
        List<TeamResponse.EmblemResponse> emblems = getTeamEmblems(savedTeam.getId());
        
        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        return TeamResponse.from(savedTeam, stats, emblems, currentMember, currentChallenge, 0);
    }

    /**
     * 내 가입 신청 내역 조회
     */
    public List<MyJoinRequestResponse> getMyJoinRequests() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 내가 보낸 모든 가입 신청 조회
        List<com.kopo.hanagreenworld.member.domain.TeamJoinRequest> requests = teamJoinRequestRepository
                .findByUserIdOrderByCreatedAtDesc(currentMember.getMemberId());

        return requests.stream()
                .map(req -> {
                    Team team = teamRepository.findById(req.getTeamId()).orElse(null);
                    String processedByName = null;
                    if (req.getProcessedBy() != null) {
                        Member processor = memberRepository.findById(req.getProcessedBy()).orElse(null);
                        processedByName = processor != null ? processor.getName() : "알 수 없음";
                    }
                    
                    return MyJoinRequestResponse.builder()
                            .requestId(req.getId())
                            .teamId(req.getTeamId())
                            .teamName(team != null ? team.getTeamName() : "삭제된 팀")
                            .teamSlogan(team != null ? team.getDescription() : "")
                            .inviteCode(team != null ? ("GG-" + team.getId()) : "")
                            .message(req.getMessage())
                            .status(req.getStatus().name())
                            .requestDate(req.getCreatedAt())
                            .processedAt(req.getProcessedAt())
                            .processedBy(processedByName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 초대 코드 파싱 (임시 구현)
     */
    private Long parseInviteCode(String inviteCode) {
        // 실제로는 초대 코드 테이블에서 조회해야 함
        // 임시로 코드에서 팀 ID 추출
        try {
            String teamIdStr = inviteCode.substring(3); // "GG-" 제거
            return Long.parseLong(teamIdStr);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }
    }

    // === 팀 가입 신청 관련 메서드들 ===

    /**
     * 팀 가입 신청
     */
    @Transactional
    public void requestJoinTeam(TeamJoinRequest request) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 팀에 속해있는지 확인
        if (memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 초대코드로 팀 찾기
        Long teamId = parseInviteCode(request.getInviteCode());
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 이미 신청한 내역이 있는지 확인
        Optional<com.kopo.hanagreenworld.member.domain.TeamJoinRequest> existingRequest = teamJoinRequestRepository
                .findByTeamIdAndUserIdAndStatus(teamId, currentMember.getMemberId(), com.kopo.hanagreenworld.member.domain.TeamJoinRequest.RequestStatus.PENDING);
        
        if (existingRequest.isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }

        // 가입 신청 생성
        com.kopo.hanagreenworld.member.domain.TeamJoinRequest joinRequest = com.kopo.hanagreenworld.member.domain.TeamJoinRequest.builder()
                .teamId(teamId)
                .userId(currentMember.getMemberId())
                .message(request.getMessage())
                .build();

        teamJoinRequestRepository.save(joinRequest);
    }

    /**
     * 팀 가입 신청 목록 조회 (방장만 가능)
     */
    public List<JoinRequestResponse> getJoinRequests(Long teamId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 팀 존재 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 방장 권한 확인
        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 대기 중인 가입 신청 목록 조회
        List<com.kopo.hanagreenworld.member.domain.TeamJoinRequest> requests = teamJoinRequestRepository
                .findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, com.kopo.hanagreenworld.member.domain.TeamJoinRequest.RequestStatus.PENDING);

        return requests.stream()
                .map(req -> {
                    Member applicant = memberRepository.findById(req.getUserId()).orElse(null);
                    return JoinRequestResponse.builder()
                            .requestId(req.getId())
                            .userId(req.getUserId())
                            .userName(applicant != null ? applicant.getName() : "알 수 없음")
                            .userLevel(1) // TODO: 실제 레벨 구현
                            .requestDate(req.getCreatedAt())
                            .message(req.getMessage())
                            .status(req.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 가입 신청 승인/거절 처리 (방장만 가능)
     */
    @Transactional
    public void handleJoinRequest(Long requestId, boolean approve) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 가입 신청 조회
        com.kopo.hanagreenworld.member.domain.TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 팀 조회
        Team team = teamRepository.findById(joinRequest.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 방장 권한 확인
        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 신청자 조회
        Member applicant = memberRepository.findById(joinRequest.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (approve) {
            // 이미 팀에 가입되어 있는지 확인
            boolean isAlreadyMember = memberTeamRepository.existsByMember_MemberIdAndTeam_IdAndIsActiveTrue(applicant.getMemberId(), team.getId());
            if (isAlreadyMember) {
                throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
            }

            // 승인 처리
            joinRequest.approve(currentMember.getMemberId());

            // 팀에 멤버 추가 (이미 존재하는 경우는 업데이트)
            Optional<MemberTeam> existingMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(team.getId(), applicant.getMemberId());
            if (existingMemberTeam.isPresent()) {
                // 이미 활성 멤버십이 존재하면 업데이트
                MemberTeam memberTeam = existingMemberTeam.get();
                memberTeam.changeRole(MemberTeam.TeamRole.MEMBER);
                // 이미 활성 상태이므로 별도 설정 불필요
                memberTeamRepository.save(memberTeam);
            } else {
                // 활성 멤버십이 없으면 비활성 멤버십을 찾아서 재활성화
                Optional<MemberTeam> inactiveMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveFalse(team.getId(), applicant.getMemberId());
                if (inactiveMemberTeam.isPresent()) {
                // 탈퇴한 멤버십을 재활성화
                MemberTeam memberTeam = inactiveMemberTeam.get();
                memberTeam.activate();
                memberTeam.changeRole(MemberTeam.TeamRole.MEMBER);
                memberTeamRepository.save(memberTeam);
                } else {
                    // 완전히 새로운 멤버십 추가
                    MemberTeam memberTeam = MemberTeam.builder()
                            .member(applicant)
                            .team(team)
                            .role(MemberTeam.TeamRole.MEMBER)
                            .build();

                    memberTeamRepository.save(memberTeam);
                }
            }
        } else {
            // 거절 처리
            joinRequest.reject(currentMember.getMemberId());
        }

        teamJoinRequestRepository.save(joinRequest);
    }

    /**
     * 팀원 강퇴 (방장만 가능)
     */
    @Transactional
    public void kickMember(Long teamId, Long memberId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 방장 권한 확인
        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 강퇴할 멤버 조회
        MemberTeam memberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        // 방장은 강퇴할 수 없음
        if (memberTeam.getRole() == MemberTeam.TeamRole.LEADER) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        // 팀에서 제거
        memberTeam.deactivate();
        memberTeamRepository.save(memberTeam);
    }

    /**
     * 팀 탈퇴
     */
    @Transactional
    public void leaveCurrentTeam() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 현재 팀 조회
        MemberTeam memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_TEAM));

        Team team = memberTeam.getTeam();

        // 방장인 경우 특별 처리
        if (memberTeam.getRole() == MemberTeam.TeamRole.LEADER) {
            // 다른 멤버가 있는지 확인
            List<MemberTeam> activeMembers = memberTeamRepository.findByTeam_IdAndIsActiveTrueOrderByJoinedAtAsc(team.getId());
            
            if (activeMembers.size() > 1) {
                throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE_WITH_MEMBERS);
            }
            
            // 혼자인 경우 팀 비활성화
            team.deactivate();
            teamRepository.save(team);
        }

        // 팀에서 탈퇴
        memberTeam.deactivate();
        memberTeamRepository.save(memberTeam);
    }

    /**
     * 방장 권한 이양 (방장만 가능)
     */
    @Transactional
    public void transferLeadership(Long teamId, Long newLeaderId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 방장 권한 확인
        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 새 방장 멤버 조회
        MemberTeam newLeaderMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, newLeaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        // 현재 방장 멤버 조회
        MemberTeam currentLeaderMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        // 권한 변경
        newLeaderMemberTeam.promoteToLeader();
        currentLeaderMemberTeam.demoteToMember();
        
        // 팀의 방장 ID 변경
        team.changeLeader(newLeaderId);

        // 저장
        memberTeamRepository.save(newLeaderMemberTeam);
        memberTeamRepository.save(currentLeaderMemberTeam);
        teamRepository.save(team);
    }

    /**
     * 팀 멤버 목록 조회
     */
    public TeamMembersResponse getTeamMembers(Long teamId) {
        // 팀 존재 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 활성 멤버 조회
        List<MemberTeam> memberTeams = memberTeamRepository.findByTeam_IdAndIsActiveTrueOrderByJoinedAtAsc(teamId);

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<TeamMembersResponse.TeamMemberResponse> members = memberTeams.stream()
                .map(mt -> {
                    Long memberId = mt.getMember().getMemberId();
                    
                    // 실제 포인트 계산
                    Long totalPoints = pointTransactionRepository.sumEarnedPointsByMemberId(memberId);
                    Long monthlyPoints = pointTransactionRepository.sumCurrentMonthEarnedPointsByMemberId(memberId);
                    
                    return TeamMembersResponse.TeamMemberResponse.builder()
                            .id(memberId)
                        .name(mt.getMember().getName())
                        .email(mt.getMember().getLoginId() + "@example.com") // TODO: 실제 이메일 필드 추가
                        .role(mt.getRole().name())
                            .totalPoints(totalPoints != null ? totalPoints : 0L)
                            .monthlyPoints(monthlyPoints != null ? monthlyPoints : 0L)
                        .joinedAt(mt.getJoinedAt())
                        .profileImageUrl("") // TODO: 프로필 이미지 구현
                        .isOnline(false) // TODO: 온라인 상태 구현
                            .build();
                })
                .collect(Collectors.toList());

        return TeamMembersResponse.builder()
                .teamId(teamId)
                .members(members)
                .totalCount(members.size())
                .build();
    }
}
