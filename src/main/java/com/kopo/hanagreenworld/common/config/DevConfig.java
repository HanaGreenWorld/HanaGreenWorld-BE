package com.kopo.hanagreenworld.common.config;

import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.point.domain.PointTransactionType;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("dev") // 개발 환경에서만 실행
public class DevConfig {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            // 개발용 테스트 계정이 없으면 생성
            if (!memberRepository.existsByLoginId("testuser")) {
                Member testMember = Member.builder()
                        .loginId("testuser")
                        .email("test@hana.com")
                        .password("test1234!")
                        .name("테스트 사용자")
                        .phoneNumber("010-1234-5678")
                        .role(Member.MemberRole.USER)
                        .status(Member.MemberStatus.ACTIVE)
                        .build();

                testMember.encodePassword(passwordEncoder);
                Member savedMember = memberRepository.save(testMember);
                
                // Member 저장 후 즉시 MemberProfile 생성
                MemberProfile profile = MemberProfile.builder()
                        .member(savedMember)
                        .nickname(savedMember.getName())
                        .build();
                memberProfileRepository.save(profile);
                
                // 초기 원큐씨앗 설정 (345개)
                profile.updateCurrentPoints(345L);
                memberProfileRepository.save(profile);
                
                log.info("개발용 테스트 계정이 생성되었습니다: testuser / test1234!");
            }

            // 관리자 계정도 생성
            if (!memberRepository.existsByLoginId("admin")) {
                Member adminMember = Member.builder()
                        .loginId("admin")
                        .email("admin@hana.com")
                        .password("admin1234!")
                        .name("관리자")
                        .phoneNumber("010-9999-9999")
                        .role(Member.MemberRole.ADMIN)
                        .status(Member.MemberStatus.ACTIVE)
                        .build();

                adminMember.encodePassword(passwordEncoder);
                Member savedAdminMember = memberRepository.save(adminMember);
                
                // 관리자 MemberProfile 생성
                MemberProfile adminProfile = MemberProfile.builder()
                        .member(savedAdminMember)
                        .nickname(savedAdminMember.getName())
                        .build();
                memberProfileRepository.save(adminProfile);
                
                log.info("개발용 관리자 계정이 생성되었습니다: admin / admin1234!");
            }

            // 테스트용 원큐씨앗 데이터 생성
            Member testMember = memberRepository.findByLoginId("testuser").orElse(null);
            if (testMember != null) {
                // 샘플 거래 내역 생성
                if (pointTransactionRepository.count() == 0) {
                    // 걷기로 적립
                    PointTransaction walkingTransaction = PointTransaction.builder()
                            .member(testMember)
                            .pointTransactionType(PointTransactionType.EARN)
                            .category(PointCategory.WALKING)
                            .description("10000걸음으로 원큐씨앗 적립")
                            .pointsAmount(10)
                            .balanceAfter(10L)
                            .occurredAt(LocalDateTime.now().minusDays(3))
                            .build();
                    pointTransactionRepository.save(walkingTransaction);

                    // 퀴즈로 적립
                    PointTransaction quizTransaction = PointTransaction.builder()
                            .member(testMember)
                            .pointTransactionType(PointTransactionType.EARN)
                            .category(PointCategory.DAILY_QUIZ)
                            .description("환경 퀴즈 완료로 원큐씨앗 적립")
                            .pointsAmount(5)
                            .balanceAfter(15L)
                            .occurredAt(LocalDateTime.now().minusDays(2))
                            .build();
                    pointTransactionRepository.save(quizTransaction);

                    // 챌린지로 적립
                    PointTransaction challengeTransaction = PointTransaction.builder()
                            .member(testMember)
                            .pointTransactionType(PointTransactionType.EARN)
                            .category(PointCategory.ECO_CHALLENGE)
                            .description("친환경 챌린지 완료로 원큐씨앗 적립")
                            .pointsAmount(10)
                            .balanceAfter(25L)
                            .occurredAt(LocalDateTime.now().minusDays(1))
                            .build();
                    pointTransactionRepository.save(challengeTransaction);

                    // 하나머니로 전환
                    PointTransaction conversionTransaction = PointTransaction.builder()
                            .member(testMember)
                            .pointTransactionType(PointTransactionType.CONVERT)
                            .category(PointCategory.HANA_MONEY_CONVERSION)
                            .description("하나머니로 전환: 20개")
                            .pointsAmount(20)
                            .balanceAfter(5L)
                            .occurredAt(LocalDateTime.now().minusHours(12))
                            .build();
                    pointTransactionRepository.save(conversionTransaction);

                    log.info("테스트용 원큐씨앗 데이터가 생성되었습니다.");
                }
            }
        };
    }
}