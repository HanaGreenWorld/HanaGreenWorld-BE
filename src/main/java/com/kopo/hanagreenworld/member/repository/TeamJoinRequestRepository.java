package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.TeamJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {
    
    // 특정 팀의 대기 중인 가입 신청 목록 조회
    List<TeamJoinRequest> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, TeamJoinRequest.RequestStatus status);
    
    // 특정 사용자의 특정 팀에 대한 대기 중인 신청이 있는지 확인
    Optional<TeamJoinRequest> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, TeamJoinRequest.RequestStatus status);
    
    // 특정 사용자의 모든 가입 신청 조회
    List<TeamJoinRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}








