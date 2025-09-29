package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.common.response.ApiResponse;
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

/**
 * 하나금융그룹 통합 정보 조회 컨트롤러
 * 하나그린월드에서 다른 관계사 정보를 통합 조회하는 API
 */
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Group Integration API", description = "하나금융그룹 통합 정보 조회 API")
public class GroupIntegrationController {

    private final GroupIntegrationService groupIntegrationService;
    private final HanamoneyIntegrationService hanamoneyIntegrationService;

    /**
     * 통합 고객 정보 조회 API
     * 하나은행, 하나카드 정보를 통합하여 조회
     * 
     * @param request 통합 고객 정보 요청
     * @return 통합 고객 정보 응답
     */
    @PostMapping("/customer-info")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "통합 고객 정보 조회",
        description = "현재 로그인한 고객의 하나금융그룹 전체 정보를 통합 조회합니다. " +
                     "하나은행 계좌/상품 정보, 하나카드/하나머니 정보를 안전하게 제공합니다. " +
                     "⚠️ 고객 동의가 필수입니다."
    )
    public ResponseEntity<ApiResponse<IntegratedCustomerInfoResponse>> getIntegratedCustomerInfo(
            @RequestBody IntegratedCustomerInfoRequest request) {
        
        try {
            log.info("통합 고객 정보 조회 요청 - 회원ID: {}, 대상서비스: {}", 
                    request.getMemberId(), String.join(",", request.getTargetServices()));

            IntegratedCustomerInfoResponse response = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            return ResponseEntity.ok(ApiResponse.success(response, "통합 고객 정보 조회가 완료되었습니다. 🏦💳"));

        } catch (SecurityException e) {
            log.error("보안 오류: {}", e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("접근이 거부되었습니다: " + e.getMessage()));

        } catch (RuntimeException e) {
            log.error("통합 고객 정보 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("통합 정보 조회에 실패했습니다: " + e.getMessage()));

        } catch (Exception e) {
            log.error("서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    /**
     * 간편 금융 현황 조회 API
     * 주요 정보만 간단히 조회
     * 
     * @param memberId 회원 ID
     * @return 간편 금융 현황
     */
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

            return ResponseEntity.ok(ApiResponse.success(summary, "금융 현황 조회가 완료되었습니다. 📊"));

        } catch (Exception e) {
            log.error("금융 현황 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("금융 현황 조회에 실패했습니다."));
        }
    }

    /**
     * 그룹 혜택 조회 API
     * 
     * @param memberId 회원 ID
     * @return 그룹 통합 혜택 정보
     */
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
                    response.getIntegratedBenefits(), 
                    "그룹 혜택 정보 조회가 완료되었습니다. 🎁"));

        } catch (Exception e) {
            log.error("그룹 혜택 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("그룹 혜택 조회에 실패했습니다."));
        }
    }

    /**
     * 상품 추천 API
     * 
     * @param memberId 회원 ID
     * @return 추천 상품 목록
     */
    @GetMapping("/recommendations/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "맞춤 상품 추천",
        description = "고객의 금융 이용 패턴을 분석하여 최적의 상품을 추천합니다."
    )
    public ResponseEntity<ApiResponse<java.util.List<String>>> getProductRecommendations(
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
            
            java.util.List<String> recommendations = response.getIntegratedBenefits() != null ?
                    response.getIntegratedBenefits().getRecommendedProducts() : 
                    java.util.Arrays.asList("하나 그린적금", "친환경 카드");

            return ResponseEntity.ok(ApiResponse.success(recommendations, "상품 추천이 완료되었습니다. 🌟"));

        } catch (Exception e) {
            log.error("상품 추천 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("상품 추천에 실패했습니다."));
        }
    }

    /**
     * 하나머니 정보 조회 API 🪙
     * 
     * @param memberId 회원 ID
     * @return 하나머니 잔액 및 거래 내역
     */
    @PostMapping("/hanamoney-info")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "하나머니 정보 조회",
        description = "하나카드에서 고객의 하나머니 잔액, 적립/사용 내역을 실시간으로 조회합니다. " +
                     "💰 현재 잔액, 누적 적립금, 최근 거래 내역을 제공합니다."
    )
    public ResponseEntity<ApiResponse<HanamoneyInfoResponse>> getHanamoneyInfo(
            @RequestBody HanamoneyInfoRequest request) {
        
        try {
            log.info("하나머니 정보 조회 요청 - 회원ID: {}", request.getMemberId());

            HanamoneyInfoResponse response = hanamoneyIntegrationService.getHanamoneyInfo(request);
            
            return ResponseEntity.ok(ApiResponse.success(
                    response, "하나머니 정보 조회가 완료되었습니다. 💰"));

        } catch (SecurityException e) {
            log.error("보안 오류: {}", e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("접근이 거부되었습니다: " + e.getMessage()));

        } catch (Exception e) {
            log.error("하나머니 정보 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("하나머니 정보 조회에 실패했습니다."));
        }
    }

    /**
     * 카드 목록 및 사용 내역 조회 API 💳
     * 
     * @param memberId 회원 ID
     * @return 보유 카드 목록 및 사용 현황
     */
    @GetMapping("/cards/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "카드 목록 및 사용 내역 조회",
        description = "고객이 보유한 하나카드 목록, 신용한도, 월 사용금액 등을 조회합니다. " +
                     "💳 카드별 혜택 정보도 함께 제공됩니다."
    )
    public ResponseEntity<ApiResponse<CardListResponse>> getCardList(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }

            log.info("카드 목록 조회 요청 - 회원ID: {}", memberId);

            // 실제로는 카드 정보 조회 서비스 구현 필요
            // 임시로 통합 정보에서 카드 정보 추출
            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"CARD"})
                    .infoType("CARD")
                    .build();

            IntegratedCustomerInfoResponse integrated = groupIntegrationService.getIntegratedCustomerInfo(request);
            
            // 카드 응답 생성 (실제로는 별도 서비스에서 처리)
            CardListResponse response = buildCardListResponse(integrated);
            
            return ResponseEntity.ok(ApiResponse.success(
                    response, "카드 목록 조회가 완료되었습니다. 💳"));

        } catch (Exception e) {
            log.error("카드 목록 조회 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("카드 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 은행 계좌/상품 목록 조회 API 🏦
     * 
     * @param memberId 회원 ID
     * @return 적금, 대출, 투자 계좌 목록
     */
    @GetMapping("/bank-accounts/{memberId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "은행 계좌/상품 목록 조회",
        description = "고객의 하나은행 적금, 대출, 투자 계좌 정보를 조회합니다. " +
                     "🏦 계좌 잔고, 대출 잔액, 투자 수익률 등을 제공합니다."
    )
    public ResponseEntity<ApiResponse<BankAccountsResponse>> getBankAccounts(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("고객 동의가 필요합니다."));
            }

            log.info("🏦 은행 계좌 목록 조회 요청 - 회원ID: {}", memberId);

            IntegratedCustomerInfoRequest request = IntegratedCustomerInfoRequest.builder()
                    .memberId(memberId)
                    .customerConsent(consent)
                    .targetServices(new String[]{"BANK"})
                    .infoType("ALL")
                    .build();

            log.info("🏦 하나은행 통합 API 호출 시작 - 회원ID: {}", memberId);
            IntegratedCustomerInfoResponse integrated = groupIntegrationService.getIntegratedCustomerInfo(request);
            log.info("🏦 하나은행 통합 API 응답 수신 완료 - 회원ID: {}", memberId);
            
            // 은행 계좌 응답 생성
            BankAccountsResponse response = buildBankAccountsResponse(integrated);
            log.info("🏦 은행 계좌 응답 생성 완료 - 적금 계좌 수: {}", 
                    response.getSavingsAccounts() != null ? response.getSavingsAccounts().size() : 0);
            
            return ResponseEntity.ok(ApiResponse.success(
                    response, "은행 계좌 목록 조회가 완료되었습니다. 🏦"));

        } catch (Exception e) {
            log.error("🏦 은행 계좌 조회 실패 - 회원ID: {}", memberId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("은행 계좌 조회에 실패했습니다."));
        }
    }

    /**
     * 카드 목록 응답 생성 헬퍼 메서드 - 실제 hanacard API 호출
     */
    private CardListResponse buildCardListResponse(IntegratedCustomerInfoResponse integrated) {
        log.info("💳 카드 목록 응답 생성 시작");
        
        java.util.List<CardListResponse.CardInfo> cards = new java.util.ArrayList<>();
        
        try {
            // 하나카드 서버에서 실제 카드 정보 조회
            if (integrated.getCardInfo() != null && integrated.getCardInfo().isAvailable()) {
                log.info("💳 하나카드 서버에서 카드 정보 조회 시작");
                
                // 통합 정보에서 카드 데이터 추출
                java.util.Map<String, Object> cardData = integrated.getCardInfo().getCardData();
                if (cardData != null && cardData.containsKey("cards")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> cardList = 
                        (java.util.List<java.util.Map<String, Object>>) cardData.get("cards");
                    
                    log.info("💳 조회된 카드 수: {}", cardList.size());
                    
                    for (java.util.Map<String, Object> cardInfo : cardList) {
                        log.info("💳 카드 상세 정보:");
                        log.info("  - 카드번호: {}", cardInfo.get("cardNumber"));
                        log.info("  - 카드명: {}", cardInfo.get("cardName"));
                        log.info("  - 카드타입: {}", cardInfo.get("cardType"));
                        log.info("  - 신용한도: {}", cardInfo.get("creditLimit"));
                        log.info("  - 사용가능한도: {}", cardInfo.get("availableLimit"));
                        log.info("  - 월사용금액: {}", cardInfo.get("monthlyUsage"));
                        
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
            
            log.info("💳 카드 목록 변환 완료 - 총 {}개", cards.size());
            
        } catch (Exception e) {
            log.error("💳 카드 정보 변환 실패", e);
            // 에러 시 빈 리스트 반환
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

    /**
     * 은행 계좌 응답 생성 헬퍼 메서드
     */
    private BankAccountsResponse buildBankAccountsResponse(IntegratedCustomerInfoResponse integrated) {
        java.util.List<BankAccountsResponse.SavingsAccountInfo> savingsAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.LoanAccountInfo> loanAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.InvestmentAccountInfo> investmentAccounts = new java.util.ArrayList<>();
        java.util.List<BankAccountsResponse.DemandDepositAccountInfo> demandDepositAccounts = new java.util.ArrayList<>();

        if (integrated.getBankInfo() != null && integrated.getBankInfo().isAvailable()) {
            log.info("🏦 하나은행 정보 사용 가능 - 상품 수: {}",
                    integrated.getBankInfo().getProductDetails() != null ? integrated.getBankInfo().getProductDetails().size() : 0);

            // 실제 하나은행 API에서 받은 적금 상품 데이터 사용
            if (integrated.getBankInfo().getProductDetails() != null && !integrated.getBankInfo().getProductDetails().isEmpty()) {
                log.info("🏦 하나은행 상품 상세 정보 처리 시작");

                // 하나은행 API 응답에서 적금 상품 정보를 적금 계좌로 변환
                for (IntegratedCustomerInfoResponse.BankInfo.ProductDetail product : integrated.getBankInfo().getProductDetails()) {
                    log.info("🏦 상품 처리 중 - 타입: {}, 이름: {}, 코드: {}", 
                            product.getProductType(), product.getProductName(), product.getProductCode());
                    
                    if ("DEMAND_DEPOSIT".equals(product.getProductType())) {
                        log.info("🏦 입출금 계좌 발견 - 상세 정보:");
                        log.info("  - 계좌번호: {}", product.getProductCode());
                        log.info("  - 상품명: {}", product.getProductName());
                        log.info("  - 잔액: {}", product.getAmount());

                        demandDepositAccounts.add(BankAccountsResponse.DemandDepositAccountInfo.builder()
                                .accountNumber(product.getProductCode())
                                .accountName(product.getProductName())
                                .accountType("CHECKING")
                                .balance(product.getAmount())
                                .accountHolderName("사용자") // 실제로는 사용자 이름 사용
                                .bankName("하나은행")
                                .accountTypeDescription("입출금예금")
                                .openDate(product.getSubscriptionDate() != null ? product.getSubscriptionDate() : java.time.LocalDateTime.now().minusMonths(6))
                                .isActive("ACTIVE".equals(product.getStatus()))
                                .status(product.getStatus())
                                .build());
                    } else if ("SAVINGS".equals(product.getProductType())) {
                        log.info("🏦 적금 상품 발견 - 상세 정보:");
                        log.info("  - 계좌번호: {}", product.getProductCode());
                        log.info("  - 상품명: {}", product.getProductName());
                        log.info("  - 잔액: {}", product.getAmount());
                        log.info("  - 기본금리: {}", product.getBaseRate());
                        log.info("  - 우대금리: {}", product.getPreferentialRate());
                        log.info("  - 적용금리: {}", product.getInterestRate());
                        log.info("  - 만기일: {}", product.getMaturityDate());
                        log.info("  - 가입일: {}", product.getSubscriptionDate());

                        savingsAccounts.add(BankAccountsResponse.SavingsAccountInfo.builder()
                                .accountNumber(product.getProductCode()) // 실제 계좌번호 사용
                                .productName(product.getProductName())
                                .accountType("REGULAR_SAVINGS")
                                .balance(product.getAmount())
                                .interestRate(product.getInterestRate()) // 실제 적용금리 사용 (null 가능)
                                .baseRate(product.getBaseRate()) // 실제 기본금리 사용 (null 가능)
                                .preferentialRate(product.getPreferentialRate()) // 실제 우대금리 사용 (null 가능)
                                .openDate(product.getSubscriptionDate() != null ? product.getSubscriptionDate() : java.time.LocalDateTime.now().minusMonths(10)) // 실제 가입일 사용
                                .maturityDate(product.getMaturityDate() != null ? product.getMaturityDate() : java.time.LocalDateTime.now().plusMonths(2)) // 실제 만기일 사용
                                .status(product.getStatus())
                                .build());
                    }
                }
                log.info("🏦 적금 계좌 변환 완료 - 총 {}개", savingsAccounts.size());
            } else {
                log.warn("🏦 하나은행 상품 상세 정보가 없습니다.");
            }
            
            // 적금 상품이 없으면 빈 목록 유지 (기본 데이터 제거)
            if (savingsAccounts.isEmpty()) {
                log.info("🏦 적금 상품이 없습니다 - 빈 목록 반환");
            }

            // 대출 계좌 - 하나은행 API에서 실제 데이터 조회
            if (integrated.getBankInfo() != null && integrated.getBankInfo().getProductDetails() != null) {
                log.info("🏦 대출 상품 조회 시작");
                for (IntegratedCustomerInfoResponse.BankInfo.ProductDetail product : integrated.getBankInfo().getProductDetails()) {
                    if ("LOAN".equals(product.getProductType()) || "MORTGAGE".equals(product.getProductType()) || 
                        "AUTO_LOAN".equals(product.getProductType()) || "GREEN_LOAN".equals(product.getProductType())) {
                        
                        log.info("🏦 대출 상품 발견 - 상세 정보:");
                        log.info("  - 상품코드: {}", product.getProductCode());
                        log.info("  - 상품명: {}", product.getProductName());
                        log.info("  - 상품타입: {}", product.getProductType());
                        log.info("  - 대출금액: {}", product.getAmount());
                        log.info("  - 잔여금액: {}", product.getRemainingAmount());
                        log.info("  - 금리: {}", product.getInterestRate());
                        log.info("  - 월상환금: {}", product.getMonthlyPayment());
                        log.info("  - 시작일: {}", product.getStartDate());
                        log.info("  - 만기일: {}", product.getMaturityDate());
                        log.info("  - 상태: {}", product.getStatus());
                        
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
                log.info("🏦 대출 계좌 변환 완료 - 총 {}개", loanAccounts.size());
            }
            
            // 대출 상품이 없으면 빈 목록 유지 (기본 데이터 제거)
            if (loanAccounts.isEmpty()) {
                log.info("🏦 대출 상품이 없습니다 - 빈 목록 반환");
            }

            // 투자 계좌는 실제 데이터만 사용 (기본 데이터 제거)
            log.info("🏦 투자 계좌는 실제 데이터만 사용");
        }

        // 실제 데이터만 사용하여 요약 정보 생성
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

    /**
     * 특정 상품 보유 여부 확인 API 🔍
     * 
     * @param request 상품 보유 여부 확인 요청
     * @return 상품 보유 여부
     */
    @PostMapping("/check-product-ownership")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "특정 상품 보유 여부 확인",
        description = "고객이 특정 상품을 보유하고 있는지 확인합니다. " +
                     "🔍 적금, 대출, 투자 상품 등의 보유 여부를 실시간으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> checkProductOwnership(
            @RequestBody java.util.Map<String, Object> request) {
        
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            log.info("🔍 상품 보유 여부 확인 요청 - 상품ID: {}", productId);

            // 현재 로그인한 사용자 정보 가져오기
            Long memberId = SecurityUtil.getCurrentMemberId();
            if (memberId == null) {
                log.error("❌ 현재 로그인한 사용자 정보를 찾을 수 없습니다.");
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("인증된 사용자 정보를 찾을 수 없습니다."));
            }
            
            log.info("🔍 현재 로그인한 사용자 ID: {}", memberId);
            
            // 하나은행 서비스에 상품 보유 여부 확인 요청
            boolean hasProduct = groupIntegrationService.checkProductOwnership(memberId, productId);
            
            java.util.Map<String, Boolean> response = java.util.Map.of("hasProduct", hasProduct);
            
            return ResponseEntity.ok(ApiResponse.success(
                    response, "상품 보유 여부 확인이 완료되었습니다. 🔍"));

        } catch (Exception e) {
            log.error("🔍 상품 보유 여부 확인 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("상품 보유 여부 확인에 실패했습니다."));
        }
    }

    /**
     * 간편 금융 현황 DTO
     */
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
