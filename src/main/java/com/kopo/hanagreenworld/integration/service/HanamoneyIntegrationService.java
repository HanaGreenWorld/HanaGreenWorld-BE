package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.dto.HanamoneyInfoRequest;
import com.kopo.hanagreenworld.integration.dto.HanamoneyInfoResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 하나머니 연동 서비스
 * 하나카드에서 하나머니 정보를 실시간으로 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HanamoneyIntegrationService {

    private final MemberRepository memberRepository;
    private final GroupIntegrationTokenService tokenService;
    private final RestTemplate restTemplate;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    /**
     * 하나머니 정보 조회
     */
    public HanamoneyInfoResponse getHanamoneyInfo(HanamoneyInfoRequest request) {
        try {
            // 1. 고객 동의 확인
            if (!Boolean.TRUE.equals(request.getCustomerConsent())) {
                throw new SecurityException("고객 동의가 필요합니다.");
            }

            // 2. 회원 정보 조회
            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

            // 3. 그룹 고객 토큰 조회 또는 생성
            String groupCustomerToken = getOrCreateGroupCustomerToken(member);

            // 4. 내부 서비스 토큰 생성
            String internalServiceToken = tokenService.generateInternalServiceToken();
            String customerInfoToken = tokenService.generateCustomerInfoToken(groupCustomerToken);
            String consentToken = generateConsentToken(member.getMemberId());

            // 5. 하나카드에서 하나머니 정보 조회
            HanamoneyInfoResponse response = getHanamoneyFromCard(internalServiceToken, customerInfoToken, consentToken);

            log.info("하나머니 정보 조회 완료 - 회원ID: {}, 잔액: {}", 
                    request.getMemberId(), response.getHanamoneyInfo().getCurrentBalance());

            return response;

        } catch (Exception e) {
            log.error("하나머니 정보 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("하나머니 정보 조회에 실패했습니다.", e);
        }
    }

    /**
     * 하나카드에서 하나머니 정보 조회
     */
    private HanamoneyInfoResponse getHanamoneyFromCard(String internalServiceToken, String customerInfoToken, String consentToken) {
        try {
            String url = cardServiceUrl + "/api/integration/hanamoney-info";
            
            log.info("하나카드 서비스 호출 시작 - URL: {}", url);
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            // 요청 바디 생성
            Map<String, String> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "consentToken", consentToken,
                    "infoType", "HANAMONEY"
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("하나카드 API 요청 - Headers: {}, Body: {}", headers, requestBody);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            log.info("하나카드 API 응답 - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseHanamoneyResponse(response.getBody());
            } else {
                throw new RuntimeException("하나머니 정보 조회 실패 - Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("하나카드 서비스 연결 실패", e);
            throw new RuntimeException("하나카드 서비스 연결에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 하나머니 응답 파싱
     */
    private HanamoneyInfoResponse parseHanamoneyResponse(Map<String, Object> response) {
        try {
            log.info("하나머니 응답 파싱 시작 - 응답: {}", response);
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            
            if (data == null) {
                throw new RuntimeException("응답 데이터가 null입니다.");
            }
            
            log.info("파싱할 데이터: {}", data);
            
            HanamoneyInfoResponse.HanamoneyInfo hanamoneyInfo = 
                    HanamoneyInfoResponse.HanamoneyInfo.builder()
                            .membershipId(data.get("membershipLevel").toString())
                            .currentBalance(new BigDecimal(data.get("currentPoints").toString()))
                            .totalEarned(new BigDecimal(data.get("accumulatedPoints").toString()))
                            .totalSpent(BigDecimal.ZERO) // 계산 필요
                            .membershipLevel(data.get("membershipLevel").toString())
                            .isActive(Boolean.parseBoolean(data.get("isSubscribed").toString()))
                            .joinDate(LocalDateTime.parse(data.get("joinDate").toString()))
                            .build();

            log.info("파싱된 하나머니 정보 - 잔액: {}, 총적립: {}", 
                    hanamoneyInfo.getCurrentBalance(), hanamoneyInfo.getTotalEarned());

            // 최근 거래 내역 (실제로는 별도 API 호출 필요)
            List<HanamoneyInfoResponse.TransactionInfo> transactions = getRecentTransactions();

            return HanamoneyInfoResponse.builder()
                    .hanamoneyInfo(hanamoneyInfo)
                    .recentTransactions(transactions)
                    .responseTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("하나머니 응답 파싱 실패", e);
            throw new RuntimeException("응답 데이터 파싱에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 최근 거래 내역 조회 (실제로는 별도 API 구현 필요)
     */
    private List<HanamoneyInfoResponse.TransactionInfo> getRecentTransactions() {
        List<HanamoneyInfoResponse.TransactionInfo> transactions = new ArrayList<>();
        
        // 임시 데이터 (실제로는 하나카드 API에서 조회)
        transactions.add(HanamoneyInfoResponse.TransactionInfo.builder()
                .transactionType("EARN")
                .amount(new BigDecimal("50000"))
                .balanceAfter(new BigDecimal("150000"))
                .description("카드 사용 적립")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build());
                
        transactions.add(HanamoneyInfoResponse.TransactionInfo.builder()
                .transactionType("SPEND")
                .amount(new BigDecimal("20000"))
                .balanceAfter(new BigDecimal("100000"))
                .description("스타벅스 결제")
                .transactionDate(LocalDateTime.now().minusDays(3))
                .build());

        return transactions;
    }

    /**
     * 그룹 고객 토큰 조회 또는 생성
     */
    private String getOrCreateGroupCustomerToken(Member member) {
        return tokenService.getGroupTokenByPhone(member.getPhoneNumber())
                .orElseGet(() -> {
                    String mockCi = generateMockCI(member);
                    return tokenService.createGroupCustomerToken(
                            mockCi,
                            member.getName(),
                            member.getPhoneNumber(),
                            member.getEmail(),
                            "19900315" // 실제로는 본인인증에서 획득
                    );
                });
    }

    /**
     * 임시 CI 생성
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
}
