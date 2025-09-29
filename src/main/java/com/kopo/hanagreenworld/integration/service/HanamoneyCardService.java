package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 하나카드 서버와 하나머니 연동 서비스
 * 하나그린세상에서 하나카드 서버로 하나머니 관련 요청을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HanamoneyCardService {

    private final RestTemplate restTemplate;
    private final GroupIntegrationTokenService tokenService;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    /**
     * 하나카드 서버에서 하나머니 적립
     * 
     * @param member 회원 정보
     * @param amount 적립 금액
     * @param description 설명
     * @return 적립 성공 여부
     */
    public boolean earnHanamoney(Member member, Long amount, String description) {
        try {
            String url = cardServiceUrl + "/api/integration/hanamoney-earn";
            
            log.info("하나카드 서버로 하나머니 적립 요청 - URL: {}, 회원ID: {}, 금액: {}", 
                    url, member.getMemberId(), amount);
            
            // 토큰 생성
            String groupCustomerToken = getOrCreateGroupCustomerToken(member);
            String internalServiceToken = tokenService.generateInternalServiceToken();
            String customerInfoToken = tokenService.generateCustomerInfoToken(groupCustomerToken);
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            // 요청 바디 생성
            Map<String, Object> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "amount", amount,
                    "description", description
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("하나카드 API 요청 - Headers: {}, Body: {}", headers, requestBody);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            log.info("하나카드 API 응답 - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                
                if (Boolean.TRUE.equals(success)) {
                    log.info("하나머니 적립 성공 - 회원ID: {}, 금액: {}", member.getMemberId(), amount);
                    return true;
                } else {
                    log.error("하나머니 적립 실패 - 회원ID: {}, 응답: {}", member.getMemberId(), responseBody);
                    return false;
                }
            } else {
                log.error("하나머니 적립 API 호출 실패 - Status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("하나카드 서비스 연결 실패", e);
            return false;
        }
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
}


