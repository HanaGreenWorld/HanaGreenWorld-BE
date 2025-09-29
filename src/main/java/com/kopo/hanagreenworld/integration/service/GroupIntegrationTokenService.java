package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.integration.domain.GroupCustomerMapping;
import com.kopo.hanagreenworld.integration.repository.GroupCustomerMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 하나금융그룹 내부 통합 토큰 서비스
 * CI를 안전하게 암호화하고 그룹 내부 토큰을 생성/관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupIntegrationTokenService {

    private final GroupCustomerMappingRepository groupCustomerMappingRepository;
    private final RestTemplate restTemplate;
    
    @Value("${encryption.key}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";

    @Value("${internal.service.secret}")
    private String secret;

    @Value("${integration.bank.url}")
    private String hanacardApiBaseUrl;

    @Value("${ci.default.token}")
    String defaultToken;

    /**
     * 새 고객의 그룹 통합 토큰 생성
     */
    public String createGroupCustomerToken(String ci, String name, String phoneNumber, String email, String birthDate) {
        try {
            // 이미 존재하는 고객인지 확인
            Optional<GroupCustomerMapping> existing = groupCustomerMappingRepository.findByPhoneNumberAndName(phoneNumber, name);
            if (existing.isPresent()) {
                log.info("기존 고객 토큰 반환: {}", existing.get().getGroupCustomerToken());
                return existing.get().getGroupCustomerToken();
            }

            // 새 토큰 생성
            String groupCustomerToken = UUID.randomUUID().toString();
            String encryptedCi = encryptCI(ci);

            GroupCustomerMapping mapping = GroupCustomerMapping.builder()
                    .groupCustomerToken(groupCustomerToken)
                    .encryptedCi(encryptedCi)
                    .name(name)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .birthDate(birthDate)
                    .build();

            groupCustomerMappingRepository.save(mapping);
            
            log.info("새 그룹 고객 토큰 생성 완료: {}", groupCustomerToken);
            return groupCustomerToken;

        } catch (Exception e) {
            log.error("그룹 고객 토큰 생성 실패", e);
            throw new RuntimeException("고객 토큰 생성에 실패했습니다.", e);
        }
    }

    /**
     * 그룹 토큰으로 고객 정보 조회
     */
    @Transactional(readOnly = true)
    public Optional<GroupCustomerMapping> getCustomerByToken(String groupCustomerToken) {
        return groupCustomerMappingRepository.findByGroupCustomerToken(groupCustomerToken);
    }

    /**
     * 휴대폰 번호로 그룹 토큰 조회
     */
    @Transactional(readOnly = true)
    public Optional<String> getGroupTokenByPhone(String phoneNumber) {
        return groupCustomerMappingRepository.findByPhoneNumber(phoneNumber)
                .map(GroupCustomerMapping::getGroupCustomerToken);
    }

    /**
     * 그룹사 계정 연결 상태 업데이트
     */
    public void updateAccountLinkStatus(String groupCustomerToken, String serviceType) {
        GroupCustomerMapping mapping = groupCustomerMappingRepository.findByGroupCustomerToken(groupCustomerToken)
                .orElseThrow(() -> new RuntimeException("고객 토큰을 찾을 수 없습니다: " + groupCustomerToken));

        switch (serviceType.toUpperCase()) {
            case "BANK":
                mapping.linkBankAccount();
                break;
            case "CARD":
                mapping.linkCardAccount();
                break;
            case "GREEN_WORLD":
                mapping.linkGreenWorldAccount();
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 서비스 타입: " + serviceType);
        }

        groupCustomerMappingRepository.save(mapping);
        log.info("계정 연결 상태 업데이트: {} - {}", groupCustomerToken, serviceType);
    }

    /**
     * CI 암호화 (실제 운영에서는 HSM 사용)
     */
    private String encryptCI(String ci) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(ci.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * CI 복호화 (내부 시스템에서만 사용, 외부 노출 금지)
     */
    private String decryptCI(String encryptedCi) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedCi));
        return new String(decryptedBytes);
    }

    /**
     * 그룹 내부 인증 토큰 생성 (API 호출 시 사용)
     */
    /**
     * 내부 서비스 인증 토큰 생성 (하나은행/하나카드용)
     */
    public String generateInternalServiceToken() {
        // 고정 시크릿을 Base64로 인코딩
        return Base64.getEncoder().encodeToString(secret.getBytes());
    }
    
    /**
     * CI 기반 통합 인증 토큰 생성 (그룹 간 통신용)
     * 이미 저장된 토큰을 우선 사용하고, 없으면 새로 생성
     */
    public String generateUnifiedAuthToken(String phoneNumber) {
        try {
            // 1. 기존 저장된 토큰 확인
            Optional<GroupCustomerMapping> existingMapping = groupCustomerMappingRepository.findByPhoneNumber(phoneNumber);
            if (existingMapping.isPresent() && existingMapping.get().getGroupCustomerToken() != null) {
                String existingToken = existingMapping.get().getGroupCustomerToken();
                log.info("기존 통합 토큰 재사용 - 전화번호: {}, 토큰: {}", phoneNumber, existingToken);
                return Base64.getEncoder().encodeToString(existingToken.getBytes());
            }
            
            // 2. 기존 토큰이 없으면 새로 생성
            log.info("새로운 통합 토큰 생성 - 전화번호: {}", phoneNumber);
            String phoneDigits = phoneNumber.replace("-", "");
            String ci = "CI_" + phoneDigits + "_" + Math.abs(phoneDigits.hashCode() % 10000);
            String unifiedToken = ci + "_UNIFIED";
            
            // 3. 양쪽 서버에 동일한 토큰 저장
            saveUnifiedTokenToBothServers(phoneNumber, unifiedToken);
            
            return Base64.getEncoder().encodeToString(unifiedToken.getBytes());
            
        } catch (Exception e) {
            log.error("통합 인증 토큰 생성 실패", e);
            // 기본값 반환 (hanabank에 등록된 전화번호와 일치)
            return Base64.getEncoder().encodeToString(defaultToken.getBytes());
        }
    }
    
    /**
     * 양쪽 서버에 동일한 통합 토큰 저장
     */
    private void saveUnifiedTokenToBothServers(String phoneNumber, String unifiedToken) {
        try {
            // 1. 하나그린세상 서버에 저장
            saveUnifiedTokenToGreenWorld(phoneNumber, unifiedToken);
            
            // 2. 하나카드 서버에 저장
            saveUnifiedTokenToCardServer(phoneNumber, unifiedToken);
            
            log.info("통합 토큰 저장 완료 - 전화번호: {}, 토큰: {}", phoneNumber, unifiedToken);
            
        } catch (Exception e) {
            log.error("통합 토큰 저장 실패", e);
        }
    }
    
    /**
     * 하나그린세상 서버에 통합 토큰 저장
     */
    private void saveUnifiedTokenToGreenWorld(String phoneNumber, String unifiedToken) {
        try {
            // 기존 매핑 정보 업데이트
            Optional<GroupCustomerMapping> mapping = groupCustomerMappingRepository.findByPhoneNumber(phoneNumber);
            if (mapping.isPresent()) {
                GroupCustomerMapping existingMapping = mapping.get();
                existingMapping.setGroupCustomerToken(unifiedToken);
                existingMapping.setLastSyncAt(LocalDateTime.now());
                groupCustomerMappingRepository.save(existingMapping);
                log.info("하나그린세상 통합 토큰 업데이트 완료");
            }
        } catch (Exception e) {
            log.error("하나그린세상 통합 토큰 저장 실패", e);
        }
    }
    
    /**
     * 하나카드 서버에 통합 토큰 저장 (HTTP API 호출)
     */
    private void saveUnifiedTokenToCardServer(String phoneNumber, String unifiedToken) {
        try {
            // 하나카드 서버 API 호출
            String url = hanacardApiBaseUrl + "/api/integration/update-unified-token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 하나카드 서버에서 기대하는 형식으로 헤더 설정
            String internalServiceToken = Base64.getEncoder().encodeToString(secret.getBytes());
            headers.set("X-Internal-Service", internalServiceToken);
            
            Map<String, String> requestBody = Map.of(
                "phoneNumber", phoneNumber,
                "unifiedToken", unifiedToken
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            // RestTemplate으로 API 호출
            restTemplate.postForObject(url, entity, Map.class);
            log.info("하나카드 서버 통합 토큰 저장 완료");
            
        } catch (Exception e) {
            log.error("하나카드 서버 통합 토큰 저장 실패", e);
        }
    }
    
    /**
     * 고객 정보 토큰 생성 (고객 식별용) - 기존 메서드 (하위 호환성)
     */
    public String generateCustomerInfoToken(String groupCustomerToken) {
        try {
            log.info("고객 정보 토큰 생성 시작 - groupCustomerToken: {}", groupCustomerToken);
            
            if (groupCustomerToken == null || groupCustomerToken.trim().isEmpty()) {
                log.warn("groupCustomerToken이 null이거나 비어있음, 기본값 사용");
                return Base64.getEncoder().encodeToString(defaultToken.getBytes());
            }
            
            // groupCustomerToken을 직접 Base64로 인코딩
            String encodedToken = Base64.getEncoder().encodeToString(groupCustomerToken.getBytes());
            log.info("고객 정보 토큰 생성 완료 - 원본: {}, 인코딩: {}", groupCustomerToken, encodedToken);
            
            return encodedToken;
            
        } catch (Exception e) {
            log.error("고객 정보 토큰 생성 실패", e);
            // 기본값 반환
            return Base64.getEncoder().encodeToString(defaultToken.getBytes());
        }
    }

    /**
     * 서명 생성
     */
    private String generateSignature(String ci, long timestamp) {
        try {
            String data = ci + "|" + timestamp + "|" + encryptionKey;
            return String.valueOf(data.hashCode());
        } catch (Exception e) {
            return "default_signature";
        }
    }

    /**
     * 내부 인증 토큰 검증
     */
    public boolean validateInternalAuthToken(String token) {
        try {
            String payload = new String(Base64.getDecoder().decode(token));
            String[] parts = payload.split(":");
            
            if (parts.length != 2) return false;
            
            String groupCustomerToken = parts[0];
            long timestamp = Long.parseLong(parts[1]);
            
            // 토큰 유효 시간 체크 (1시간)
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > 3600000) return false;
            
            // 고객 토큰 존재 여부 확인
            return groupCustomerMappingRepository.findByGroupCustomerToken(groupCustomerToken).isPresent();
            
        } catch (Exception e) {
            log.error("토큰 검증 실패", e);
            return false;
        }
    }

    /**
     * 내부 토큰에서 그룹 고객 토큰 추출
     */
    public String extractGroupCustomerToken(String internalToken) {
        try {
            String payload = new String(Base64.getDecoder().decode(internalToken));
            return payload.split(":")[0];
        } catch (Exception e) {
            log.error("토큰에서 그룹 고객 토큰 추출 실패", e);
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
    }
}
