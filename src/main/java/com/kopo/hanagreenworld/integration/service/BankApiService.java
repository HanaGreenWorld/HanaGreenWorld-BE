package com.kopo.hanagreenworld.integration.service;

import com.kopo.hanagreenworld.deposit.dto.DemandDepositAccountResponse;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BankApiService {

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final GroupIntegrationTokenService tokenService;

    @Value("${integration.bank.url}")
    private String bankServiceUrl;

    /**
     * 하나은행에서 입출금 계좌 목록 조회
     */
    public List<DemandDepositAccountResponse> getDepositAccounts(String phoneNumber) {
        try {
            log.info("하나은행 입출금 계좌 목록 조회 시작 - 전화번호: {}", phoneNumber);

            // 기존 토큰 조회
            String groupCustomerToken = tokenService.getGroupTokenByPhone(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("그룹 고객 토큰을 찾을 수 없습니다. 전화번호: " + phoneNumber));
            String internalServiceToken = tokenService.generateInternalServiceToken();

            String url = bankServiceUrl + "/api/integration/deposit-accounts";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);

            Map<String, String> request = Map.of(
                    "groupCustomerToken", groupCustomerToken,
                    "requestingService", "GREEN_WORLD"
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("하나은행 입출금 계좌 목록 조회 성공");

                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> accountsData = (List<Map<String, Object>>) responseBody.get("data");

                List<DemandDepositAccountResponse> accounts = new ArrayList<>();
                if (accountsData != null) {
                    for (Map<String, Object> accountData : accountsData) {
                        accounts.add(DemandDepositAccountResponse.builder()
                                .accountNumber((String) accountData.get("accountNumber"))
                                .accountName((String) accountData.get("accountName"))
                                .balance(((Number) accountData.get("balance")).longValue())
                                .accountType((String) accountData.get("accountType"))
                                .isActive((Boolean) accountData.get("isActive"))
                                .build());
                    }
                }

                return accounts;
            } else {
                throw new RuntimeException("하나은행 입출금 계좌 목록 조회에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("하나은행 입출금 계좌 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("입출금 계좌 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 회원ID로 입출금 계좌 목록 조회
     */
    public List<DemandDepositAccountResponse> getDepositAccountsByMemberId(Long memberId) {
        try {
            log.info("회원 입출금 계좌 목록 조회 시작 - 회원ID: {}", memberId);

            // 회원 정보 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // 전화번호로 계좌 목록 조회
            return getDepositAccounts(member.getPhoneNumber());

        } catch (Exception e) {
            log.error("회원 입출금 계좌 목록 조회 실패 - 회원ID: {}", memberId, e);
            throw new RuntimeException("입출금 계좌 목록 조회에 실패했습니다.", e);
        }
    }
}
