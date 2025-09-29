package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
        List<Challenge> findByIsActiveTrue();
}