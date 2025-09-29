package com.kopo.hanagreenworld.auth.repository;

import com.kopo.hanagreenworld.auth.domain.RefreshToken;
import com.kopo.hanagreenworld.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByMemberAndIsActiveTrue(Member member);
    
    Optional<RefreshToken> findByRefreshTokenAndIsActiveTrue(String refreshToken);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isActive = false WHERE rt.member = :member")
    void deactivateAllByMember(@Param("member") Member member);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isActive = false WHERE rt.refreshToken = :refreshToken")
    void deactivateByToken(@Param("refreshToken") String refreshToken);
}
