package com.kopo.hanagreenworld.common.config;

import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.repository.ChallengeRecordRepository;
import com.kopo.hanagreenworld.activity.repository.ChallengeRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.domain.MemberStatus;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.point.domain.PointTransactionType;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DevConfig {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeRecordRepository challengeRecordRepository;
    private final PasswordEncoder passwordEncoder;

    // @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            // 테스트 계정
            if (!memberRepository.existsByLoginId("testuser")) {
                Member testMember = Member.builder()
                        .loginId("testuser")
                        .email("test@hana.com")
                        .password("test1234!")
                        .name("테스트 사용자")
                        .phoneNumber("010-1234-5678")
                        .role(Member.MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build();

                testMember.encodePassword(passwordEncoder);
                Member savedMember = memberRepository.save(testMember);

                MemberProfile profile = MemberProfile.builder()
                        .member(savedMember)
                        .nickname(savedMember.getName())
                        .build();
                memberProfileRepository.save(profile);

                profile.updateCurrentPoints(345L);
                memberProfileRepository.save(profile);
                
                log.info("개발용 테스트 계정이 생성되었습니다: testuser / test1234!");
            }

            // 관리자 계정
            if (!memberRepository.existsByLoginId("admin")) {
                Member adminMember = Member.builder()
                        .loginId("admin")
                        .email("admin@hana.com")
                        .password("admin1234!")
                        .name("관리자")
                        .phoneNumber("010-9999-9999")
                        .role(Member.MemberRole.ADMIN)
                        .status(MemberStatus.ACTIVE)
                        .build();

                adminMember.encodePassword(passwordEncoder);
                Member savedAdminMember = memberRepository.save(adminMember);

                MemberProfile adminProfile = MemberProfile.builder()
                        .member(savedAdminMember)
                        .nickname(savedAdminMember.getName())
                        .build();
                memberProfileRepository.save(adminProfile);
                
                log.info("개발용 관리자 계정이 생성되었습니다: admin / admin1234!");
            }

            // 테스트용 원큐씨앗 데이터
            Member testMember = memberRepository.findByLoginId("testuser").orElse(null);
            if (testMember != null) {
                // 샘플 거래 내역 생성
                if (pointTransactionRepository.count() == 0) {
                    // 걷기로 적립
                    PointTransaction walkingTransaction = PointTransaction.builder()
                            .member(testMember)
                            .pointTransactionType(PointTransactionType.EARN)
                            .category(PointCategory.WALKING)
                            .description("10000걸음")
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
                            .description("환경 퀴즈 정답")
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
                            .description("친환경 챌린지 성공")
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
                            .description("하나머니로 전환")
                            .pointsAmount(20)
                            .balanceAfter(5L)
                            .occurredAt(LocalDateTime.now().minusHours(12))
                            .build();
                    pointTransactionRepository.save(conversionTransaction);

                    log.info("테스트용 원큐씨앗 데이터가 생성되었습니다.");
                }
                
                // 테스트용 챌린지 레코드 생성
                if (challengeRecordRepository.count() == 0) {
                    var challenges = challengeRepository.findAll();
                    if (!challenges.isEmpty()) {
                        var challenge = challenges.get(0);

                        ChallengeRecord challengeRecord = ChallengeRecord.builder()
                                .challenge(challenge)
                                .member(testMember)
                                .activityDate(LocalDateTime.now().minusDays(2))
                                .participationDate(LocalDateTime.now().minusDays(2))
                                .verificationStatus("PENDING")
                                .aiConfidence(0.95)
                                .aiExplanation("테스트용 챌린지 성공")
                                .build();

                        challengeRecord.approve(50, null, LocalDateTime.now().minusDays(2));
                        challengeRecordRepository.save(challengeRecord);
                        
                        log.info("테스트용 챌린지 레코드가 생성되었습니다: {}", challenge.getTitle());
                    } else {
                        log.warn("챌린지가 없어서 테스트용 챌린지 레코드를 생성할 수 없습니다.");
                    }
                }
            }
        };
    }
}