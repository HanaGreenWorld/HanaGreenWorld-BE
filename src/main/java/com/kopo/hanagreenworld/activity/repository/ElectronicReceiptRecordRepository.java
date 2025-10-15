package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.ElectronicReceiptRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ElectronicReceiptRecordRepository extends JpaRepository<ElectronicReceiptRecord, Long> {

    Page<ElectronicReceiptRecord> findByMember_MemberIdOrderByReceiptDateDesc(Long memberId, Pageable pageable);

    List<ElectronicReceiptRecord> findByMember_MemberIdOrderByReceiptDateDesc(Long memberId);

    @Query("SELECT e FROM ElectronicReceiptRecord e WHERE e.member.memberId = :memberId " +
           "AND e.receiptDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.receiptDate DESC")
    List<ElectronicReceiptRecord> findByMemberAndDateRange(
        @Param("memberId") Long memberId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long countByMember_MemberId(Long memberId);

    @Query("SELECT COALESCE(SUM(e.pointsEarned), 0) FROM ElectronicReceiptRecord e WHERE e.member.memberId = :memberId")
    Integer sumPointsByMemberId(@Param("memberId") Long memberId);
}

