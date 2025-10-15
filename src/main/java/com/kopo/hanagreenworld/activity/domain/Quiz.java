package com.kopo.hanagreenworld.activity.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Quiz extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "JSON", nullable = false)
    private String options;

    @Column(name = "correct_answer", nullable = false)
    private Integer correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "points_reward", nullable = false)
    private Integer pointsReward;

    @Column(name = "quiz_date")
    private LocalDate quizDate;

    @Column(name = "topic", length = 100)
    private String topic;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Builder
    public Quiz(String question, String options, Integer correctAnswer, String explanation, 
               Integer pointsReward, LocalDate quizDate, String topic, String difficulty) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.pointsReward = pointsReward;
        this.quizDate = quizDate;
        this.topic = topic;
        this.difficulty = difficulty;
    }
}