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

    Optional<MemberTeam> findByMember_MemberIdAndIsActiveTrue(Long memberId);

    @Query("SELECT COUNT(mt) FROM MemberTeam mt WHERE mt.team.id = :teamId AND mt.isActive = true")
    Integer countActiveMembersByTeamId(@Param("teamId") Long teamId);

    boolean existsByMember_MemberIdAndTeam_IdAndIsActiveTrue(Long memberId, Long teamId);

    long countByTeam_IdAndIsActiveTrue(Long teamId);

    Optional<MemberTeam> findByTeam_IdAndMember_MemberIdAndIsActiveTrue(Long teamId, Long memberId);

    Optional<MemberTeam> findByTeam_IdAndMember_MemberIdAndIsActiveFalse(Long teamId, Long memberId);

    List<MemberTeam> findByTeam_IdAndIsActiveTrueOrderByJoinedAtAsc(Long teamId);
}
