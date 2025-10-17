package com.kopo.hanagreenworld.scheduler;

import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyDataResetScheduler {

    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileService memberProfileService;

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void resetMonthlyData() {

        try {
            // 모든 활성 사용자 프로필 조회
            List<Long> allMemberIds = memberProfileRepository.findAll()
                    .stream()
                    .map(profile -> profile.getMember().getMemberId())
                    .toList();

            int successCount = 0;
            int failCount = 0;
            
            // 각 사용자의 이번달 데이터 초기화
            for (Long memberId : allMemberIds) {
                try {
                    memberProfileService.resetCurrentMonthData(memberId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                }
            }

        } catch (Exception e) {
            log.error("월간 데이터 초기화 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
