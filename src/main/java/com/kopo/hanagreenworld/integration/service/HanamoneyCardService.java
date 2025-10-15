package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HanamoneyCardService {

    private final RestTemplate restTemplate;
    private final GroupIntegrationService groupIntegrationService;

    @Value("${integration.card.url}")
    private String cardServiceUrl;

    public boolean earnHanamoney(Member member, Long amount, String description) {
        try {
            String url = cardServiceUrl + "/api/integration/hanamoney-earn";

            // CI 추출 및 customerInfoToken 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
            }

            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String internalServiceToken = groupIntegrationService.generateInternalServiceToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            Map<String, Object> requestBody = Map.of(
                    "customerInfoToken", customerInfoToken,
                    "requestingService", "GREEN_WORLD",
                    "amount", amount,
                    "description", description
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

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

}


