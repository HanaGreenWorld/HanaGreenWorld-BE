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
import java.util.Base64;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HanamoneyIntegrationService {

    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate;
    private final GroupIntegrationService groupIntegrationService;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    public HanamoneyInfoResponse getHanamoneyInfo(HanamoneyInfoRequest request) {
        try {
            // 고객 동의 확인
            if (!Boolean.TRUE.equals(request.getCustomerConsent())) {
                throw new SecurityException("고객 동의가 필요합니다.");
            }

            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = generateMockCI(member);
            }

            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());

            String internalServiceToken = groupIntegrationService.generateInternalServiceToken();
            String consentToken = generateConsentToken(member.getMemberId());

            HanamoneyInfoResponse response = getHanamoneyFromCard(internalServiceToken, customerInfoToken, consentToken);

            return response;

        } catch (Exception e) {
            log.error("하나머니 정보 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("하나머니 정보 조회에 실패했습니다.", e);
        }
    }

    private HanamoneyInfoResponse getHanamoneyFromCard(String internalServiceToken, String customerInfoToken, String consentToken) {
        try {
            String url = cardServiceUrl + "/api/integration/hanamoney-info";

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

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

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

    private HanamoneyInfoResponse parseHanamoneyResponse(Map<String, Object> response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            
            if (data == null) {
                throw new RuntimeException("응답 데이터가 null입니다.");
            }
            
            HanamoneyInfoResponse.HanamoneyInfo hanamoneyInfo = 
                    HanamoneyInfoResponse.HanamoneyInfo.builder()
                            .membershipId(data.get("membershipLevel").toString())
                            .currentBalance(new BigDecimal(data.get("currentPoints").toString()))
                            .totalEarned(new BigDecimal(data.get("accumulatedPoints").toString()))
                            .totalSpent(BigDecimal.ZERO)
                            .membershipLevel(data.get("membershipLevel").toString())
                            .isActive(Boolean.parseBoolean(data.get("isSubscribed").toString()))
                            .joinDate(LocalDateTime.parse(data.get("joinDate").toString()))
                            .build();

            List<HanamoneyInfoResponse.TransactionInfo> transactions = getRecentTransactions();

            return HanamoneyInfoResponse.builder()
                    .hanamoneyInfo(hanamoneyInfo)
                    .recentTransactions(transactions)
                    .responseTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("응답 데이터 파싱에 실패했습니다: " + e.getMessage());
        }
    }

    private List<HanamoneyInfoResponse.TransactionInfo> getRecentTransactions() {
        List<HanamoneyInfoResponse.TransactionInfo> transactions = new ArrayList<>();

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

    private String generateMockCI(Member member) {
        return "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
    }

    private String generateConsentToken(Long memberId) {
        return "CONSENT_" + memberId + "_" + System.currentTimeMillis();
    }
}
