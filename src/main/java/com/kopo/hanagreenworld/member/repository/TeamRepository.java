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

    Optional<Team> findByTeamName(String teamName);

    List<Team> findByIsActiveTrue();

    @Query("""
        SELECT t FROM Team t 
        WHERE t.isActive = true 
        ORDER BY t.currentTeamPoints DESC
        """)
    List<Team> findTeamsByMonthlyRanking();

    @Query("""
        SELECT COUNT(t) + 1 FROM Team t 
        WHERE t.isActive = true 
        AND t.currentTeamPoints > (
            SELECT t2.currentTeamPoints 
            FROM Team t2 
            WHERE t2.id = :teamId
        )
        """)
    Integer findTeamRankByCurrentPoints(@Param("teamId") Long teamId);

    List<Team> findByIsActiveTrueOrderByTotalTeamPointsDesc();

    @Query("""
        SELECT COUNT(t) + 1 FROM Team t 
        WHERE t.isActive = true 
        AND t.totalTeamPoints > (
            SELECT t2.totalTeamPoints 
            FROM Team t2 
            WHERE t2.id = :teamId
        )
        """)
    Integer findTeamRankByTotalPoints(@Param("teamId") Long teamId);
}

