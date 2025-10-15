package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRecordRepository extends JpaRepository<ChallengeRecord, Long> {
    List<ChallengeRecord> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<ChallengeRecord> findByMember_MemberIdAndChallenge_Id(Long memberId, Long challengeId);
    boolean existsByMember_MemberIdAndChallenge_Id(Long memberId, Long challengeId);
    boolean existsByMember_MemberIdAndChallenge_IdAndCreatedAtBetween(Long memberId, Long challengeId, LocalDateTime startTime, LocalDateTime endTime);
    int countByMember_MemberIdAndVerificationStatus(Long memberId, String verificationStatus);
    
    // AI 검증 관련
    List<ChallengeRecord> findByVerificationStatus(String verificationStatus);
    List<ChallengeRecord> findByVerificationStatusOrderByCreatedAtDesc(String verificationStatus);
    
    // 환경 임팩트 계산용
    List<ChallengeRecord> findByMemberIdAndVerificationStatus(Long memberId, String verificationStatus);
    
    // 월간 챌린지 기록 조회 (모든 상태)
    List<ChallengeRecord> findByMember_MemberIdAndActivityDateBetween(Long memberId, 
        java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    
    // 월간 챌린지 기록 조회 (검증 상태별)
    List<ChallengeRecord> findByMember_MemberIdAndVerificationStatusAndActivityDateBetween(Long memberId, String verificationStatus, 
        java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    
    // 팀별 챌린지 참여 기록 조회
    List<ChallengeRecord> findByTeamId(Long teamId);
    
    // 행동 패턴 분석용 - 최근 제출 이력 조회
    List<ChallengeRecord> findByMember_MemberIdAndCreatedAtAfter(Long memberId, LocalDateTime since);
    
    // AI 성능 모니터링용 - 기간별 조회
    List<ChallengeRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<ChallengeRecord> findByCreatedAtAfter(LocalDateTime since);
    
    // 팀별 이번 달 완료된 챌린지 수 조회
    @Query("SELECT COUNT(cr) FROM ChallengeRecord cr " +
           "JOIN MemberTeam mt ON cr.member.memberId = mt.member.memberId " +
           "WHERE mt.team.id = :teamId AND mt.isActive = true " +
           "AND cr.verificationStatus = 'VERIFIED' " +
           "AND DATE_FORMAT(cr.activityDate, '%Y-%m') = :reportDate")
    Integer countTeamCompletedChallengesThisMonth(@Param("teamId") Long teamId, @Param("reportDate") String reportDate);
    
    // 팀별 탄소 절감량 계산
    @Query("SELECT COALESCE(SUM(c.carbonSaved), 0) FROM ChallengeRecord cr " +
           "JOIN cr.challenge c " +
           "JOIN MemberTeam mt ON cr.member.memberId = mt.member.memberId " +
           "WHERE mt.team.id = :teamId AND mt.isActive = true " +
           "AND cr.verificationStatus = 'VERIFIED'")
    Double calculateTeamCarbonSaved(@Param("teamId") Long teamId);
    
    // 팀별 월간 탄소 절감량 계산
    @Query("SELECT COALESCE(SUM(c.carbonSaved), 0) FROM ChallengeRecord cr " +
           "JOIN cr.challenge c " +
           "JOIN MemberTeam mt ON cr.member.memberId = mt.member.memberId " +
           "WHERE mt.team.id = :teamId AND mt.isActive = true " +
           "AND cr.verificationStatus = 'VERIFIED' " +
           "AND DATE_FORMAT(cr.activityDate, '%Y-%m') = :reportDate")
    Double calculateTeamMonthlyCarbonSaved(@Param("teamId") Long teamId, @Param("reportDate") String reportDate);
}