package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.domain.GroupCustomerMapping;
import com.kopo.hanagreenworld.integration.dto.IntegratedCustomerInfoRequest;
import com.kopo.hanagreenworld.integration.dto.IntegratedCustomerInfoResponse;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 하나금융그룹 통합 서비스
 * 관계사 정보를 통합하여 조회하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroupIntegrationService {

    private final MemberRepository memberRepository;
    private final GroupIntegrationTokenService tokenService;
    private final RestTemplate restTemplate;

    @Value("${integration.bank.url}")
    private String bankServiceUrl;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    /**
     * 통합 고객 정보 조회
     */
    public IntegratedCustomerInfoResponse getIntegratedCustomerInfo(IntegratedCustomerInfoRequest request) {
        try {
            log.info("=== 통합 고객 정보 조회 시작 ===");
            log.info("회원ID: {}", request.getMemberId());
            log.info("요청서비스: {}", Arrays.toString(request.getTargetServices()));
            log.info("정보타입: {}", request.getInfoType());
            log.info("고객동의: {}", request.getCustomerConsent());

            // 1. 고객 동의 확인
            if (!Boolean.TRUE.equals(request.getCustomerConsent())) {
                log.error("고객 동의가 필요합니다.");
                throw new SecurityException("고객 동의가 필요합니다.");
            }
            log.info("1단계: 고객 동의 확인 완료");

            // 2. 회원 정보 조회
            log.info("2단계: 회원 정보 조회 시작");
            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));
            log.info("회원 정보 조회 완료 - 이름: {}, 이메일: {}", member.getName(), member.getEmail());

            // 3. 그룹 고객 토큰 조회 또는 생성
            log.info("3단계: 그룹 고객 토큰 조회/생성 시작");
            String groupCustomerToken = getOrCreateGroupCustomerToken(member);
            log.info("🔑 생성된 Group Customer Token: {}", groupCustomerToken);
            log.info("🔑 Token 형식 확인 - CI_전화번호_해시코드: {}", groupCustomerToken.startsWith("CI_"));

            // 4. 내부 서비스 토큰 생성
            log.info("4단계: 내부 서비스 토큰 생성 시작");
            String internalServiceToken = tokenService.generateInternalServiceToken();
            
            // groupCustomerToken null 체크
            if (groupCustomerToken == null || groupCustomerToken.trim().isEmpty()) {
                log.error("groupCustomerToken이 null이거나 비어있음: {}", groupCustomerToken);
                throw new RuntimeException("그룹 고객 토큰을 생성할 수 없습니다.");
            }
            
            String customerInfoToken = tokenService.generateCustomerInfoToken(groupCustomerToken);
            String consentToken = generateConsentToken(member.getMemberId());
            log.info("🔑 내부 서비스 토큰: {}", internalServiceToken);
            log.info("🔑 고객 정보 토큰: {}", customerInfoToken);
            log.info("🔑 동의 토큰: {}", consentToken);
            
            // customerInfoToken null 체크
            if (customerInfoToken == null || customerInfoToken.trim().isEmpty()) {
                log.error("customerInfoToken이 null이거나 비어있음: {}", customerInfoToken);
                throw new RuntimeException("고객 정보 토큰을 생성할 수 없습니다.");
            }

            // 5. 관계사별 정보 조회
            IntegratedCustomerInfoResponse.BankInfo bankInfo = null;
            IntegratedCustomerInfoResponse.CardInfo cardInfo = null;

            List<String> targetServices = Arrays.asList(request.getTargetServices());

            if (targetServices.contains("BANK") || targetServices.contains("ALL")) {
                log.info("5단계: 하나은행 정보 조회 시작");
                bankInfo = getBankInfo(internalServiceToken, customerInfoToken, consentToken, request.getInfoType());
                log.info("하나은행 정보 조회 완료 - 상품수: {}", 
                        bankInfo != null && bankInfo.getMainProducts() != null ? bankInfo.getMainProducts().size() : 0);
            }

            if (targetServices.contains("CARD") || targetServices.contains("ALL")) {
                log.info("6단계: 하나카드 정보 조회 시작");
                cardInfo = getCardInfo(internalServiceToken, customerInfoToken, consentToken, request.getInfoType());
                log.info("하나카드 정보 조회 완료 - 카드수: {}", cardInfo != null ? cardInfo.getCardCount() : 0);
            }

            // 6. 통합 응답 생성
            return buildIntegratedResponse(member, bankInfo, cardInfo);

        } catch (Exception e) {
            log.error("통합 고객 정보 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("통합 고객 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 그룹 고객 토큰 조회 또는 생성
     */
    private String getOrCreateGroupCustomerToken(Member member) {
        Optional<String> existingToken = tokenService.getGroupTokenByPhone(member.getPhoneNumber());
        
        if (existingToken.isPresent()) {
            return existingToken.get();
        }

        // 새 토큰 생성 (실제로는 본인인증을 통해 CI 획득)
        String mockCi = generateMockCI(member);
        return tokenService.createGroupCustomerToken(
                mockCi,
                member.getName(),
                member.getPhoneNumber(),
                member.getEmail(),
                "19900101" // 실제로는 본인인증에서 생년월일 획득
        );
    }

    /**
     * 하나은행 정보 조회
     */
    private IntegratedCustomerInfoResponse.BankInfo getBankInfo(String internalServiceToken, String customerInfoToken, String consentToken, String infoType) {
        try {
            String url = bankServiceUrl + "/api/integration/customer-info";
            log.info("하나은행 API 호출 시작");
            log.info("URL: {}", url);
            log.info("내부 서비스 토큰: {}", internalServiceToken);
            log.info("고객 정보 토큰: {}", customerInfoToken);
            log.info("동의 토큰: {}", consentToken);
            log.info("정보 타입: {}", infoType);
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);
            log.info("요청 헤더 설정 완료");

            // 요청 바디 생성
            Map<String, String> requestBody = Map.of(
                    "groupCustomerToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "consentToken", consentToken,
                    "infoType", infoType != null ? infoType : "ALL"
            );
            log.info("요청 바디 생성 완료: {}", requestBody);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            log.info("HTTP 요청 엔티티 생성 완료");
            
            log.info("하나은행 API 호출 실행 중...");
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("하나은행 API 응답 수신 완료");
            log.info("응답 상태코드: {}", response.getStatusCode());
            log.info("응답 헤더: {}", response.getHeaders());
            log.info("응답 바디: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("하나은행 응답 파싱 시작");
                IntegratedCustomerInfoResponse.BankInfo bankInfo = parseBankResponse(response.getBody());
                log.info("하나은행 응답 파싱 완료 - 상품수: {}", 
                        bankInfo != null && bankInfo.getMainProducts() != null ? bankInfo.getMainProducts().size() : 0);
                return bankInfo;
            } else {
                log.error("하나은행 API 호출 실패 - 상태코드: {}", response.getStatusCode());
                return IntegratedCustomerInfoResponse.BankInfo.builder()
                        .isAvailable(false)
                        .errorMessage("하나은행 정보 조회 실패")
                        .build();
            }

        } catch (Exception e) {
            log.error("하나은행 정보 조회 실패", e);
            return IntegratedCustomerInfoResponse.BankInfo.builder()
                    .isAvailable(false)
                    .errorMessage("하나은행 서비스 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 하나카드 정보 조회
     */
    private IntegratedCustomerInfoResponse.CardInfo getCardInfo(String internalServiceToken, String customerInfoToken, String consentToken, String infoType) {
        try {
            String url = cardServiceUrl + "/api/integration/customer-info";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            Map<String, String> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "consentToken", consentToken,
                    "infoType", infoType != null ? infoType : "ALL"
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseCardResponse(response.getBody());
            } else {
                return IntegratedCustomerInfoResponse.CardInfo.builder()
                        .isAvailable(false)
                        .errorMessage("하나카드 정보 조회 실패")
                        .build();
            }

        } catch (Exception e) {
            log.error("하나카드 정보 조회 실패", e);
            return IntegratedCustomerInfoResponse.CardInfo.builder()
                    .isAvailable(false)
                    .errorMessage("하나카드 서비스 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 통합 응답 생성
     */
    private IntegratedCustomerInfoResponse buildIntegratedResponse(
            Member member,
            IntegratedCustomerInfoResponse.BankInfo bankInfo,
            IntegratedCustomerInfoResponse.CardInfo cardInfo) {

        // 고객 요약 정보
        IntegratedCustomerInfoResponse.CustomerSummary customerSummary = 
                IntegratedCustomerInfoResponse.CustomerSummary.builder()
                        .name(member.getName())
                        .email(member.getEmail())
                        .phoneNumber(member.getPhoneNumber())
                        .overallGrade(determineOverallGrade(bankInfo, cardInfo))
                        .hasBankAccount(bankInfo != null && bankInfo.isAvailable())
                        .hasCardAccount(cardInfo != null && cardInfo.isAvailable())
                        .hasGreenWorldAccount(true)
                        .firstJoinDate(member.getCreatedAt())
                        .build();

        // 통합 혜택 정보
        IntegratedCustomerInfoResponse.IntegratedBenefits integratedBenefits = 
                buildIntegratedBenefits(bankInfo, cardInfo);

        return IntegratedCustomerInfoResponse.builder()
                .customerSummary(customerSummary)
                .bankInfo(bankInfo)
                .cardInfo(cardInfo)
                .integratedBenefits(integratedBenefits)
                .responseTime(LocalDateTime.now())
                .build();
    }

    /**
     * 하나은행 응답 파싱
     */
    private IntegratedCustomerInfoResponse.BankInfo parseBankResponse(Map<String, Object> response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                log.warn("응답 데이터가 null입니다.");
                return createErrorBankInfo("응답 데이터가 없습니다.");
            }
            
            Map<String, Object> customerInfo = (Map<String, Object>) data.get("customerInfo");
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) data.get("accounts");
            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("products");

            BigDecimal totalBalance = BigDecimal.ZERO;
            List<IntegratedCustomerInfoResponse.BankInfo.AccountInfo> accountDetails = new ArrayList<>();
            List<IntegratedCustomerInfoResponse.BankInfo.ProductDetail> productDetails = new ArrayList<>();

            if (accounts != null) {
                for (Map<String, Object> account : accounts) {
                    log.info("🏦 계좌 정보 처리 중:");
                    log.info("  - 계좌번호: {}", account.get("accountNumber"));
                    log.info("  - 계좌타입: {}", account.get("accountType"));
                    log.info("  - 잔액: {}", account.get("balance"));

                    accountDetails.add(IntegratedCustomerInfoResponse.BankInfo.AccountInfo.builder()
                            .accountNumber(account.get("accountNumber") != null ? account.get("accountNumber").toString() : "")
                            .accountType(account.get("accountType") != null ? account.get("accountType").toString() : "")
                            .accountName(account.get("accountName") != null ? account.get("accountName").toString() : "")
                            .balance(account.get("balance") != null ? new BigDecimal(account.get("balance").toString()) : BigDecimal.ZERO)
                            .currency(account.get("currency") != null ? account.get("currency").toString() : "KRW")
                            .openDate(account.get("openDate") != null ? java.time.LocalDateTime.parse(account.get("openDate").toString()) : null)
                            .isActive(account.get("isActive") != null ? (Boolean) account.get("isActive") : true)
                            .build());

                    // 입출금 계좌를 ProductDetail로도 추가
                    if ("DEMAND_DEPOSIT".equals(account.get("accountType"))) {
                        log.info("🏦 입출금 계좌를 ProductDetail로 추가:");
                        log.info("  - 계좌번호: {}", account.get("accountNumber"));
                        log.info("  - 계좌명: {}", account.get("accountName"));
                        log.info("  - 잔액: {}", account.get("balance"));
                        
                        productDetails.add(IntegratedCustomerInfoResponse.BankInfo.ProductDetail.builder()
                                .productCode(account.get("accountNumber") != null ? account.get("accountNumber").toString() : "")
                                .productName(account.get("accountName") != null ? account.get("accountName").toString() : "입출금예금")
                                .productType("DEMAND_DEPOSIT")
                                .amount(account.get("balance") != null ? new BigDecimal(account.get("balance").toString()) : BigDecimal.ZERO)
                                .subscriptionDate(account.get("openDate") != null ? java.time.LocalDateTime.parse(account.get("openDate").toString()) : null)
                                .status(account.get("isActive") != null && (Boolean) account.get("isActive") ? "ACTIVE" : "INACTIVE")
                                .build());
                    }

                    if (account.get("balance") != null) {
                        totalBalance = totalBalance.add(new BigDecimal(account.get("balance").toString()));
                    }
                }
            }

            List<String> mainProducts = products != null ?
                    products.stream()
                            .map(product -> product.get("productName").toString())
                            .limit(3)
                            .collect(java.util.stream.Collectors.toList()) : new ArrayList<>();

            // 상품별 상세 정보 저장 (대출 정보 포함)
            if (products != null) {
                for (Map<String, Object> product : products) {
                    log.info("🏦 상품 상세 정보 처리 중:");
                    log.info("  - 상품코드: {}", product.get("productCode"));
                    log.info("  - 상품명: {}", product.get("productName"));
                    log.info("  - 상품타입: {}", product.get("productType"));
                    log.info("  - 금액: {}", product.get("amount"));
                    log.info("  - 잔여금액: {}", product.get("remainingAmount"));
                    log.info("  - 적용금리: {}", product.get("interestRate"));
                    log.info("  - 기본금리: {}", product.get("baseRate"));
                    log.info("  - 우대금리: {}", product.get("preferentialRate"));
                    log.info("  - 월상환금: {}", product.get("monthlyPayment"));
                    log.info("  - 시작일: {}", product.get("startDate"));
                    log.info("  - 만기일: {}", product.get("maturityDate"));
                    log.info("  - 상태: {}", product.get("status"));
                    
                    productDetails.add(IntegratedCustomerInfoResponse.BankInfo.ProductDetail.builder()
                            .productCode(product.get("productCode") != null ? product.get("productCode").toString() : "")
                            .productName(product.get("productName") != null ? product.get("productName").toString() : "")
                            .productType(product.get("productType") != null ? product.get("productType").toString() : "")
                            .amount(product.get("amount") != null ? new BigDecimal(product.get("amount").toString()) : BigDecimal.ZERO)
                            .remainingAmount(product.get("remainingAmount") != null ? new BigDecimal(product.get("remainingAmount").toString()) : null)
                            .interestRate(product.get("interestRate") != null ? new BigDecimal(product.get("interestRate").toString()) : null)
                            .baseRate(product.get("baseRate") != null ? new BigDecimal(product.get("baseRate").toString()) : null)
                            .preferentialRate(product.get("preferentialRate") != null ? new BigDecimal(product.get("preferentialRate").toString()) : null)
                            .monthlyPayment(product.get("monthlyPayment") != null ? new BigDecimal(product.get("monthlyPayment").toString()) : null)
                            .startDate(product.get("startDate") != null ? java.time.LocalDateTime.parse(product.get("startDate").toString()) : null)
                            .maturityDate(product.get("maturityDate") != null ? java.time.LocalDateTime.parse(product.get("maturityDate").toString()) : null)
                            .subscriptionDate(product.get("subscriptionDate") != null ? java.time.LocalDateTime.parse(product.get("subscriptionDate").toString()) : null)
                            .status(product.get("status") != null ? product.get("status").toString() : "UNKNOWN")
                            .build());
                }
            }

            // 상품 정보를 로그에 출력 (디버깅용)
            if (products != null) {
                log.info("하나은행 상품 정보:");
                for (Map<String, Object> product : products) {
                    log.info("  - 상품명: {}, 타입: {}, 금액: {}", 
                        product.get("productName"), 
                        product.get("productType"), 
                        product.get("amount"));
                }
            }

            return IntegratedCustomerInfoResponse.BankInfo.builder()
                    .isAvailable(true)
                    .customerGrade(customerInfo != null && customerInfo.get("customerGrade") != null ? 
                            customerInfo.get("customerGrade").toString() : "STANDARD")
                    .accountCount(accounts != null ? accounts.size() : 0)
                    .productCount(products != null ? products.size() : 0)
                    .totalBalance(totalBalance)
                    .mainProducts(mainProducts)
                    .accounts(accountDetails)
                    .productDetails(productDetails)
                    .build();

        } catch (Exception e) {
            log.error("하나은행 응답 파싱 실패", e);
            return createErrorBankInfo("응답 데이터 파싱 실패");
        }
    }

    private IntegratedCustomerInfoResponse.BankInfo createErrorBankInfo(String errorMessage) {
        return IntegratedCustomerInfoResponse.BankInfo.builder()
                .isAvailable(false)
                .customerGrade("STANDARD")
                .accountCount(0)
                .productCount(0)
                .totalBalance(BigDecimal.ZERO)
                .mainProducts(new ArrayList<>())
                .accounts(new ArrayList<>())
                .productDetails(new ArrayList<>())
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 하나카드 응답 파싱 - 실제 카드 데이터 포함
     */
    private IntegratedCustomerInfoResponse.CardInfo parseCardResponse(Map<String, Object> response) {
        try {
            log.info("💳 하나카드 응답 파싱 시작");
            log.info("💳 응답 데이터: {}", response);
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                log.warn("💳 응답 데이터가 null입니다");
                return IntegratedCustomerInfoResponse.CardInfo.builder()
                        .isAvailable(false)
                        .errorMessage("응답 데이터가 없습니다")
                        .build();
            }
            
            log.info("💳 파싱된 데이터: {}", data);
            
            // 카드 데이터 직접 저장
            java.util.Map<String, Object> cardData = new java.util.HashMap<>();
            cardData.put("cards", data.get("cards"));
            cardData.put("summary", data.get("summary"));
            cardData.put("responseTime", data.get("responseTime"));
            
            log.info("💳 저장된 카드 데이터: {}", cardData);
            
            // 기존 로직 유지 (호환성을 위해)
            Map<String, Object> customerInfo = (Map<String, Object>) data.get("customerInfo");
            List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");
            Map<String, Object> hanamoneyInfo = (Map<String, Object>) data.get("hanamoneyInfo");

            List<String> mainCards = cards != null ?
                    cards.stream()
                            .map(card -> card.get("cardName").toString())
                            .limit(3)
                            .collect(java.util.stream.Collectors.toList()) : new ArrayList<>();

            log.info("💳 파싱 완료 - 카드 수: {}", cards != null ? cards.size() : 0);

            return IntegratedCustomerInfoResponse.CardInfo.builder()
                    .isAvailable(true)
                    .customerGrade(customerInfo != null ? customerInfo.get("customerGrade").toString() : "BRONZE")
                    .cardCount(cards != null ? cards.size() : 0)
                    .totalCreditLimit(customerInfo != null ? new BigDecimal(customerInfo.get("totalCreditLimit").toString()) : BigDecimal.ZERO)
                    .availableCredit(customerInfo != null ? new BigDecimal(customerInfo.get("totalCreditLimit").toString())
                            .subtract(new BigDecimal(customerInfo.get("usedCreditAmount").toString())) : BigDecimal.ZERO)
                    .hasHanamoney(hanamoneyInfo != null && Boolean.parseBoolean(hanamoneyInfo.get("isSubscribed").toString()))
                    .hanamoneyPoints(hanamoneyInfo != null ? new BigDecimal(hanamoneyInfo.get("currentPoints").toString()) : BigDecimal.ZERO)
                    .hanamoneyLevel(hanamoneyInfo != null ? hanamoneyInfo.get("membershipLevel").toString() : "BRONZE")
                    .mainCards(mainCards)
                    .cardData(cardData) // 실제 카드 데이터 저장
                    .build();

        } catch (Exception e) {
            log.error("💳 하나카드 응답 파싱 실패", e);
            return IntegratedCustomerInfoResponse.CardInfo.builder()
                    .isAvailable(false)
                    .errorMessage("응답 데이터 파싱 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 전체 고객 등급 결정
     */
    private String determineOverallGrade(
            IntegratedCustomerInfoResponse.BankInfo bankInfo,
            IntegratedCustomerInfoResponse.CardInfo cardInfo) {

        List<String> grades = new ArrayList<>();
        
        if (bankInfo != null && bankInfo.isAvailable()) {
            grades.add(bankInfo.getCustomerGrade());
        }
        
        if (cardInfo != null && cardInfo.isAvailable()) {
            grades.add(cardInfo.getCustomerGrade());
        }

        // 가장 높은 등급으로 설정
        if (grades.contains("DIAMOND") || grades.contains("VIP")) return "DIAMOND";
        if (grades.contains("PLATINUM") || grades.contains("GOLD")) return "PLATINUM";
        if (grades.contains("GOLD") || grades.contains("SILVER")) return "GOLD";
        return "SILVER";
    }

    /**
     * 통합 혜택 정보 생성
     */
    private IntegratedCustomerInfoResponse.IntegratedBenefits buildIntegratedBenefits(
            IntegratedCustomerInfoResponse.BankInfo bankInfo,
            IntegratedCustomerInfoResponse.CardInfo cardInfo) {

        List<String> benefits = new ArrayList<>();
        List<String> recommendedProducts = new ArrayList<>();
        BigDecimal totalPoints = BigDecimal.ZERO;

        // 은행 혜택
        if (bankInfo != null && bankInfo.isAvailable()) {
            benefits.add("하나원큐 통합 금융 서비스");
            if ("VIP".equals(bankInfo.getCustomerGrade())) {
                benefits.add("VIP 우대 금리");
            }
            recommendedProducts.add("하나 정기예금");
        }

        // 카드 혜택
        if (cardInfo != null && cardInfo.isAvailable()) {
            benefits.add("하나카드 통합 혜택");
            totalPoints = totalPoints.add(cardInfo.getHanamoneyPoints());
            if (cardInfo.isHasHanamoney()) {
                benefits.add("하나머니 포인트 적립");
            }
            recommendedProducts.add("하나 그린카드");
        }

        // 그린월드 혜택
        benefits.add("친환경 활동 포인트");
        benefits.add("그린 챌린지 참여");
        recommendedProducts.add("ESG 투자상품");

        String groupLevel = determineGroupLevel(bankInfo, cardInfo);
        boolean isPremiumEligible = "DIAMOND".equals(groupLevel) || "PLATINUM".equals(groupLevel);

        return IntegratedCustomerInfoResponse.IntegratedBenefits.builder()
                .groupCustomerLevel(groupLevel)
                .availableBenefits(benefits)
                .totalPoints(totalPoints)
                .recommendedProducts(recommendedProducts)
                .eligibleForPremiumService(isPremiumEligible)
                .build();
    }

    /**
     * 그룹 고객 레벨 결정
     */
    private String determineGroupLevel(
            IntegratedCustomerInfoResponse.BankInfo bankInfo,
            IntegratedCustomerInfoResponse.CardInfo cardInfo) {

        int score = 0;

        // 은행 점수
        if (bankInfo != null && bankInfo.isAvailable()) {
            score += bankInfo.getAccountCount() * 10;
            score += bankInfo.getProductCount() * 20;
            if ("VIP".equals(bankInfo.getCustomerGrade())) score += 100;
        }

        // 카드 점수
        if (cardInfo != null && cardInfo.isAvailable()) {
            score += cardInfo.getCardCount() * 15;
            if (cardInfo.isHasHanamoney()) score += 50;
            if ("DIAMOND".equals(cardInfo.getCustomerGrade())) score += 100;
        }

        if (score >= 200) return "DIAMOND";
        if (score >= 100) return "PLATINUM";
        if (score >= 50) return "GOLD";
        return "SILVER";
    }

    /**
     * 임시 CI 생성 (실제로는 본인인증 API 연동)
     */
    private String generateMockCI(Member member) {
        return "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
    }

    /**
     * 고객 동의 토큰 생성
     */
    private String generateConsentToken(Long memberId) {
        return "CONSENT_" + memberId + "_" + System.currentTimeMillis();
    }

    /**
     * 특정 상품 보유 여부 확인
     */
    public boolean checkProductOwnership(Long memberId, Long productId) {
        try {
            log.info("🔍 상품 보유 여부 확인 시작 - 회원ID: {}, 상품ID: {}", memberId, productId);

            // 회원 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

            // 그룹 고객 토큰 조회 또는 생성
            String groupCustomerToken = getOrCreateGroupCustomerToken(member);
            log.info("🔑 그룹 고객 토큰: {}", groupCustomerToken);

            // 하나은행 서비스에 상품 보유 여부 확인 요청
            String url = bankServiceUrl + "/api/integration/check-product-ownership";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", tokenService.generateInternalServiceToken());

            Map<String, Object> requestBody = Map.of(
                    "groupCustomerToken", groupCustomerToken,
                    "productId", productId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseData = (Map<String, Object>) response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseData.get("data");
                Boolean hasProduct = (Boolean) data.get("hasProduct");
                
                log.info("🔍 상품 보유 여부 확인 결과: {}", hasProduct);
                return hasProduct != null ? hasProduct : false;
            } else {
                log.error("🔍 하나은행 서비스 응답 오류: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("🔍 상품 보유 여부 확인 실패", e);
            return false;
        }
    }
}
