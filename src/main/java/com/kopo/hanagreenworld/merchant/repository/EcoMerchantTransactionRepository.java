package com.kopo.hanagreenworld.merchant.repository;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchantTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcoMerchantTransactionRepository extends JpaRepository<EcoMerchantTransaction, Long> {

    // 회원별 친환경 가맹점 거래 내역 조회 (최신순)
    List<EcoMerchantTransaction> findByMember_MemberIdOrderByTransactionDateDesc(Long memberId);

    // 회원별 친환경 가맹점 거래 내역 조회 (페이징)
    Page<EcoMerchantTransaction> findByMember_MemberIdOrderByTransactionDateDesc(Long memberId, Pageable pageable);

    // 회원별 특정 기간 거래 내역 조회
    List<EcoMerchantTransaction> findByMember_MemberIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long memberId, LocalDateTime startDate, LocalDateTime endDate);

    // 카드 거래 ID로 조회 (중복 방지용)
    Optional<EcoMerchantTransaction> findByCardTransactionId(Long cardTransactionId);

    // 회원별 이번 달 거래 내역 조회
    @Query("SELECT emt FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId " +
           "AND YEAR(emt.transactionDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(emt.transactionDate) = MONTH(CURRENT_DATE) " +
           "ORDER BY emt.transactionDate DESC")
    List<EcoMerchantTransaction> findCurrentMonthTransactionsByMemberId(@Param("memberId") Long memberId);

    // 회원별 총 적립 원큐씨앗 합계
    @Query("SELECT COALESCE(SUM(emt.earnedSeeds), 0) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId AND emt.isProcessed = true")
    Long sumEarnedSeedsByMemberId(@Param("memberId") Long memberId);

    // 회원별 총 거래 금액 합계
    @Query("SELECT COALESCE(SUM(emt.transactionAmount), 0) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId AND emt.isProcessed = true")
    Long sumTransactionAmountByMemberId(@Param("memberId") Long memberId);

    // 회원별 이번 달 적립 원큐씨앗 합계
    @Query("SELECT COALESCE(SUM(emt.earnedSeeds), 0) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId " +
           "AND emt.isProcessed = true " +
           "AND YEAR(emt.transactionDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(emt.transactionDate) = MONTH(CURRENT_DATE)")
    Long sumCurrentMonthEarnedSeedsByMemberId(@Param("memberId") Long memberId);

    // 회원별 이번 달 거래 금액 합계
    @Query("SELECT COALESCE(SUM(emt.transactionAmount), 0) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId " +
           "AND emt.isProcessed = true " +
           "AND YEAR(emt.transactionDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(emt.transactionDate) = MONTH(CURRENT_DATE)")
    Long sumCurrentMonthTransactionAmountByMemberId(@Param("memberId") Long memberId);

    // 회원별 고유 가맹점 수
    @Query("SELECT COUNT(DISTINCT emt.businessNumber) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId AND emt.isProcessed = true")
    Long countDistinctMerchantsByMemberId(@Param("memberId") Long memberId);

    // 회원별 이번 달 고유 가맹점 수
    @Query("SELECT COUNT(DISTINCT emt.businessNumber) FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId " +
           "AND emt.isProcessed = true " +
           "AND YEAR(emt.transactionDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(emt.transactionDate) = MONTH(CURRENT_DATE)")
    Long countCurrentMonthDistinctMerchantsByMemberId(@Param("memberId") Long memberId);

    // 가맹점별 거래 통계
    @Query("SELECT emt.businessNumber, emt.merchantName, " +
           "COUNT(emt.id) as transactionCount, " +
           "SUM(emt.transactionAmount) as totalAmount, " +
           "SUM(emt.earnedSeeds) as totalSeeds " +
           "FROM EcoMerchantTransaction emt " +
           "WHERE emt.member.memberId = :memberId AND emt.isProcessed = true " +
           "GROUP BY emt.businessNumber, emt.merchantName " +
           "ORDER BY totalSeeds DESC")
    List<Object[]> getMerchantStatsByMemberId(@Param("memberId") Long memberId);
}
