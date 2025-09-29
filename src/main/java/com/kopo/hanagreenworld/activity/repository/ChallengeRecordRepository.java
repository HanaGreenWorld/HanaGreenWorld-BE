package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
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
}