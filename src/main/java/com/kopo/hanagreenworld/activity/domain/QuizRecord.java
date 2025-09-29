package com.kopo.hanagreenworld.activity.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.member.domain.Member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_records")
@Getter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuizRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "selected_answer", nullable = false)
    private Integer selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

 	@Column(name = "points_awarded", nullable = false)
	private Integer pointsAwarded;

	@Column(name = "activity_date", nullable = false)
	private LocalDateTime activityDate;

    @Builder
    public QuizRecord(Member member, Quiz quiz, Integer selectedAnswer) {
        this.member = member;
        this.quiz = quiz;
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = selectedAnswer.equals(quiz.getCorrectAnswer());
        this.pointsAwarded = this.isCorrect ? quiz.getPointsReward() : 0;
        this.activityDate = LocalDateTime.now();
    }

    public void updatePointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }
}