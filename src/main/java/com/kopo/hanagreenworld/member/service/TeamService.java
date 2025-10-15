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
import com.kopo.hanagreenworld.chat.service.TeamChatService;
import com.kopo.hanagreenworld.chat.domain.TeamChatMessage;
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
    private final TeamChatService teamChatService;

    public TeamResponse getMyTeam() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MemberTeam memberTeam = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        Team team = memberTeam.getTeam();
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());

        Member leader = memberRepository.findById(team.getLeaderId())
                .orElse(null);
        
        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        // 완료된 챌린지 수 계산
        Integer completedChallenges = challengeRecordRepository.countByMember_MemberIdAndVerificationStatus(
                currentMember.getMemberId(), "VERIFIED");

        boolean isLeader = team.isLeader(currentMember.getMemberId());

        TeamResponse response = TeamResponse.from(team, stats, leader, currentChallenge, completedChallenges);
        
        // 팀장 여부 설정
        response = TeamResponse.builder()
                .id(response.getId())
                .name(response.getName())
                .slogan(response.getSlogan())
                .completedChallenges(response.getCompletedChallenges())
                .rank(response.getRank())
                .members(response.getMembers())
                .owner(response.getOwner())
                .isLeader(isLeader)
                .createdAt(response.getCreatedAt())
                .inviteCode(response.getInviteCode())
                .currentChallenge(response.getCurrentChallenge())
                .totalSeeds(response.getTotalSeeds())
                .carbonSavedKg(response.getCarbonSavedKg())
                .stats(response.getStats())
                .build();

        return response;
    }

    public TeamRankingResponse getTeamRanking() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 상위 10개 팀 조회
        List<Team> topTeams = teamRepository.findTeamsByMonthlyRanking()
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<TeamRankingResponse.TopTeamResponse> topTeamResponses = topTeams.stream()
                .map(this::convertToTopTeamResponse)
                .collect(Collectors.toList());

        Optional<MemberTeam> myMemberTeamOpt = memberTeamRepository.findByMember_MemberIdAndIsActiveTrue(currentMember.getMemberId());
        
        TeamRankingResponse.TeamRankingInfo myTeamInfo = null;
        if (myMemberTeamOpt.isPresent()) {
            myTeamInfo = getMyTeamRankingInfo(myMemberTeamOpt.get().getTeam(), currentMonth);
        } else {
            myTeamInfo = TeamRankingResponse.TeamRankingInfo.builder()
                    .teamId(null)
                    .teamName("팀에 가입해주세요!")
                    .currentRank(null)
                    .previousRank(null)
                    .monthlyPoints(0L)
                    .totalPoints(0L)
                    .members(0)
                    .trend("none")
                    .rankChange(0)
                    .build();
        }

        // 전체 팀 수 조회
        Integer totalTeams = teamRepository.findByIsActiveTrue().size();

        return TeamRankingResponse.create(topTeamResponses, myTeamInfo, totalTeams);
    }

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
        
        // 팀 탈퇴 시스템 메시지 생성
        createTeamLeaveSystemMessage(memberTeam.getTeam(), currentMember);
    }

    public TeamResponse.TeamStatsResponse getTeamStats(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Long monthlyPoints = team.getCurrentTeamPoints();
        Long totalPoints = team.getTotalTeamPoints();
        Double carbonSavedKg = team.getTotalCarbonSaved();

        if (monthlyPoints == null || monthlyPoints == 0) {
            monthlyPoints = pointTransactionRepository.findMonthlyTeamPoints(teamId, currentMonth);
        }
        if (totalPoints == null || totalPoints == 0) {
            totalPoints = pointTransactionRepository.findTotalTeamPoints(teamId);
        }
        if (carbonSavedKg == null || carbonSavedKg == 0.0) {
            carbonSavedKg = challengeRecordRepository.calculateTeamCarbonSaved(teamId);
        }
        
        // 월간 탄소절감량 계산
        Double monthlyCarbonSaved = team.getCurrentCarbonSaved();
        if (monthlyCarbonSaved == null || monthlyCarbonSaved == 0.0) {
            monthlyCarbonSaved = calculateMonthlyCarbonSaved(teamId, currentMonth);
        }

        // 랭킹
        Integer monthlyRank = teamRepository.findTeamRankByCurrentPoints(teamId);
        Integer totalRank = teamRepository.findTeamRankByTotalPoints(teamId);
        
        // 활성 멤버 수
        Integer activeMembers = memberTeamRepository.countActiveMembersByTeamId(teamId);
        
        // 이번 달 완료된 챌린지 수
        Integer completedChallengesThisMonth = challengeRecordRepository.countTeamCompletedChallengesThisMonth(teamId, currentMonth);

        TeamResponse.TeamStatsResponse result = TeamResponse.TeamStatsResponse.builder()
                .monthlyPoints(monthlyPoints != null ? monthlyPoints : 0L)
                .totalPoints(totalPoints != null ? totalPoints : 0L)
                .monthlyRank(monthlyRank != null ? monthlyRank : 999)
                .totalRank(totalRank != null ? totalRank : 999)
                .carbonSavedKg(carbonSavedKg != null ? carbonSavedKg : 0.0)
                .monthlyCarbonSaved(monthlyCarbonSaved != null ? monthlyCarbonSaved : 0.0)
                .activeMembers(activeMembers)
                .completedChallengesThisMonth(completedChallengesThisMonth)
                .build();

        return result;
    }

    private TeamRankingResponse.TopTeamResponse convertToTopTeamResponse(Team team) {
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());

        Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
        String leaderName = leader != null ? leader.getName() : "알 수 없음";

        return TeamRankingResponse.TopTeamResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .slogan(team.getDescription())
                .rank(stats.getMonthlyRank())
                .totalPoints(stats.getTotalPoints())
                .monthlyPoints(stats.getMonthlyPoints())
                .members(stats.getActiveMembers())
                .leaderName(leaderName)
                .build();
    }

    private TeamRankingResponse.TeamRankingInfo getMyTeamRankingInfo(Team team, String currentMonth) {
        TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());
        
        // 이전 달 랭킹 조회
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

        // 팀장 정보 조회
        Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
        
        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        return TeamResponse.from(team, stats, leader, currentChallenge, 0);
    }

    public List<TeamResponse> getTeamList() {
        List<Team> teams = teamRepository.findByIsActiveTrueOrderByTotalTeamPointsDesc();
        
        return teams.stream().map(team -> {
            TeamResponse.TeamStatsResponse stats = getTeamStats(team.getId());

            // 팀장 정보 조회
            Member leader = memberRepository.findById(team.getLeaderId()).orElse(null);
            
            // 현재 진행 중인 챌린지 조회
            Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                    .findFirst()
                    .orElse(null);
            
            return TeamResponse.from(team, stats, leader, currentChallenge, 0);
        }).collect(Collectors.toList());
    }

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

        MemberTeam memberTeam = MemberTeam.builder()
                .member(currentMember)
                .team(savedTeam)
                .role(MemberTeam.TeamRole.LEADER)
                .build();

        memberTeamRepository.save(memberTeam);

        TeamResponse.TeamStatsResponse stats = getTeamStats(savedTeam.getId());

        // 현재 진행 중인 챌린지 조회
        Challenge currentChallenge = challengeRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElse(null);
        
        return TeamResponse.from(savedTeam, stats, currentMember, currentChallenge, 0);
    }

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
                            .status(req.getStatus().name())
                            .requestDate(req.getCreatedAt())
                            .processedAt(req.getProcessedAt())
                            .processedBy(processedByName)
                            .build();
                })
                .collect(Collectors.toList());
    }

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
                .build();

        teamJoinRequestRepository.save(joinRequest);
    }

    public List<JoinRequestResponse> getJoinRequests(Long teamId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

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
                            .userLevel(applicant.getMemberProfile().getEcoLevel().getLevelNumber())
                            .requestDate(req.getCreatedAt())
                            .status(req.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void handleJoinRequest(Long requestId, boolean approve) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        com.kopo.hanagreenworld.member.domain.TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        Team team = teamRepository.findById(joinRequest.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Member applicant = memberRepository.findById(joinRequest.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (approve) {
            // 이미 팀에 가입되어 있는지 확인
            boolean isAlreadyMember = memberTeamRepository.existsByMember_MemberIdAndTeam_IdAndIsActiveTrue(applicant.getMemberId(), team.getId());
            if (isAlreadyMember) {
                throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
            }

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

            createTeamJoinSystemMessage(team, applicant);
        } else {
            // 거절 처리
            joinRequest.reject(currentMember.getMemberId());
        }

        teamJoinRequestRepository.save(joinRequest);
    }

    @Transactional
    public void kickMember(Long teamId, Long memberId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        MemberTeam memberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        // 방장은 강퇴할 수 없음
        if (memberTeam.getRole() == MemberTeam.TeamRole.LEADER) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        // 팀에서 제거
        memberTeam.deactivate();
        memberTeamRepository.save(memberTeam);
        
        // 팀 강퇴 시스템 메시지 생성
        createTeamKickSystemMessage(team, memberTeam.getMember());
    }

    @Transactional
    public void leaveCurrentTeam() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

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
        
        // 팀 탈퇴 시스템 메시지 생성
        createTeamLeaveSystemMessage(team, currentMember);

        // 팀에서 탈퇴
        memberTeam.deactivate();
        memberTeamRepository.save(memberTeam);
    }

    @Transactional
    public void transferLeadership(Long teamId, Long newLeaderId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 방장 권한 확인
        if (!team.getLeaderId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        MemberTeam newLeaderMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, newLeaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        MemberTeam currentLeaderMemberTeam = memberTeamRepository.findByTeam_IdAndMember_MemberIdAndIsActiveTrue(teamId, currentMember.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_IN_TEAM));

        newLeaderMemberTeam.promoteToLeader();
        currentLeaderMemberTeam.demoteToMember();

        team.changeLeader(newLeaderId);

        memberTeamRepository.save(newLeaderMemberTeam);
        memberTeamRepository.save(currentLeaderMemberTeam);
        teamRepository.save(team);
    }

    public TeamMembersResponse getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

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
                        .email(mt.getMember().getEmail())
                        .role(mt.getRole().name())
                            .totalPoints(totalPoints != null ? totalPoints : 0L)
                            .monthlyPoints(monthlyPoints != null ? monthlyPoints : 0L)
                        .joinedAt(mt.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return TeamMembersResponse.builder()
                .teamId(teamId)
                .members(members)
                .totalCount(members.size())
                .build();
    }

    private void createTeamJoinSystemMessage(Team team, Member newMember) {
        try {
            // 시스템 메시지 생성
            TeamChatMessage systemMessage = TeamChatMessage.builder()
                    .team(team)
                    .sender(newMember)
                    .messageText(newMember.getName() + "님이 입장하셨습니다.")
                    .messageType(TeamChatMessage.MessageType.SYSTEM)
                    .build();
            
            // 메시지 저장
            teamChatService.saveSystemMessage(systemMessage);
        } catch (Exception e) {
            log.error("팀 가입 시스템 메시지 생성 실패: 팀 ID = {}, 새 멤버 = {}, 오류 = {}", 
                    team.getId(), newMember.getName(), e.getMessage());
        }
    }

    private void createTeamLeaveSystemMessage(Team team, Member leavingMember) {
        try {
            // 시스템 메시지 생성
            TeamChatMessage systemMessage = TeamChatMessage.builder()
                    .team(team)
                    .sender(leavingMember)
                    .messageText(leavingMember.getName() + "님이 탈퇴하셨습니다.")
                    .messageType(TeamChatMessage.MessageType.SYSTEM)
                    .build();

            teamChatService.saveSystemMessage(systemMessage);
        } catch (Exception e) {
            log.error("팀 탈퇴 시스템 메시지 생성 실패: 팀 ID = {}, 탈퇴 멤버 = {}, 오류 = {}", 
                    team.getId(), leavingMember.getName(), e.getMessage());
        }
    }

    private void createTeamKickSystemMessage(Team team, Member kickedMember) {
        try {
            // 시스템 메시지 생성
            TeamChatMessage systemMessage = TeamChatMessage.builder()
                    .team(team)
                    .sender(kickedMember)
                    .messageText(kickedMember.getName() + "님이 강퇴되었습니다.")
                    .messageType(TeamChatMessage.MessageType.SYSTEM)
                    .build();
            
            // 메시지 저장
            teamChatService.saveSystemMessage(systemMessage);
            
            log.info("팀 강퇴 시스템 메시지 생성 완료: 팀 ID = {}, 강퇴 멤버 = {}", team.getId(), kickedMember.getName());
        } catch (Exception e) {
            log.error("팀 강퇴 시스템 메시지 생성 실패: 팀 ID = {}, 강퇴 멤버 = {}, 오류 = {}", 
                    team.getId(), kickedMember.getName(), e.getMessage());
        }
    }

    private Double calculateMonthlyCarbonSaved(Long teamId, String reportDate) {
        try {
            return challengeRecordRepository.calculateTeamMonthlyCarbonSaved(teamId, reportDate);
        } catch (Exception e) {
            log.error("월간 탄소절감량 계산 실패: teamId={}, reportDate={}, error={}", 
                    teamId, reportDate, e.getMessage());
            return 0.0;
        }
    }
}
