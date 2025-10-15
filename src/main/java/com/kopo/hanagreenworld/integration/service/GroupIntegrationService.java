package com.kopo.hanagreenworld.integration.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroupIntegrationService {

    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate;

    @Value("${integration.bank.url}")
    private String bankServiceUrl;

    @Value("${integration.card.url}")
    private String cardServiceUrl;
    
    @Value("${internal.service.secret}")
    private String secret;

    public IntegratedCustomerInfoResponse getIntegratedCustomerInfo(IntegratedCustomerInfoRequest request) {
        try {
            // 고객 동의 확인
            if (!Boolean.TRUE.equals(request.getCustomerConsent())) {
                throw new SecurityException("고객 동의가 필요합니다.");
            }

            // 회원 정보 조회
            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

            // CI 추출
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }

            // 내부 서비스 토큰 생성
            String internalServiceToken = generateInternalServiceToken();

            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String consentToken = generateConsentToken(member.getMemberId());

            IntegratedCustomerInfoResponse.BankInfo bankInfo = null;
            IntegratedCustomerInfoResponse.CardInfo cardInfo = null;

            List<String> targetServices = Arrays.asList(request.getTargetServices());

            if (targetServices.contains("BANK") || targetServices.contains("ALL")) {
                bankInfo = getBankInfo(internalServiceToken, customerInfoToken, consentToken, request.getInfoType());
            }

            if (targetServices.contains("CARD") || targetServices.contains("ALL")) {
                cardInfo = getCardInfo(internalServiceToken, customerInfoToken, consentToken, request.getInfoType());
            }

            return buildIntegratedResponse(member, bankInfo, cardInfo);

        } catch (Exception e) {
            throw new RuntimeException("통합 고객 정보 조회에 실패했습니다.", e);
        }
    }

    private IntegratedCustomerInfoResponse.BankInfo getBankInfo(String internalServiceToken, String customerInfoToken, String consentToken, String infoType) {
        try {
            String url = bankServiceUrl + "/api/integration/customer-info";

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            // 요청 바디 생성
            Map<String, String> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "consentToken", consentToken,
                    "infoType", infoType != null ? infoType : "ALL"
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                IntegratedCustomerInfoResponse.BankInfo bankInfo = parseBankResponse(response.getBody());
                return bankInfo;
            } else {
                return IntegratedCustomerInfoResponse.BankInfo.builder()
                        .isAvailable(false)
                        .errorMessage("하나은행 정보 조회 실패")
                        .build();
            }

        } catch (Exception e) {
            return IntegratedCustomerInfoResponse.BankInfo.builder()
                    .isAvailable(false)
                    .errorMessage("하나은행 서비스 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    IntegratedCustomerInfoResponse.CardInfo getCardInfo(String internalServiceToken, String customerInfoToken, String consentToken, String infoType) {
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
                        .hasBankAccount(bankInfo != null && bankInfo.isAvailable())
                        .hasCardAccount(cardInfo != null && cardInfo.isAvailable())
                        .hasGreenWorldAccount(true)
                        .firstJoinDate(member.getCreatedAt())
                        .build();

        return IntegratedCustomerInfoResponse.builder()
                .customerSummary(customerSummary)
                .bankInfo(bankInfo)
                .cardInfo(cardInfo)
                .responseTime(LocalDateTime.now())
                .build();
    }

    private IntegratedCustomerInfoResponse.BankInfo parseBankResponse(Map<String, Object> response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
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

                    accountDetails.add(IntegratedCustomerInfoResponse.BankInfo.AccountInfo.builder()
                            .accountNumber(account.get("accountNumber") != null ? account.get("accountNumber").toString() : "")
                            .accountType(account.get("accountType") != null ? account.get("accountType").toString() : "")
                            .accountName(account.get("accountName") != null ? account.get("accountName").toString() : "")
                            .balance(account.get("balance") != null ? new BigDecimal(account.get("balance").toString()) : BigDecimal.ZERO)
                            .currency(account.get("currency") != null ? account.get("currency").toString() : "KRW")
                            .openDate(account.get("openDate") != null ? java.time.LocalDateTime.parse(account.get("openDate").toString()) : null)
                            .isActive(account.get("isActive") != null ? (Boolean) account.get("isActive") : true)
                            .build());

                    if ("DEMAND_DEPOSIT".equals(account.get("accountType"))) {
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

            if (products != null) {
                for (Map<String, Object> product : products) {
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

    private IntegratedCustomerInfoResponse.CardInfo parseCardResponse(Map<String, Object> response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return IntegratedCustomerInfoResponse.CardInfo.builder()
                        .isAvailable(false)
                        .errorMessage("응답 데이터가 없습니다")
                        .build();
            }

            // 카드 데이터 직접 저장
            java.util.Map<String, Object> cardData = new java.util.HashMap<>();
            cardData.put("cards", data.get("cards"));
            cardData.put("summary", data.get("summary"));
            cardData.put("responseTime", data.get("responseTime"));

            // 기존 로직 유지 (호환성을 위해)
            Map<String, Object> customerInfo = (Map<String, Object>) data.get("customerInfo");
            List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");
            Map<String, Object> hanamoneyInfo = (Map<String, Object>) data.get("hanamoneyInfo");

            List<String> mainCards = cards != null ?
                    cards.stream()
                            .map(card -> card.get("cardName").toString())
                            .limit(3)
                            .collect(java.util.stream.Collectors.toList()) : new ArrayList<>();

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
                    .cardData(cardData)
                    .build();

        } catch (Exception e) {
            return IntegratedCustomerInfoResponse.CardInfo.builder()
                    .isAvailable(false)
                    .errorMessage("응답 데이터 파싱 실패: " + e.getMessage())
                    .build();
        }
    }

    private String generateMockCI(Member member) {
        return "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
    }

    private String generateConsentToken(Long memberId) {
        return "CONSENT_" + memberId + "_" + System.currentTimeMillis();
    }

    public boolean checkProductOwnership(Long memberId, Long productId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

            // CI 추출
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나은행 서비스에 상품 보유 여부 확인 요청
            String url = bankServiceUrl + "/api/integration/check-product-ownership";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", generateInternalServiceToken());

            Map<String, Object> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "productId", productId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseData = (Map<String, Object>) response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseData.get("data");
                Boolean hasProduct = (Boolean) data.get("hasProduct");

                return hasProduct != null ? hasProduct : false;
            } else {
                return false;
            }

        } catch (Exception e) {
            log.error("상품 보유 여부 확인 실패", e);
            return false;
        }
    }

    public String generateInternalServiceToken() {
        // 고정 시크릿을 Base64로 인코딩
        return Base64.getEncoder().encodeToString(secret.getBytes());
    }
}
