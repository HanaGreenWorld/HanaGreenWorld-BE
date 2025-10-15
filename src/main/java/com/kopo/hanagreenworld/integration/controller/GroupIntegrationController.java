package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.integration.dto.*;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import com.kopo.hanagreenworld.integration.service.HanamoneyIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Group Integration API", description = "하나금융그룹 통합 정보 조회 API")
public class GroupIntegrationController {

    private final GroupIntegrationService groupIntegrationService;
    private final HanamoneyIntegrationService hanamoneyIntegrationService;

    @PostMapping("/customer-info")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "통합 고객 정보 조회",
        description = "현재 로그인한 고객의 하나금융그룹 전체 정보를 통합 조회합니다. " +
                     "하나은행 계좌/상품 정보, 하나카드/하나머니 정보를 안전하게 제공합니다. " +
                     "⚠고객 동의가 필수입니다."
    )
    public ResponseEntity<ApiResponse<IntegratedCustomerInfoResponse>> getIntegratedCustomerInfo(
            @RequestBody IntegratedCustomerInfoRequest request) {
        
        try {
            IntegratedCustomerInfoResponse response = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            return ResponseEntity.ok(ApiResponse.success("통합 고객 정보 조회가 완료되었습니다.", response));

        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("접근이 거부되었습니다: " + e.getMessage()));

        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("통합 정보 조회에 실패했습니다: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @GetMapping("/financial-summary/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "간편 금융 현황 조회",
        description = "고객의 하나금융그룹 주요 정보를 간단히 조회합니다. " +
                     "계좌 수, 카드 수, 포인트 등 핵심 정보만 제공합니다."
    )
    public ResponseEntity<ApiResponse<FinancialSummary>> getFinancialSummary(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }

            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"ALL"})
                    .infoType("BASIC")
                    .build();

            IntegratedCustomerInfoResponse response = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            FinancialSummary summary = FinancialSummary.builder()
                    .customerName(response.getCustomerSummary().getName())
                    .overallGrade(response.getCustomerSummary().getOverallGrade())
                    .bankAccountCount(response.getBankInfo() != null ? response.getBankInfo().getAccountCount() : 0)
                    .cardCount(response.getCardInfo() != null ? response.getCardInfo().getCardCount() : 0)
                    .hanamoneyPoints(response.getCardInfo() != null ? response.getCardInfo().getHanamoneyPoints() : null)
                    .totalBenefits(response.getIntegratedBenefits() != null ? 
                            response.getIntegratedBenefits().getAvailableBenefits().size() : 0)
                    .isPremiumEligible(response.getIntegratedBenefits() != null && 
                            response.getIntegratedBenefits().isEligibleForPremiumService())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("금융 현황 조회가 완료되었습니다.", summary));

        } catch (Exception e) {
            log.error("금융 현황 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("금융 현황 조회에 실패했습니다."));
        }
    }

    @GetMapping("/benefits/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "그룹 통합 혜택 조회",
        description = "하나금융그룹 통합 혜택 및 추천 상품을 조회합니다. " +
                     "고객 등급에 따른 맞춤 혜택을 제공합니다."
    )
    public ResponseEntity<ApiResponse<IntegratedCustomerInfoResponse.IntegratedBenefits>> getGroupBenefits(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }

            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"ALL"})
                    .infoType("ALL")
                    .build();

            IntegratedCustomerInfoResponse response = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "그룹 혜택 정보 조회가 완료되었습니다.", response.getIntegratedBenefits()));

        } catch (Exception e) {
            log.error("그룹 혜택 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("그룹 혜택 조회에 실패했습니다."));
        }
    }

    @PostMapping("/hanamoney-info")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "하나머니 정보 조회",
        description = "하나카드에서 고객의 하나머니 잔액, 적립/사용 내역을 실시간으로 조회합니다. " +
                     "현재 잔액, 누적 적립금, 최근 거래 내역을 제공합니다."
    )
    public ResponseEntity<ApiResponse<HanamoneyInfoResponse>> getHanamoneyInfo(
            @RequestBody HanamoneyInfoRequest request) {
        
        try {
            HanamoneyInfoResponse response = hanamoneyIntegrationService.getHanamoneyInfo(request);
            
            return ResponseEntity.ok(ApiResponse.success("하나머니 정보 조회가 완료되었습니다.", response));

        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("접근이 거부되었습니다: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("하나머니 정보 조회에 실패했습니다."));
        }
    }

    @GetMapping("/cards/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "카드 목록 및 사용 내역 조회",
        description = "고객이 보유한 하나카드 목록, 신용한도, 월 사용금액 등을 조회합니다. " +
                     "카드별 혜택 정보도 함께 제공됩니다."
    )
    public ResponseEntity<ApiResponse<CardListResponse>> getCardList(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }


            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"CARD"})
                    .infoType("CARD")
                    .build();

            IntegratedCustomerInfoResponse integrated = groupIntegrationService.getIntegratedCustomerInfo(request);

            CardListResponse response = buildCardListResponse(integrated);
            
            return ResponseEntity.ok(ApiResponse.success("카드 목록 조회가 완료되었습니다.", response));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카드 목록 조회에 실패했습니다."));
        }
    }

    @GetMapping("/bank-accounts/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "은행 계좌/상품 목록 조회",
        description = "고객의 하나은행 적금, 대출, 투자 계좌 정보를 조회합니다. " +
                     "계좌 잔고, 대출 잔액, 투자 수익률 등을 제공합니다."
    )
    public ResponseEntity<ApiResponse<BankAccountsResponse>> getBankAccounts(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }

            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"BANK"})
                    .infoType("ALL")
                    .build();

            IntegratedCustomerInfoResponse integrated = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            // 은행 계좌 응답 생성
            BankAccountsResponse response = buildBankAccountsResponse(integrated);
            return ResponseEntity.ok(ApiResponse.success("은행 계좌 목록 조회가 완료되었습니다.", response));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("은행 계좌 조회에 실패했습니다."));
        }
    }

    private CardListResponse buildCardListResponse(IntegratedCustomerInfoResponse integrated) {
        java.util.List<CardListResponse.CardInfo> cards = new java.util.ArrayList<>();
        
        try {
            // 하나카드 서버에서 실제 카드 정보 조회
            if (integrated.getCardInfo() != null && integrated.getCardInfo().isAvailable()) {
                
                // 통합 정보에서 카드 데이터 추출
                java.util.Map<String, Object> cardData = integrated.getCardInfo().getCardData();
                if (cardData != null && cardData.containsKey("cards")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> cardList = 
                        (java.util.List<java.util.Map<String, Object>>) cardData.get("cards");
                    
                    for (java.util.Map<String, Object> cardInfo : cardList) {
                        cards.add(CardListResponse.CardInfo.builder()
                                .cardNumber(cardInfo.get("cardNumber").toString())
                                .cardName(cardInfo.get("cardName").toString())
                                .cardType(cardInfo.get("cardType").toString())
                                .cardStatus(cardInfo.get("cardStatus").toString())
                                .creditLimit((java.math.BigDecimal) cardInfo.get("creditLimit"))
                                .availableLimit((java.math.BigDecimal) cardInfo.get("availableLimit"))
                                .monthlyUsage((java.math.BigDecimal) cardInfo.get("monthlyUsage"))
                                .issueDate((java.time.LocalDateTime) cardInfo.get("issueDate"))
                                .expiryDate((java.time.LocalDateTime) cardInfo.get("expiryDate"))
                                .benefits((java.util.List<String>) cardInfo.get("benefits"))
                                .build());
                    }
                }
            }

            
        } catch (Exception e) {
            log.error("카드 정보 변환 실패", e);
        }

        // 요약 정보 생성
        java.math.BigDecimal totalCreditLimit = cards.stream()
                .map(CardListResponse.CardInfo::getCreditLimit)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal totalAvailableLimit = cards.stream()
                .map(CardListResponse.CardInfo::getAvailableLimit)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal monthlyTotalUsage = cards.stream()
                .map(CardListResponse.CardInfo::getMonthlyUsage)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        CardListResponse.CardSummary summary = CardListResponse.CardSummary.builder()
                .totalCardCount(cards.size())
                .activeCardCount(cards.size())
                .totalCreditLimit(totalCreditLimit)
                .totalAvailableLimit(totalAvailableLimit)
                .monthlyTotalUsage(monthlyTotalUsage)
                .primaryCardType(cards.isEmpty() ? "NONE" : cards.get(0).getCardType())
                .build();

        return CardListResponse.builder()
                .cards(cards)
                .summary(summary)
                .responseTime(java.time.LocalDateTime.now())
                .build();
    }

    private BankAccountsResponse buildBankAccountsResponse(IntegratedCustomerInfoResponse integrated) {
        java.util.List<BankAccountsResponse.SavingsAccountInfo> savingsAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.LoanAccountInfo> loanAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.InvestmentAccountInfo> investmentAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.DemandDepositAccountInfo> demandDepositAccounts = new java.util.ArrayList<>();

        if (integrated.getBankInfo() != null && integrated.getBankInfo().isAvailable()) {
            if (integrated.getBankInfo().getProductDetails() != null && !integrated.getBankInfo().getProductDetails().isEmpty()) {
                for (IntegratedCustomerInfoResponse.BankInfo.ProductDetail product : integrated.getBankInfo().getProductDetails()) {
                    if ("DEMAND_DEPOSIT".equals(product.getProductType())) {
                        demandDepositAccounts.add(BankAccountsResponse.DemandDepositAccountInfo.builder()
                                .accountNumber(product.getProductCode())
                                .accountName(product.getProductName())
                                .accountType("CHECKING")
                                .balance(product.getAmount())
                                .bankName("하나은행")
                                .accountTypeDescription("입출금예금")
                                .openDate(product.getSubscriptionDate() != null ? product.getSubscriptionDate() : java.time.LocalDateTime.now().minusMonths(6))
                                .isActive("ACTIVE".equals(product.getStatus()))
                                .status(product.getStatus())
                                .build());
                    } else if ("SAVINGS".equals(product.getProductType())) {

                        savingsAccounts.add(BankAccountsResponse.SavingsAccountInfo.builder()
                                .accountNumber(product.getProductCode())
                                .productName(product.getProductName())
                                .accountType("REGULAR_SAVINGS")
                                .balance(product.getAmount())
                                .interestRate(product.getInterestRate())
                                .baseRate(product.getBaseRate())
                                .preferentialRate(product.getPreferentialRate())
                                .openDate(product.getSubscriptionDate() != null ? product.getSubscriptionDate() : java.time.LocalDateTime.now().minusMonths(10))
                                .maturityDate(product.getMaturityDate() != null ? product.getMaturityDate() : java.time.LocalDateTime.now().plusMonths(2))
                                .status(product.getStatus())
                                .build());
                    }
                }
            } else {
                log.warn("하나은행 상품 상세 정보가 없습니다.");
            }

            if (savingsAccounts.isEmpty()) {
                log.info("적금 상품이 없습니다");
            }

            if (integrated.getBankInfo() != null && integrated.getBankInfo().getProductDetails() != null) {
                for (IntegratedCustomerInfoResponse.BankInfo.ProductDetail product : integrated.getBankInfo().getProductDetails()) {
                    if ("LOAN".equals(product.getProductType()) || "MORTGAGE".equals(product.getProductType()) || 
                        "AUTO_LOAN".equals(product.getProductType()) || "GREEN_LOAN".equals(product.getProductType())) {
                        loanAccounts.add(BankAccountsResponse.LoanAccountInfo.builder()
                                .accountNumber(product.getProductCode())
                                .productName(product.getProductName())
                                .accountType(product.getProductType())
                                .loanAmount(product.getAmount())
                                .remainingAmount(product.getRemainingAmount() != null ? product.getRemainingAmount() : product.getAmount())
                                .interestRate(product.getInterestRate() != null ? product.getInterestRate() : new java.math.BigDecimal("4.5"))
                                .openDate(product.getStartDate() != null ? product.getStartDate() : java.time.LocalDateTime.now().minusYears(2))
                                .maturityDate(product.getMaturityDate() != null ? product.getMaturityDate() : java.time.LocalDateTime.now().plusYears(3))
                                .status(product.getStatus())
                                .build());
                    }
                }
            }

            if (loanAccounts.isEmpty()) {
                log.info("대출 상품이 없습니다");
            }
        }

        java.math.BigDecimal totalSavingsBalance = savingsAccounts.stream()
                .map(BankAccountsResponse.SavingsAccountInfo::getBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalDemandDepositBalance = demandDepositAccounts.stream()
                .map(BankAccountsResponse.DemandDepositAccountInfo::getBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalLoanBalance = loanAccounts.stream()
                .map(BankAccountsResponse.LoanAccountInfo::getRemainingAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalInvestmentValue = investmentAccounts.stream()
                .map(BankAccountsResponse.InvestmentAccountInfo::getCurrentValue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        BankAccountsResponse.AccountSummary summary = BankAccountsResponse.AccountSummary.builder()
                .totalAccountCount(savingsAccounts.size() + demandDepositAccounts.size() + loanAccounts.size() + investmentAccounts.size())
                .totalSavingsBalance(totalSavingsBalance)
                .totalLoanBalance(totalLoanBalance)
                .totalInvestmentValue(totalInvestmentValue)
                .totalDepositBalance(totalDemandDepositBalance)
                .customerGrade("STANDARD") // 기본 등급
                .build();

        return BankAccountsResponse.builder()
                .savingsAccounts(savingsAccounts)
                .demandDepositAccounts(demandDepositAccounts)
                .loanAccounts(loanAccounts)
                .investmentAccounts(investmentAccounts)
                .summary(summary)
                .responseTime(java.time.LocalDateTime.now())
                .build();
    }

    @PostMapping("/check-product-ownership")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "특정 상품 보유 여부 확인",
        description = "고객이 특정 상품을 보유하고 있는지 확인합니다. " +
                     "적금, 대출, 투자 상품 등의 보유 여부를 실시간으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> checkProductOwnership(
            @RequestBody java.util.Map<String, Object> request) {
        
        try {
            Long productId = Long.valueOf(request.get("productId").toString());

            // 현재 로그인한 사용자 정보 가져오기
            Long memberId = SecurityUtil.getCurrentMemberId();
            if (memberId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("인증된 사용자 정보를 찾을 수 없습니다."));
            }

            // 하나은행 서비스에 상품 보유 여부 확인 요청
            boolean hasProduct = groupIntegrationService.checkProductOwnership(memberId, productId);
            
            java.util.Map<String, Boolean> response = java.util.Map.of("hasProduct", hasProduct);
            
            return ResponseEntity.ok(ApiResponse.success("상품 보유 여부 확인이 완료되었습니다.", response));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("상품 보유 여부 확인에 실패했습니다."));
        }
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FinancialSummary {
        private String customerName;
        private String overallGrade;
        private int bankAccountCount;
        private int cardCount;
        private java.math.BigDecimal hanamoneyPoints;
        private int totalBenefits;
        private boolean isPremiumEligible;
    }
}
