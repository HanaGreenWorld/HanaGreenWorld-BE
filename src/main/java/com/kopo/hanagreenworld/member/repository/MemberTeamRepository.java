package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.MemberTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberTeamRepository extends JpaRepository<MemberTeam, Long> {
    
    /**
     * 특정 사용자가 속한 활성 팀 조회
     */
    Optional<MemberTeam> findByMember_MemberIdAndIsActiveTrue(Long memberId);
    
    /**
     * 특정 팀의 모든 활성 멤버 조회
     */
    List<MemberTeam> findByTeam_IdAndIsActiveTrue(Long teamId);
    
    /**
     * 특정 팀의 멤버 수 조회
     */
    @Query("SELECT COUNT(mt) FROM MemberTeam mt WHERE mt.team.id = :teamId AND mt.isActive = true")
    Integer countActiveMembersByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 특정 사용자가 특정 팀에 속해있는지 확인
     */
    boolean existsByMember_MemberIdAndTeam_IdAndIsActiveTrue(Long memberId, Long teamId);

    /**
     * 특정 사용자와 특정 팀으로 활성 멤버십 확인
     */
    boolean existsByMemberAndTeamAndIsActiveTrue(com.kopo.hanagreenworld.member.domain.Member member, com.kopo.hanagreenworld.member.domain.Team team);
    
    /**
     * 특정 팀의 팀장 조회
     */
    @Query("SELECT mt FROM MemberTeam mt WHERE mt.team.id = :teamId AND mt.role = 'LEADER' AND mt.isActive = true")
    Optional<MemberTeam> findLeaderByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 특정 팀의 활성 멤버 수 조회
     */
    long countByTeam_IdAndIsActiveTrue(Long teamId);
    
    /**
     * 특정 팀과 멤버로 활성 멤버십 조회
     */
    Optional<MemberTeam> findByTeam_IdAndMember_MemberIdAndIsActiveTrue(Long teamId, Long memberId);

    /**
     * 특정 팀과 멤버로 비활성 멤버십 조회
     */
    Optional<MemberTeam> findByTeam_IdAndMember_MemberIdAndIsActiveFalse(Long teamId, Long memberId);
    
    /**
     * 특정 팀의 모든 활성 멤버를 가입 순으로 조회
     */
    List<MemberTeam> findByTeam_IdAndIsActiveTrueOrderByJoinedAtAsc(Long teamId);
}
