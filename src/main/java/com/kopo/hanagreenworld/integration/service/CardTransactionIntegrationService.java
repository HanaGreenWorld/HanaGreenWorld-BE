package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.dto.CardTransactionResponse;
import com.kopo.hanagreenworld.integration.dto.CardConsumptionSummaryResponse;
import com.kopo.hanagreenworld.integration.dto.CardIntegratedInfoResponse;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionIntegrationService {

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final GroupIntegrationService groupIntegrationService;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    public List<CardTransactionResponse> getCardTransactions(Long memberId) {
        try {

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/transactions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }
                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 거래내역 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public CardConsumptionSummaryResponse getMonthlyConsumptionSummary(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                Map<String, Integer> categoryAmounts = new HashMap<>();
                if (data.get("categoryAmounts") != null) {
                    Map<String, Object> rawCategoryAmounts = (Map<String, Object>) data.get("categoryAmounts");
                    for (Map.Entry<String, Object> entry : rawCategoryAmounts.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            categoryAmounts.put(entry.getKey(), ((Number) value).intValue());
                        }
                    }
                }
                
                CardConsumptionSummaryResponse result = CardConsumptionSummaryResponse.builder()
                        .totalAmount(data.get("totalAmount") != null ? 
                            Long.valueOf(data.get("totalAmount").toString()) : 0L)
                        .totalCashback(data.get("totalCashback") != null ? 
                            Long.valueOf(data.get("totalCashback").toString()) : 0L)
                        .categoryAmounts(categoryAmounts)
                        .recentTransactions(new ArrayList<>())
                        .build();

                return result;
            } else {
                return CardConsumptionSummaryResponse.builder()
                        .totalAmount(0L)
                        .totalCashback(0L)
                        .categoryAmounts(new HashMap<>())
                        .recentTransactions(new ArrayList<>())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 월간 소비현황 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(0L)
                    .totalCashback(0L)
                    .categoryAmounts(new HashMap<>())
                    .recentTransactions(new ArrayList<>())
                    .build();
        }
    }

    public CardConsumptionSummaryResponse getConsumptionSummary(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
            
            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            headers.set("X-Requesting-Service", "GREEN_WORLD");

            HttpEntity<String> entity = new HttpEntity<>(headers);


            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                CardConsumptionSummaryResponse result = CardConsumptionSummaryResponse.builder()
                        .totalAmount(((Number) data.get("totalAmount")).longValue())
                        .totalCashback(((Number) data.get("totalCashback")).longValue())
                        .categoryAmounts((Map<String, Integer>) data.get("categoryAmounts"))
                        .recentTransactions(new ArrayList<>())
                        .build();

                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return CardConsumptionSummaryResponse.builder()
                        .totalAmount(0L)
                        .totalCashback(0L)
                        .categoryAmounts(new HashMap<>())
                        .recentTransactions(new ArrayList<>())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 월간 소비현황 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return CardConsumptionSummaryResponse.builder()
                    .totalAmount(0L)
                    .totalCashback(0L)
                    .categoryAmounts(new HashMap<>())
                    .recentTransactions(new ArrayList<>())
                    .build();
        }
    }

    public List<CardTransactionResponse> getTransactionsByCategory(Long memberId, String category) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/cards/user/" + memberId + "/transactions/category/" + category;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }

                return result;
            } else {
                log.warn("하나카드 서버 응답 오류 - Status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            log.error("하나카드 서버 카테고리별 거래내역 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public CardIntegratedInfoResponse.CardListInfo getCardList(Long memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId;
            log.info("하나카드 서버 API 호출 준비 - URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", groupIntegrationService.generateInternalServiceToken());
            headers.set("X-Customer-Info", customerInfoToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> summary = (Map<String, Object>) data.get("summary");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");

                    // 첫 번째 카드를 주 카드로 사용
                    String primaryCardName = "";
                    String primaryCardType = "";
                    if (!cards.isEmpty()) {
                        Map<String, Object> primaryCard = cards.get(0);
                        primaryCardName = (String) primaryCard.get("cardName");
                        primaryCardType = (String) primaryCard.get("cardType");
                    }
                    
                    // 실제 카드 목록 매핑
                    List<CardIntegratedInfoResponse.CardDetail> cardDetails = new ArrayList<>();
                    for (Map<String, Object> cardData : cards) {
                        java.time.LocalDateTime issueDate = null;
                        java.time.LocalDateTime expiryDate = null;
                        
                        try {
                            Object issueDateObj = cardData.get("issueDate");
                            if (issueDateObj instanceof String) {
                                issueDate = java.time.LocalDateTime.parse((String) issueDateObj);
                            } else if (issueDateObj instanceof java.time.LocalDateTime) {
                                issueDate = (java.time.LocalDateTime) issueDateObj;
                            }
                            
                            Object expiryDateObj = cardData.get("expiryDate");
                            if (expiryDateObj instanceof String) {
                                expiryDate = java.time.LocalDateTime.parse((String) expiryDateObj);
                            } else if (expiryDateObj instanceof java.time.LocalDateTime) {
                                expiryDate = (java.time.LocalDateTime) expiryDateObj;
                            }
                        } catch (Exception e) {
                            log.warn("날짜 파싱 실패 - issueDate: {}, expiryDate: {}", cardData.get("issueDate"), cardData.get("expiryDate"));
                        }

                        String imageUrl = (String) cardData.get("cardImageUrl");
                        log.info("카드 데이터 매핑 시작 - 카드명: {}, 이미지URL: {}", cardData.get("cardName"), imageUrl);

                        CardIntegratedInfoResponse.CardDetail cardDetail = CardIntegratedInfoResponse.CardDetail.builder()
                                .cardNumber((String) cardData.get("cardNumber"))
                                .cardName((String) cardData.get("cardName"))
                                .cardType((String) cardData.get("cardType"))
                                .cardStatus((String) cardData.get("cardStatus"))
                                .creditLimit(((Number) cardData.getOrDefault("creditLimit", 0)).longValue())
                                .availableLimit(((Number) cardData.getOrDefault("availableLimit", 0)).longValue())
                                .monthlyUsage(((Number) cardData.getOrDefault("monthlyUsage", 0)).longValue())
                                .cardImageUrl(imageUrl)
                                .issueDate(issueDate)
                                .expiryDate(expiryDate)
                                .benefits((List<String>) cardData.get("benefits"))
                                .build();
                        cardDetails.add(cardDetail);
                    }
                    
                    return CardIntegratedInfoResponse.CardListInfo.builder()
                            .totalCards(((Number) summary.getOrDefault("totalCardCount", 0)).longValue())
                            .totalCreditLimit(((Number) summary.getOrDefault("totalCreditLimit", 0)).longValue())
                            .usedAmount(((Number) summary.getOrDefault("monthlyTotalUsage", 0)).longValue())
                            .availableLimit(((Number) summary.getOrDefault("totalAvailableLimit", 0)).longValue())
                            .primaryCardName(primaryCardName)
                            .primaryCardType(primaryCardType)
                            .cards(cardDetails)
                            .build();
                }
            }
            
            log.warn("하나카드 서버에서 카드 목록 조회 실패 또는 빈 응답 - 회원ID: {}", memberId);
            
        } catch (Exception e) {
            log.error("하나카드 서버 카드 목록 조회 실패 - 회원ID: {}, 에러: {}", memberId, e.getMessage(), e);
        }
        
        // 실패 시 빈 카드 정보 반환
        return CardIntegratedInfoResponse.CardListInfo.builder()
                .totalCards(0L)
                .totalCreditLimit(0L)
                .usedAmount(0L)
                .availableLimit(0L)
                .primaryCardName("")
                .primaryCardType("")
                .cards(new ArrayList<>()) // 빈 카드 목록
                .build();
    }

    public CardIntegratedInfoResponse getCardIntegratedInfo(Long memberId) {
        try {
            log.info("카드 통합 정보 조회 시작 - 회원ID: {}", memberId);

            // 카드 거래내역 조회
            List<CardTransactionResponse> transactions = getCardTransactions(memberId);
            
            // 월간 소비현황 조회
            CardConsumptionSummaryResponse consumptionSummary = getConsumptionSummary(memberId);
            
            // 친환경 혜택 정보 조회
            Map<String, Object> ecoBenefits = getDefaultEcoBenefits();
            
            // 카드 목록 조회 (하나카드 서버에서)
            CardIntegratedInfoResponse.CardListInfo cardList = getCardList(memberId);

            CardIntegratedInfoResponse response = CardIntegratedInfoResponse.builder()
                    .cardList(cardList)
                    .transactions(transactions)
                    .consumptionSummary(consumptionSummary)
                    .ecoBenefits(ecoBenefits)
                    .build();

            return response;

        } catch (Exception e) {
            return CardIntegratedInfoResponse.builder()
                    .cardList(CardIntegratedInfoResponse.CardListInfo.builder()
                            .totalCards(0L)
                            .totalCreditLimit(0L)
                            .usedAmount(0L)
                            .availableLimit(0L)
                            .primaryCardName("")
                            .primaryCardType("")
                            .build())
                    .transactions(new ArrayList<>())
                    .consumptionSummary(CardConsumptionSummaryResponse.builder()
                            .totalAmount(0L)
                            .totalCashback(0L)
                            .categoryAmounts(new HashMap<>())
                            .build())
                    .ecoBenefits(getDefaultEcoBenefits())
                    .build();
        }
    }

    private Map<String, Object> getDefaultEcoBenefits() {
        Map<String, Object> benefits = new HashMap<>();
        benefits.put("totalBenefits", 2500L);
        benefits.put("benefits", new ArrayList<>());
        benefits.put("stats", Map.of(
            "totalEcoTransactions", 0,
            "totalEcoAmount", 0L,
            "totalAdditionalSeeds", 0L,
            "averageAdditionalSeeds", 0,
            "ecoMerchantCount", 0
        ));
        return benefits;
    }

    private String generateMockCI(Member member) {
        return "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
    }
}
