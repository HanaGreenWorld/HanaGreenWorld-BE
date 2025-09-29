package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // 랜덤 퀴즈 조회
    @Query(value = "SELECT * FROM quizzes ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Quiz> findRandomQuiz();
}












