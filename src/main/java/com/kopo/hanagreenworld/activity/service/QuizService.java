package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.Quiz;
import com.kopo.hanagreenworld.activity.domain.QuizRecord;
import com.kopo.hanagreenworld.activity.repository.QuizRepository;
import com.kopo.hanagreenworld.activity.repository.QuizRecordRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizRecordRepository quizRecordRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileService memberProfileService;
    private final EcoSeedService ecoSeedService;
    private final QuizGeneratorService quizGeneratorService;

    @Transactional(readOnly = true)
    public Quiz getDailyQuiz(Long memberId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);

        // 오늘 이미 퀴즈를 풀었는지 확인
        if (quizRecordRepository.existsByMember_MemberIdAndActivityDateBetween(memberId, today, tomorrow)) {
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_ATTEMPTED);
        }

        // 오늘의 일일 퀴즈 반환
        LocalDate todayDate = LocalDate.now();
        return quizRepository.findByQuizDate(todayDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_GENERATION_FAILED));
    }

    @Transactional(readOnly = true)
    public QuizRecord getTodayQuizResult(Long memberId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);

        return quizRecordRepository.findByMember_MemberIdAndActivityDateBetween(memberId, today, tomorrow)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_ATTEMPTED));
    }

    @Transactional
    public QuizRecord attemptQuiz(Long memberId, Long quizId, Integer selectedAnswer) {
        // 오늘 자정 시작과 끝 시간 계산
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        // 이미 퀴즈를 풀었는지 확인
        if (quizRecordRepository.existsByMember_MemberIdAndActivityDateBetween(memberId, today, tomorrow)) {
            throw new BusinessException(ErrorCode.QUIZ_ALREADY_ATTEMPTED);
        }

        // 퀴즈와 회원 정보 조회
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 퀴즈 기록 생성
        QuizRecord record = QuizRecord.builder()
                .member(member)
                .quiz(quiz)
                .selectedAnswer(selectedAnswer)
                .build();

        QuizRecord savedRecord = quizRecordRepository.save(record);

        // 정답인 경우 포인트 적립
        if (savedRecord.getIsCorrect()) {
            // 가중치 기반 랜덤 보상 생성 (5~10 매우 높음, 드물게 100~10000)
            int baseReward = generateWeightedReward();

            // 연속 정답 보너스 계산
            Integer streak = quizRecordRepository.getCurrentStreak(memberId);
            Integer bonusPoints = calculateBonusPoints(streak);

            int totalReward = baseReward + bonusPoints;
            savedRecord.updatePointsAwarded(totalReward);

            EcoSeedEarnRequest pointRequest = EcoSeedEarnRequest.builder()
                    .category(PointCategory.DAILY_QUIZ)
                    .pointsAmount(totalReward)
                    .description("환경 퀴즈 정답")
                    .build();

            ecoSeedService.earnEcoSeeds(pointRequest);
            
            // MemberProfile에 활동횟수 업데이트 (탄소절감량 제외)
            memberProfileService.updateMemberActivityWithoutCarbon(memberId);
        }

        return savedRecord;
    }

    // 5~10: 약 92% 확률, 100~1000: 약 6% 확률, 1000~10000: 약 2% 확률
    private int generateWeightedReward() {
        double r = Math.random();
        if (r < 0.92) {
            return 5 + (int)Math.floor(Math.random() * 6); // 5~10
        } else if (r < 0.98) {
            return 100 + (int)Math.floor(Math.random() * 901); // 100~1000
        } else {
            return 1000 + (int)Math.floor(Math.random() * 9001); // 1000~10000
        }
    }

    private Integer calculateBonusPoints(Integer streak) {
        if (streak == null || streak <= 1) return 0;
        if (streak <= 3) return 2;
        if (streak <= 7) return 5;
        return 10;
    }

    @Transactional(readOnly = true)
    public List<QuizRecord> getMemberQuizHistory(Long memberId) {
        return quizRecordRepository.findByMember_MemberIdOrderByActivityDateDesc(memberId);
    }

    @Transactional(readOnly = true)
    public Boolean hasParticipatedToday(Long memberId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        return quizRecordRepository.existsByMember_MemberIdAndActivityDateBetween(memberId, today, tomorrow);
    }
}
