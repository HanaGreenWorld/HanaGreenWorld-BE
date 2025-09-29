package com.kopo.hanagreenworld.activity.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.persistence.*;

import com.kopo.hanagreenworld.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "walking_records",
	indexes = {
		@Index(name = "idx_walking_member_date", columnList = "member_id, activity_date")
	}
)
@Getter
@NoArgsConstructor
public class WalkingRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "walking_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	// 공통 필드
	@Column(name = "activity_amount", nullable = false)
	private Long activityAmount; // 걸음수

	@Column(name = "carbon_saved", precision = 5, scale = 2, nullable = false)
	private BigDecimal carbonSaved; // kg

	@Column(name = "points_awarded", nullable = false)
	private Integer pointsAwarded;

	@Column(name = "activity_date", nullable = false)
	private LocalDateTime activityDate;

	// 걷기 전용 상세
	@Column(name = "distance_km", precision = 7, scale = 3)
	private BigDecimal distanceKm; // 선택

	@Builder
	public WalkingRecord(Member member, Long activityAmount, BigDecimal carbonSaved, Integer pointsAwarded,
	                     LocalDateTime activityDate, BigDecimal distanceKm) {
		this.member = member;
		this.activityAmount = activityAmount;
		this.carbonSaved = carbonSaved;
		this.pointsAwarded = pointsAwarded;
		this.activityDate = activityDate == null ? LocalDateTime.now() : activityDate;
		this.distanceKm = distanceKm;
	}

}