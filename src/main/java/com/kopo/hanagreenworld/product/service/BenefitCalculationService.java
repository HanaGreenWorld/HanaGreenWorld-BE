package com.kopo.hanagreenworld.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import com.kopo.hanagreenworld.integration.service.CardTransactionIntegrationService;
import com.kopo.hanagreenworld.integration.dto.IntegratedCustomerInfoRequest;
import com.kopo.hanagreenworld.integration.dto.IntegratedCustomerInfoResponse;
import com.kopo.hanagreenworld.integration.dto.CardConsumptionSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenefitCalculationService {

    private final ObjectMapper objectMapper;
    private final GroupIntegrationService groupIntegrationService;
    private final CardTransactionIntegrationService cardTransactionIntegrationService;

    public String calculateFinancialBenefit(Long memberId, String currentLevel) {
        try {
            Map<String, Integer> benefit = new HashMap<>();
            
            // 실제 사용자 금융 데이터 조회
            FinancialData financialData = getUserFinancialData(memberId);
            
            long savingsBalance = financialData.savingsBalance;
            long loanBalance = financialData.loanBalance;
            long monthlyCardUsage = financialData.monthlyCardUsage;

            double savingsRate = 0.0;
            double loanRate = 0.0;
            double cardRate = 0.0;
            

            String levelType = extractLevelType(currentLevel);
            
            switch (levelType) {
                case "BEGINNER":
                    savingsRate = 0.5; // 0.5% 우대금리
                    loanRate = 0.5; // 0.5% 우대금리
                    cardRate = 1.0; // 1% 추가 적립
                    break;
                    
                case "INTERMEDIATE":
                    savingsRate = 1.0; // 1.0% 우대금리
                    loanRate = 1.0; // 1.0% 우대금리
                    cardRate = 3.0; // 3% 추가 적립
                    break;
                    
                case "EXPERT":
                    savingsRate = 2.0; // 2.0% 우대금리
                    loanRate = 2.0; // 2.0% 우대금리
                    cardRate = 5.0; // 5% 추가 적립
                    break;
                    
                default:
                    log.warn("알 수 없는 레벨: {} (추출된 타입: {})", currentLevel, levelType);
                    savingsRate = 0.5;
                    loanRate = 0.5;
                    cardRate = 1.0;
                    break;
            }

            int savingsBenefit = (int) (savingsBalance * savingsRate / 100 / 12);
            int loanBenefit = (int) (loanBalance * loanRate / 100 / 12);
            int cardBenefit = (int) (monthlyCardUsage * cardRate / 100);
            
            benefit.put("savingsInterest", savingsBenefit);
            benefit.put("loanBenefit", loanBenefit);
            benefit.put("cardDiscount", cardBenefit);
            
            int total = savingsBenefit + loanBenefit + cardBenefit;
            benefit.put("total", total);
            
            String result = objectMapper.writeValueAsString(benefit);
            return result;
            
        } catch (Exception e) {
            log.error("금융 혜택 계산 실패 - memberId: {}, currentLevel: {}, error: {}",
                     memberId, currentLevel, e.getMessage(), e);
            return "{\"savingsInterest\":0,\"cardDiscount\":0,\"loanBenefit\":0,\"total\":0}";
        }
    }

    private String extractLevelType(String levelString) {
        if (levelString == null || levelString.trim().isEmpty()) {
            return "BEGINNER";
        }
        
        // 레벨 번호 추출
        if (levelString.contains("Lv1") || levelString.contains("새내기")) {
            return "BEGINNER";
        } else if (levelString.contains("Lv2") || levelString.contains("실천가")) {
            return "INTERMEDIATE";
        } else if (levelString.contains("Lv3") || levelString.contains("전문가")) {
            return "EXPERT";
        }

        return "BEGINNER";
    }

    private FinancialData getUserFinancialData(Long memberId) {
        try {
            // 은행 계좌 정보 조회 (적금, 대출)
            IntegratedCustomerInfoRequest bankRequest = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(true)
                    .targetServices(new String[]{"BANK"})
                    .infoType("ALL")
                    .build();
            
            IntegratedCustomerInfoResponse bankResponse = groupIntegrationService.getIntegratedCustomerInfo(bankRequest);
            
            long savingsBalance = 0L;
            long loanBalance = 0L;
            
            if (bankResponse.getBankInfo() != null && bankResponse.getBankInfo().isAvailable()) {
                // 적금 잔액 합계
                if (bankResponse.getBankInfo().getProductDetails() != null) {
                    for (IntegratedCustomerInfoResponse.BankInfo.ProductDetail product : bankResponse.getBankInfo().getProductDetails()) {
                        if ("SAVINGS".equals(product.getProductType())) {
                            savingsBalance += product.getAmount() != null ? product.getAmount().longValue() : 0L;
                        } else if ("LOAN".equals(product.getProductType())) {
                            loanBalance += product.getAmount() != null ? product.getAmount().longValue() : 0L;
                        }
                    }
                }
            }
            
            // 카드 사용액 조회 (그린라이프 카드)
            long monthlyCardUsage = 0L;
            try {
                CardConsumptionSummaryResponse cardSummary = cardTransactionIntegrationService.getConsumptionSummary(memberId);
                if (cardSummary != null && cardSummary.getTotalAmount() != null) {
                    monthlyCardUsage = cardSummary.getTotalAmount();
                }
            } catch (Exception e) {
                log.warn("카드 사용액 조회 실패: {}", e.getMessage());
            }
            
            return new FinancialData(savingsBalance, loanBalance, monthlyCardUsage);
            
        } catch (Exception e) {
            log.error("금융 데이터 조회 실패: {}", e.getMessage(), e);
            return new FinancialData(0L, 0L, 0L);
        }
    }

    private static class FinancialData {
        final long savingsBalance;
        final long loanBalance;
        final long monthlyCardUsage;
        
        FinancialData(long savingsBalance, long loanBalance, long monthlyCardUsage) {
            this.savingsBalance = savingsBalance;
            this.loanBalance = loanBalance;
            this.monthlyCardUsage = monthlyCardUsage;
        }
    }
}
