package com.kopo.hanagreenworld.point.repository;

import com.kopo.hanagreenworld.point.domain.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    // 회원별 거래 내역 조회 (최신순)
    Page<PointTransaction> findByMember_MemberIdOrderByOccurredAtDesc(Long memberId, Pageable pageable);
    
    // 회원별 특정 카테고리 거래 내역 조회
    List<PointTransaction> findByMember_MemberIdAndCategoryOrderByOccurredAtDesc(Long memberId, String category);
    
    // 회원별 적립 내역 합계
    @Query("SELECT COALESCE(SUM(pt.pointsAmount), 0) FROM PointTransaction pt " +
        "WHERE pt.member.memberId = :memberId AND pt.pointTransactionType = 'EARN'")
    Long sumEarnedPointsByMemberId(@Param("memberId") Long memberId);
    
    // 회원별 사용 내역 합계
    @Query("SELECT COALESCE(SUM(pt.pointsAmount), 0) FROM PointTransaction pt " +
        "WHERE pt.member.memberId = :memberId AND pt.pointTransactionType = 'USE'")
    Long sumUsedPointsByMemberId(@Param("memberId") Long memberId);
    
    // 회원별 하나머니 전환 내역 합계 (CONVERT 타입)
    @Query("SELECT COALESCE(SUM(ABS(pt.pointsAmount)), 0) FROM PointTransaction pt " +
           "WHERE pt.member.memberId = :memberId AND pt.pointTransactionType = 'CONVERT'")
    Long sumConvertedPointsByMemberId(@Param("memberId") Long memberId);
    
    // 회원별 이번 달 적립 포인트 합계
    @Query("SELECT COALESCE(SUM(pt.pointsAmount), 0) FROM PointTransaction pt " +
           "WHERE pt.member.memberId = :memberId AND pt.pointTransactionType = 'EARN' " +
           "AND YEAR(pt.occurredAt) = YEAR(CURRENT_DATE) AND MONTH(pt.occurredAt) = MONTH(CURRENT_DATE)")
    Long sumCurrentMonthEarnedPointsByMemberId(@Param("memberId") Long memberId);
    
    // 팀별 월간 포인트 합계
    @Query("SELECT COALESCE(SUM(pt.pointsAmount), 0) FROM PointTransaction pt " +
           "JOIN MemberTeam mt ON pt.member.memberId = mt.member.memberId " +
           "WHERE mt.team.id = :teamId AND mt.isActive = true " +
           "AND pt.pointTransactionType = 'EARN' " +
           "AND DATE_FORMAT(pt.occurredAt, '%Y-%m') = :reportDate")
    Long findMonthlyTeamPoints(@Param("teamId") Long teamId, @Param("reportDate") String reportDate);
    
    // 팀별 총 포인트 합계
    @Query("SELECT COALESCE(SUM(pt.pointsAmount), 0) FROM PointTransaction pt " +
           "JOIN MemberTeam mt ON pt.member.memberId = mt.member.memberId " +
           "WHERE mt.team.id = :teamId AND mt.isActive = true " +
           "AND pt.pointTransactionType = 'EARN'")
    Long findTotalTeamPoints(@Param("teamId") Long teamId);
}