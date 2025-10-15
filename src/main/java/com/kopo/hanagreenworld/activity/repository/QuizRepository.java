package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // 랜덤 퀴즈 조회
    @Query(value = "SELECT * FROM quizzes ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Quiz> findRandomQuiz();
    
    // 특정 날짜의 일일 퀴즈 조회
    Optional<Quiz> findByQuizDate(LocalDate date);
    
    // 특정 날짜에 일일 퀴즈가 존재하는지 확인
    boolean existsByQuizDate(LocalDate date);
    
    // 오늘의 일일 퀴즈 조회
    @Query("SELECT q FROM Quiz q WHERE q.quizDate = :today")
    Optional<Quiz> findTodayQuiz(@Param("today") LocalDate today);
}
















