package com.kopo.hanagreenworld.merchant.service;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import com.kopo.hanagreenworld.merchant.domain.EcoMerchantTransaction;
import com.kopo.hanagreenworld.merchant.repository.EcoMerchantRepository;
import com.kopo.hanagreenworld.merchant.repository.EcoMerchantTransactionRepository;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import com.kopo.hanagreenworld.member.service.EcoReportService;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcoMerchantMatchingService {

    private final EcoMerchantRepository ecoMerchantRepository;
    private final EcoMerchantTransactionRepository ecoMerchantTransactionRepository;
    private final EcoSeedService ecoSeedService;
    private final EcoReportService ecoReportService;
    private final PointTransactionRepository pointTransactionRepository;
    private final MemberRepository memberRepository;

    public Optional<EcoMerchant> findEcoMerchantByBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return ecoMerchantRepository.findByBusinessNumberAndIsActiveTrue(businessNumber);
    }

    @Transactional
    public Map<String, Object> processEcoMerchantTransaction(Long userId, String businessNumber, 
                                                           String merchantName, Long amount, 
                                                           String transactionDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long cardTransactionId = System.currentTimeMillis();
            var existingTransaction = ecoMerchantTransactionRepository.findByCardTransactionId(cardTransactionId);
            
            if (existingTransaction.isPresent()) {
                log.info("이미 처리된 친환경 가맹점 거래입니다 - 사용자ID: {}, 카드거래ID: {}", userId, cardTransactionId);
                result.put("isEcoMerchant", false);
                result.put("message", "이미 처리된 거래");
                return result;
            }
            
            // 1. 사업자 번호로 친환경 가맹점 찾기
            Optional<EcoMerchant> ecoMerchantOpt = findEcoMerchantByBusinessNumber(businessNumber);
            
            if (ecoMerchantOpt.isEmpty()) {
                result.put("isEcoMerchant", false);
                result.put("message", "일반 가맹점");
                return result;
            }
            
            EcoMerchant ecoMerchant = ecoMerchantOpt.get();

            // 2. 사용자의 현재 레벨 조회
            String currentLevel = ecoReportService.getUserCurrentLevel(userId);

            // 3. 레벨에 따른 원큐씨앗 지급
            Long additionalSeeds = calculateEcoSeedsByLevel(currentLevel, amount);
            
            // 4. 원큐씨앗 지급 (웹훅용 메서드 사용)
            EcoSeedEarnRequest earnRequest = EcoSeedEarnRequest.builder()
                    .pointsAmount(additionalSeeds.intValue())
                    .category(PointCategory.ECO_MERCHANT)
                    .description(String.format("%s", ecoMerchant.getName()))
                    .build();
            
            ecoSeedService.earnEcoSeedsForWebhook(userId, earnRequest);
            
            // 5. 친환경 가맹점 거래 내역 저장
            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + userId));
            
            java.time.LocalDateTime parsedTransactionDate = java.time.LocalDateTime.parse(transactionDate);
            BigDecimal benefitRate = BigDecimal.valueOf(additionalSeeds).divide(BigDecimal.valueOf(amount), 4, java.math.RoundingMode.HALF_UP);
            
            EcoMerchantTransaction ecoTransaction = EcoMerchantTransaction.builder()
                    .member(member)
                    .ecoMerchant(ecoMerchant)
                    .cardTransactionId(cardTransactionId)
                    .merchantName(merchantName)
                    .businessNumber(businessNumber)
                    .transactionAmount(amount)
                    .transactionDate(parsedTransactionDate)
                    .category("친환경 가맹점")
                    .merchantCategory(ecoMerchant.getCategory().name())
                    .earnedSeeds(additionalSeeds)
                    .userLevel(currentLevel)
                    .benefitRate(benefitRate)
                    .isProcessed(true)
                    .build();
            
            ecoMerchantTransactionRepository.save(ecoTransaction);

            result.put("isEcoMerchant", true);
            result.put("merchantName", ecoMerchant.getName());
            result.put("category", ecoMerchant.getCategory().getDisplayName());
            result.put("additionalSeeds", additionalSeeds);
            result.put("totalSeeds", ecoSeedService.getUserTotalSeeds(userId));
            result.put("message", String.format("%s에서 친환경 가맹점 혜택으로 %d원큐씨앗 추가 지급!", 
                    ecoMerchant.getName(), additionalSeeds));

            return result;
            
        } catch (Exception e) {
            log.error("친환경 가맹점 매칭 처리 실패 - 사용자ID: {}, 사업자번호: {}, 에러: {}", 
                    userId, businessNumber, e.getMessage(), e);
            result.put("isEcoMerchant", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    public List<Map<String, Object>> getUserEcoMerchantHistory(Long userId) {

        try {
            List<EcoMerchantTransaction> transactions = ecoMerchantTransactionRepository
                .findByMember_MemberIdOrderByTransactionDateDesc(userId);
            
            List<Map<String, Object>> history = new ArrayList<>();
            
            for (EcoMerchantTransaction transaction : transactions) {
                Map<String, Object> historyItem = new HashMap<>();
                
                historyItem.put("merchantName", transaction.getMerchantName());
                historyItem.put("category", transaction.getEcoMerchant().getCategory().getDisplayName());
                historyItem.put("amount", transaction.getTransactionAmount());
                historyItem.put("additionalSeeds", transaction.getEarnedSeeds());
                historyItem.put("transactionDate", transaction.getTransactionDate().toLocalDate().toString());
                historyItem.put("businessNumber", transaction.getBusinessNumber());
                historyItem.put("userLevel", transaction.getUserLevel());
                historyItem.put("benefitRate", transaction.getBenefitRate());
                
                history.add(historyItem);
            }
            
            log.info("친환경 가맹점 이용 내역 조회 완료 - 사용자ID: {}, 건수: {}", userId, history.size());
            return history;
            
        } catch (Exception e) {
            log.error("친환경 가맹점 이용 내역 조회 실패 - 사용자ID: {}, 에러: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getEcoMerchantStats(Long userId) {
        try {
            Long totalAdditionalSeeds = ecoMerchantTransactionRepository.sumEarnedSeedsByMemberId(userId);
            Long totalEcoAmount = ecoMerchantTransactionRepository.sumTransactionAmountByMemberId(userId);
            Long uniqueMerchantCount = ecoMerchantTransactionRepository.countDistinctMerchantsByMemberId(userId);
            
            // 이번 달 통계
            Long currentMonthSeeds = ecoMerchantTransactionRepository.sumCurrentMonthEarnedSeedsByMemberId(userId);
            Long currentMonthAmount = ecoMerchantTransactionRepository.sumCurrentMonthTransactionAmountByMemberId(userId);
            Long currentMonthMerchants = ecoMerchantTransactionRepository.countCurrentMonthDistinctMerchantsByMemberId(userId);
            
            // 전체 거래 수
            List<EcoMerchantTransaction> allTransactions = ecoMerchantTransactionRepository
                .findByMember_MemberIdOrderByTransactionDateDesc(userId);
            long totalTransactions = allTransactions.size();
            
            long averageAdditionalSeeds = totalTransactions == 0 ? 0 : totalAdditionalSeeds / totalTransactions;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEcoTransactions", totalTransactions);
            stats.put("totalEcoAmount", totalEcoAmount);
            stats.put("totalAdditionalSeeds", totalAdditionalSeeds);
            stats.put("averageAdditionalSeeds", averageAdditionalSeeds);
            stats.put("ecoMerchantCount", uniqueMerchantCount);
            stats.put("currentMonthSeeds", currentMonthSeeds);
            stats.put("currentMonthAmount", currentMonthAmount);
            stats.put("currentMonthMerchants", currentMonthMerchants);
            
            log.info("친환경 가맹점 통계 조회 완료 - 사용자ID: {}, 총거래: {}, 총씨앗: {}, 총금액: {}", 
                    userId, totalTransactions, totalAdditionalSeeds, totalEcoAmount);
            
            return stats;
            
        } catch (Exception e) {
            log.error("친환경 가맹점 통계 조회 실패 - 사용자ID: {}, 에러: {}", userId, e.getMessage(), e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("totalEcoTransactions", 0);
            errorStats.put("totalEcoAmount", 0L);
            errorStats.put("totalAdditionalSeeds", 0L);
            errorStats.put("averageAdditionalSeeds", 0);
            errorStats.put("ecoMerchantCount", 0);
            errorStats.put("currentMonthSeeds", 0L);
            errorStats.put("currentMonthAmount", 0L);
            errorStats.put("currentMonthMerchants", 0L);
            return errorStats;
        }
    }

    private Long calculateEcoSeedsByLevel(String level, Long amount) {
        // 레벨별 원큐씨앗 지급 비율
        double rate = switch (level.toUpperCase()) {
            case "BEGINNER" -> 0.005;  // 0.5% (레벨 1)
            case "INTERMEDIATE" -> 0.01; // 1.0% (레벨 2)
            case "EXPERT" -> 0.02;     // 2.0% (레벨 3)
            default -> 0.005;          // 기본값 0.5%
        };
        
        Long ecoSeeds = Math.round(amount * rate);
        log.info("💰 레벨별 원큐씨앗 계산 - 레벨: {}, 거래금액: {}, 비율: {}%, 지급씨앗: {}", 
                level, amount, rate * 100, ecoSeeds);
        
        return ecoSeeds;
    }
}
