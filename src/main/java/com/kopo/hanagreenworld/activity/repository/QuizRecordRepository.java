package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.QuizRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
    // 특정 회원의 특정 기간 퀴즈 참여 여부 확인
    boolean existsByMember_MemberIdAndActivityDateBetween(Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 특정 기간 퀴즈 결과 조회 (단일 결과)
    Optional<QuizRecord> findByMember_MemberIdAndActivityDateBetween(Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 특정 기간 퀴즈 결과 조회 (복수 결과)
    List<QuizRecord> findByMember_MemberIdAndActivityDateBetweenOrderByActivityDateDesc(Long memberId, LocalDateTime start, LocalDateTime end);
    
    // 특정 회원의 모든 퀴즈 기록 조회
    List<QuizRecord> findByMember_MemberIdOrderByActivityDateDesc(Long memberId);
    
    // 특정 회원의 연속 정답 횟수 조회
    @Query(value = """
        WITH RECURSIVE cte AS (
            SELECT quiz_record_id, activity_date, is_correct, 1 as streak
            FROM quiz_records
            WHERE member_id = ?1 AND is_correct = true
            AND activity_date = (
                SELECT MAX(activity_date)
                FROM quiz_records
                WHERE member_id = ?1 AND is_correct = true
            )
            UNION ALL
            SELECT qr.quiz_record_id, qr.activity_date, qr.is_correct, cte.streak + 1
            FROM quiz_records qr
            INNER JOIN cte ON DATE(qr.activity_date) = DATE(DATE_SUB(cte.activity_date, INTERVAL 1 DAY))
            WHERE qr.member_id = ?1 AND qr.is_correct = true
        )
        SELECT MAX(streak) FROM cte
    """, nativeQuery = true)
    Integer getCurrentStreak(Long memberId);
}
