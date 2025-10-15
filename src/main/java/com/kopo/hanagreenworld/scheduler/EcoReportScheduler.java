package com.kopo.hanagreenworld.scheduler;

import com.kopo.hanagreenworld.member.domain.EcoReport;
import com.kopo.hanagreenworld.member.service.EcoReportService;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EcoReportScheduler {

    private final EcoReportService ecoReportService;
    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 L * ?")
    @Transactional
    public void generateMonthlyReports() {

        try {
            // 이전 달 리포트 생성 (현재 날짜 기준)
            LocalDateTime now = LocalDateTime.now();
            String reportMonth = now.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // 모든 활성 사용자 조회
            List<Long> activeMemberIds = memberRepository.findActiveMemberIds();
            log.info("활성 사용자 수: {}", activeMemberIds.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Long memberId : activeMemberIds) {
                try {
                    EcoReport report = ecoReportService.generateMonthlyReport(memberId, reportMonth);
                    successCount++;
                    
                } catch (Exception e) {
                    failureCount++;
                }
            }
        } catch (Exception e) {
            log.error("월간 리포트 자동 생성 스케줄러 실패: {}", e.getMessage(), e);
        }
    }
}
