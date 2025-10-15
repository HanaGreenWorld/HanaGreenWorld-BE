package com.kopo.hanagreenworld.member.repository;

import com.kopo.hanagreenworld.member.domain.EcoReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcoReportRepository extends JpaRepository<EcoReport, Long> {

    Optional<EcoReport> findByMember_MemberIdAndReportMonth(Long memberId, String reportMonth);

    List<EcoReport> findByMember_MemberIdOrderByReportMonthDesc(Long memberId);
}
