package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.dto.CardTransactionResponse;
import com.kopo.hanagreenworld.integration.dto.CardConsumptionSummaryResponse;
import com.kopo.hanagreenworld.integration.dto.CardIntegratedInfoResponse;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionIntegrationService {

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final GroupIntegrationTokenService groupIntegrationTokenService;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    /**
     * 카드 거래내역 조회
     */
    public List<CardTransactionResponse> getCardTransactions(Long memberId) {
        try {
            log.info("하나카드 서버에서 거래내역 조회 시작 - 회원ID: {}", memberId);

            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 기반 통합 인증 토큰 생성 (단일 토큰으로 통신)
            String unifiedAuthToken = groupIntegrationTokenService.generateUnifiedAuthToken(member.getPhoneNumber());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/transactions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "aGFuYS1pbnRlcm5hbC1zZXJ2aWNlLTIwMjQ=");
            headers.set("X-Customer-Info", unifiedAuthToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("🔍 [하나그린세상] 하나카드 서버 거래내역 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            log.info("🔍 [하나그린세상] 요청 헤더: {}", headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L) // 임시 ID (하나카드 응답에 id 필드가 없음)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }
                
                log.info("하나카드 서버에서 거래내역 조회 성공 - 건수: {}", result.size());
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

    /**
     * 월간 소비현황 조회
     */
    public CardConsumptionSummaryResponse getMonthlyConsumptionSummary(Long memberId) {
        try {
            log.info("하나카드 서버에서 월간 소비현황 조회 시작 - 회원ID: {}", memberId);

            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 기반 통합 인증 토큰 생성 (단일 토큰으로 통신)
            String unifiedAuthToken = groupIntegrationTokenService.generateUnifiedAuthToken(member.getPhoneNumber());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "aGFuYS1pbnRlcm5hbC1zZXJ2aWNlLTIwMjQ=");
            headers.set("X-Customer-Info", unifiedAuthToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("🔍 [하나그린세상] 하나카드 서버 월간 소비현황 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            log.info("🔍 [하나그린세상] 요청 헤더: {}", headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                // categoryAmounts 안전하게 변환
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
                        .recentTransactions(new ArrayList<>()) // TODO: 실제 데이터 매핑
                        .build();
                
                log.info("하나카드 서버에서 월간 소비현황 조회 성공");
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

    /**
     * 월간 소비현황 조회
     */
    public CardConsumptionSummaryResponse getConsumptionSummary(Long memberId) {
        try {
            log.info("하나카드 서버에서 월간 소비현황 조회 시작 - 회원ID: {}", memberId);
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));
            String unifiedAuthToken = groupIntegrationTokenService.generateUnifiedAuthToken(member.getPhoneNumber());
            String url = cardServiceUrl + "/api/integration/cards/" + memberId + "/consumption/summary";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "aGFuYS1pbnRlcm5hbC1zZXJ2aWNlLTIwMjQ=");
            headers.set("X-Customer-Info", unifiedAuthToken);
            headers.set("X-Requesting-Service", "GREEN_WORLD");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("🔍 [하나그린세상] 하나카드 서버 월간 소비현황 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            log.info("🔍 [하나그린세상] 요청 헤더: {}", headers);

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

                log.info("하나카드 서버에서 월간 소비현황 조회 성공 - 총소비: {}, 총캐시백: {}", 
                        result.getTotalAmount(), result.getTotalCashback());
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

    /**
     * 카테고리별 거래내역 조회
     */
    public List<CardTransactionResponse> getTransactionsByCategory(Long memberId, String category) {
        try {
            log.info("하나카드 서버에서 카테고리별 거래내역 조회 시작 - 회원ID: {}, 카테고리: {}", memberId, category);

            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // 하나카드 서버 API 호출을 위한 토큰 생성
            String groupCustomerToken = groupIntegrationTokenService.getGroupTokenByPhone(member.getPhoneNumber())
                    .orElseGet(() -> groupIntegrationTokenService.createGroupCustomerToken(
                            "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode(),
                            member.getName(),
                            member.getPhoneNumber(),
                            member.getEmail(),
                            "19900315" // Placeholder
                    ));
            // CI 기반 통합 인증 토큰 생성 (단일 토큰으로 통신)
            String unifiedAuthToken = groupIntegrationTokenService.generateUnifiedAuthToken(member.getPhoneNumber());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/cards/user/" + memberId + "/transactions/category/" + category;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "aGFuYS1pbnRlcm5hbC1zZXJ2aWNlLTIwMjQ=");
            headers.set("X-Customer-Info", unifiedAuthToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("하나카드 서버 카테고리별 거래내역 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) data.get("transactions");
                
                List<CardTransactionResponse> result = new ArrayList<>();
                if (transactions != null) {
                    for (Map<String, Object> transaction : transactions) {
                        result.add(CardTransactionResponse.builder()
                                .id(0L) // 임시 ID (하나카드 응답에 id 필드가 없음)
                                .transactionDate(transaction.get("transactionDate").toString())
                                .merchantName(transaction.get("merchantName").toString())
                                .category(transaction.get("category").toString())
                                .amount(Long.valueOf(transaction.get("amount").toString()))
                                .cashbackAmount(transaction.get("cashbackAmount") != null ? 
                                    Long.valueOf(transaction.get("cashbackAmount").toString()) : 0L)
                                .build());
                    }
                }
                
                log.info("하나카드 서버에서 카테고리별 거래내역 조회 성공 - 건수: {}", result.size());
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

    /**
     * 카드 목록 조회
     */
    public CardIntegratedInfoResponse.CardListInfo getCardList(Long memberId) {
        try {
            log.info("하나카드 서버에서 카드 목록 조회 시작 - 회원ID: {}", memberId);

            // Member 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // CI 기반 통합 인증 토큰 생성
            String unifiedAuthToken = groupIntegrationTokenService.generateUnifiedAuthToken(member.getPhoneNumber());

            // 하나카드 서버 API 호출
            String url = cardServiceUrl + "/api/integration/cards/" + memberId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "aGFuYS1pbnRlcm5hbC1zZXJ2aWNlLTIwMjQ=");
            headers.set("X-Customer-Info", unifiedAuthToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("🔍 [하나그린세상] 하나카드 서버 카드 목록 조회 요청 - URL: {}, 회원ID: {}", url, memberId);
            log.info("🔍 [하나그린세상] 요청 헤더: {}", headers);
            
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
                    
                    log.info("하나카드 서버에서 카드 목록 조회 성공 - 회원ID: {}, 카드 수: {}", memberId, cards.size());
                    
                    // 첫 번째 카드를 주 카드로 사용
                    String primaryCardName = "";
                    String primaryCardType = "";
                    if (!cards.isEmpty()) {
                        Map<String, Object> primaryCard = cards.get(0);
                        primaryCardName = (String) primaryCard.get("cardName");
                        primaryCardType = (String) primaryCard.get("cardType");
                    }
                    
                    // 실제 카드 목록 매핑 💳
                    List<CardIntegratedInfoResponse.CardDetail> cardDetails = new ArrayList<>();
                    for (Map<String, Object> cardData : cards) {
                        // 날짜 문자열을 LocalDateTime으로 파싱
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
                        
                        CardIntegratedInfoResponse.CardDetail cardDetail = CardIntegratedInfoResponse.CardDetail.builder()
                                .cardNumber((String) cardData.get("cardNumber"))
                                .cardName((String) cardData.get("cardName"))
                                .cardType((String) cardData.get("cardType"))
                                .cardStatus((String) cardData.get("cardStatus"))
                                .creditLimit(((Number) cardData.getOrDefault("creditLimit", 0)).longValue())
                                .availableLimit(((Number) cardData.getOrDefault("availableLimit", 0)).longValue())
                                .monthlyUsage(((Number) cardData.getOrDefault("monthlyUsage", 0)).longValue())
                                .cardImageUrl((String) cardData.get("cardImageUrl"))
                                .issueDate(issueDate)
                                .expiryDate(expiryDate)
                                .benefits((List<String>) cardData.get("benefits"))
                                .build();
                        cardDetails.add(cardDetail);
                        
                        log.info("💳 카드 상세 매핑 완료 - 카드명: {}, 카드번호: {}, 이미지URL: {}", 
                                cardDetail.getCardName(), cardDetail.getCardNumber(), cardDetail.getCardImageUrl());
                    }
                    
                    return CardIntegratedInfoResponse.CardListInfo.builder()
                            .totalCards(((Number) summary.getOrDefault("totalCardCount", 0)).longValue())
                            .totalCreditLimit(((Number) summary.getOrDefault("totalCreditLimit", 0)).longValue())
                            .usedAmount(((Number) summary.getOrDefault("monthlyTotalUsage", 0)).longValue())
                            .availableLimit(((Number) summary.getOrDefault("totalAvailableLimit", 0)).longValue())
                            .primaryCardName(primaryCardName)
                            .primaryCardType(primaryCardType)
                            .cards(cardDetails) // 실제 카드 목록 추가
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

    /**
     * 카드 통합 정보 조회 🎯
     * 카드 목록, 거래내역, 소비현황을 한 번에 조회
     */
    public CardIntegratedInfoResponse getCardIntegratedInfo(Long memberId) {
        try {
            log.info("카드 통합 정보 조회 시작 - 회원ID: {}", memberId);

            // 1. 카드 거래내역 조회
            List<CardTransactionResponse> transactions = getCardTransactions(memberId);
            
            // 2. 월간 소비현황 조회
            CardConsumptionSummaryResponse consumptionSummary = getConsumptionSummary(memberId);
            
            // 3. 친환경 혜택 정보 조회 (임시 데이터)
            Map<String, Object> ecoBenefits = new HashMap<>();
            ecoBenefits.put("totalEcoAmount", 6500L);
            ecoBenefits.put("totalEcoCashback", 105L);
            ecoBenefits.put("ecoCategories", Map.of("교통", 1500, "카페", 5000));
            ecoBenefits.put("ecoScore", 85);
            ecoBenefits.put("monthlyGoal", 10000L);
            ecoBenefits.put("achievementRate", 65.0);
            
            // 4. 카드 목록 조회 (하나카드 서버에서)
            CardIntegratedInfoResponse.CardListInfo cardList = getCardList(memberId);

            CardIntegratedInfoResponse response = CardIntegratedInfoResponse.builder()
                    .cardList(cardList)
                    .transactions(transactions)
                    .consumptionSummary(consumptionSummary)
                    .ecoBenefits(ecoBenefits)
                    .build();

            log.info("카드 통합 정보 조회 성공 - 회원ID: {}, 거래건수: {}", memberId, transactions.size());
            return response;

        } catch (Exception e) {
            log.error("카드 통합 정보 조회 실패 - 회원ID: {}", memberId, e);
            
            // 에러 시 빈 응답 반환
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
                    .build();
        }
    }
}
