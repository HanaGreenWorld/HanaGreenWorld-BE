package com.kopo.hanagreenworld.member.domain;

import jakarta.persistence.*;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "team_scores",
    indexes = {
        @Index(name = "idx_team_score_month", columnList = "team_id, report_date"),
        @Index(name = "idx_team_score_total", columnList = "report_date, total_score DESC")
    }
)
@Getter
@NoArgsConstructor
public class TeamScore extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_score_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // 년월 (YYYY-MM 형식)
    @Column(name = "report_date", length = 7, nullable = false)
    private String reportDate;

    // 해당 월 누적 팀 점수
    @Column(name = "total_score", nullable = false)
    private Long totalScore = 0L;

    // 월간 팀 랭킹
    @Column(name = "monthly_rank")
    private Integer monthlyRank;

    // 랭킹 보상 포인트 (팀원 전원에게 지급)
    @Column(name = "ranking_points")
    private Integer rankingPoints;

    @Builder
    public TeamScore(Team team, String reportDate, Long totalScore,
                    Integer monthlyRank, Integer rankingPoints) {
        this.team = team;
        this.reportDate = reportDate;
        this.totalScore = totalScore == null ? 0L : totalScore;
        this.monthlyRank = monthlyRank;
        this.rankingPoints = rankingPoints;
    }

    public void addScore(Long score) {
        this.totalScore += score;
    }

    public void setRankAndPoints(Integer rank, Integer points) {
        this.monthlyRank = rank;
        this.rankingPoints = points;
    }
}