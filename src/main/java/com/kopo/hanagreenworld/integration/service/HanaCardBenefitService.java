package com.kopo.hanagreenworld.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 하나카드 혜택 서비스 통합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HanaCardBenefitService {

    private final RestTemplate restTemplate;

    @Value("${integration.card.url}")
    private String hanacardApiBaseUrl;

    /**
     * 사용자의 혜택 패키지 목록 조회
     */
    public Map<String, Object> getBenefitPackages(Long userId) {
        log.info("하나카드에서 혜택 패키지 조회: userId={}", userId);
        
        try {
            String url = hanacardApiBaseUrl + "/api/card-benefits/users/" + userId + "/packages";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("하나카드 혜택 패키지 조회 성공: userId={}", userId);
                return response.getBody();
            } else {
                log.error("하나카드 혜택 패키지 조회 실패: userId={}, status={}", userId, response.getStatusCode());
                return createFallbackResponse();
            }
        } catch (Exception e) {
            log.error("하나카드 혜택 패키지 조회 중 오류 발생: userId={}", userId, e);
            return createFallbackResponse();
        }
    }

    /**
     * 혜택 패키지 변경
     */
    public Map<String, Object> changeBenefitPackage(Long userId, Long cardProductId, String packageCode, String changeReason) {
        log.info("하나카드에서 혜택 패키지 변경: userId={}, cardProductId={}, packageCode={}", userId, cardProductId, packageCode);
        
        try {
            String url = hanacardApiBaseUrl + "/api/card-benefits/users/" + userId + "/change?cardProductId=" + cardProductId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, String> requestBody = Map.of(
                    "packageCode", packageCode,
                    "changeReason", changeReason != null ? changeReason : "사용자 요청"
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("하나카드 혜택 패키지 변경 성공: userId={}, packageCode={}", userId, packageCode);
                return response.getBody();
            } else {
                log.error("하나카드 혜택 패키지 변경 실패: userId={}, status={}", userId, response.getStatusCode());
                return Map.of("success", false, "message", "혜택 패키지 변경에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("하나카드 혜택 패키지 변경 중 오류 발생: userId={}", userId, e);
            return Map.of("success", false, "message", "혜택 패키지 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 현재 활성화된 혜택 패키지 조회
     */
    public Map<String, Object> getCurrentBenefitPackage(Long userId, Long cardProductId) {
        log.info("하나카드에서 현재 혜택 패키지 조회: userId={}, cardProductId={}", userId, cardProductId);
        
        try {
            String url = hanacardApiBaseUrl + "/api/card-benefits/users/" + userId + "/current?cardProductId=" + cardProductId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("하나카드 현재 혜택 패키지 조회 성공: userId={}", userId);
                return response.getBody();
            } else {
                log.error("하나카드 현재 혜택 패키지 조회 실패: userId={}, status={}", userId, response.getStatusCode());
                return Map.of("packageName", "올인원 그린라이프");
            }
        } catch (Exception e) {
            log.error("하나카드 현재 혜택 패키지 조회 중 오류 발생: userId={}", userId, e);
            return Map.of("packageName", "올인원 그린라이프");
        }
    }

    /**
     * 폴백 응답 생성 (하나카드 API 실패 시)
     */
    private Map<String, Object> createFallbackResponse() {
        return Map.of(
                "currentPackage", "올인원 그린라이프",
                "packages", java.util.List.of(
                        Map.of(
                                "packageName", "올인원 그린라이프 캐시백",
                                "packageDescription", "친환경 생활 종합 혜택",
                                "packageIcon", "hanaIcon3d_17.png",
                                "maxCashback", "최대 4% 캐시백",
                                "isActive", true,
                                "benefits", java.util.List.of(
                                        Map.of("category", "전기차 충전소", "cashbackRate", "3%", "description", "완속/급속 충전소", "icon", "hanaIcon3d_65.png"),
                                        Map.of("category", "대중교통", "cashbackRate", "2%", "description", "지하철, 버스", "icon", "hanaIcon3d_67.png"),
                                        Map.of("category", "공유킥보드, 따릉이", "cashbackRate", "4%", "description", "공유 모빌리티", "icon", "hanaIcon3d_69.png")
                                )
                        )
                )
        );
    }
}
