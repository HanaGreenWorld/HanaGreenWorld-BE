package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    /**
     * 팀 이름으로 팀 조회
     */
    Optional<Team> findByTeamName(String teamName);
    
    /**
     * 활성화된 팀들만 조회
     */
    List<Team> findByIsActiveTrue();
    
    /**
     * 팀장 ID로 팀 조회
     */
    Optional<Team> findByLeaderId(Long leaderId);
    
    /**
     * 팀 랭킹 조회 (월간 점수 기준)
     */
    @Query("""
        SELECT t FROM Team t 
        LEFT JOIN TeamScore ts ON t.id = ts.team.id 
        WHERE t.isActive = true 
        AND (ts.reportDate = :reportDate OR ts.reportDate IS NULL)
        ORDER BY COALESCE(ts.totalScore, 0) DESC
        """)
    List<Team> findTeamsByMonthlyRanking(@Param("reportDate") String reportDate);
    
    /**
     * 특정 팀의 월간 랭킹 조회
     */
    @Query("""
        SELECT COUNT(t) + 1 FROM Team t 
        LEFT JOIN TeamScore ts ON t.id = ts.team.id 
        WHERE t.isActive = true 
        AND (ts.reportDate = :reportDate OR ts.reportDate IS NULL)
        AND COALESCE(ts.totalScore, 0) > (
            SELECT COALESCE(ts2.totalScore, 0) 
            FROM TeamScore ts2 
            WHERE ts2.team.id = :teamId 
            AND ts2.reportDate = :reportDate
        )
        """)
    Integer findTeamRankByMonth(@Param("teamId") Long teamId, @Param("reportDate") String reportDate);
    
    /**
     * 팀원 수가 가장 많은 팀들 조회
     */
    @Query("""
        SELECT t FROM Team t 
        WHERE t.isActive = true 
        ORDER BY (
            SELECT COUNT(mt) FROM MemberTeam mt 
            WHERE mt.team.id = t.id 
            AND mt.isActive = true
        ) DESC
        """)
    List<Team> findTeamsByMemberCount();
    
    /**
     * 특정 사용자가 속한 팀 조회
     */
    @Query("""
        SELECT t FROM Team t 
        JOIN MemberTeam mt ON t.id = mt.team.id 
        WHERE mt.member.id = :memberId 
        AND mt.isActive = true 
        AND t.isActive = true
        """)
    Optional<Team> findByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 활성화된 팀들을 총 팀 포인트 순으로 조회
     */
    List<Team> findByIsActiveTrueOrderByTotalTeamPointsDesc();
}

