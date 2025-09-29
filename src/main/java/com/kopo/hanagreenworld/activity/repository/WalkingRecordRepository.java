package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.WalkingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalkingRecordRepository extends JpaRepository<WalkingRecord, Long> {
    
    // 특정 회원의 특정 날짜 걷기 기록 조회
    Optional<WalkingRecord> findByMember_MemberIdAndActivityDateBetween(
        Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 특정 기간 걷기 기록 목록 조회
    List<WalkingRecord> findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(
        Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 오늘 걷기 기록 존재 여부 확인
    boolean existsByMember_MemberIdAndActivityDateBetween(
        Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 월간 걷기 통계
    @Query("SELECT SUM(w.activityAmount), SUM(w.carbonSaved), SUM(w.pointsAwarded) " +
           "FROM WalkingRecord w " +
           "WHERE w.member.memberId = :memberId " +
           "AND w.activityDate BETWEEN :startDate AND :endDate")
    Object[] getMonthlyStats(Long memberId, LocalDateTime startDate, LocalDateTime endDate);
    
    // 특정 회원의 연속 걷기 일수
    @Query(value = """
        WITH RECURSIVE cte AS (
            SELECT activity_date, 1 as streak
            FROM walking_records
            WHERE member_id = :memberId
            AND activity_date = (
                SELECT MAX(activity_date)
                FROM walking_records
                WHERE member_id = :memberId
            )
            UNION ALL
            SELECT wr.activity_date, cte.streak + 1
            FROM walking_records wr
            INNER JOIN cte ON DATE(wr.activity_date) = DATE(DATE_SUB(cte.activity_date, INTERVAL 1 DAY))
            WHERE wr.member_id = :memberId
        )
        SELECT MAX(streak) FROM cte
    """, nativeQuery = true)
    Integer getCurrentStreak(Long memberId);
    
    // 특정 회원의 최근 걷기 기록 (최대 5개)
    List<WalkingRecord> findTop5ByMember_MemberIdOrderByActivityDateDesc(Long memberId);
}
