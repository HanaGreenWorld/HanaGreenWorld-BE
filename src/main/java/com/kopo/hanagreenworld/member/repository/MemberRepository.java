package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    Optional<Member> findByLoginId(String loginId);

    @Override
    Optional<Member> findById(Long aLong);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    Optional<Member> findByCi(String ci);

    boolean existsByLoginId(String loginId);
    
    boolean existsByEmail(String email);

    @Query("SELECT m.memberId FROM Member m WHERE m.status = 'ACTIVE'")
    List<Long> findActiveMemberIds();
}