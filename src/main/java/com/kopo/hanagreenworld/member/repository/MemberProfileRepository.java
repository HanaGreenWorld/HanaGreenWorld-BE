package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
    
    Optional<MemberProfile> findByMember_MemberId(Long memberId);
}