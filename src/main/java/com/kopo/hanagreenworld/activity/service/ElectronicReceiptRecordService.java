package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.ElectronicReceiptRecord;
import com.kopo.hanagreenworld.activity.dto.ElectronicReceiptRecordResponse;
import com.kopo.hanagreenworld.activity.repository.ElectronicReceiptRecordRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ElectronicReceiptRecordService {

    private final ElectronicReceiptRecordRepository electronicReceiptRecordRepository;
    private final GroupIntegrationService groupIntegrationService;
    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate;
    
    @Value("${integration.bank.url}")
    private String bankServiceUrl;

    public Page<ElectronicReceiptRecordResponse> getElectronicReceiptRecords(Long memberId, Pageable pageable) {
        Page<ElectronicReceiptRecord> records = electronicReceiptRecordRepository
            .findByMember_MemberIdOrderByReceiptDateDesc(memberId, pageable);
        
        return records.map(ElectronicReceiptRecordResponse::from);
    }

    public List<ElectronicReceiptRecordResponse> getElectronicReceiptRecords(Long memberId) {

        try {
            // 하나은행에서 전자확인증 데이터 가져오기
            List<ElectronicReceiptRecordResponse> bankData = getElectronicReceiptsFromBank(memberId);
            
            if (bankData != null && !bankData.isEmpty()) {
                log.info("하나은행에서 전자확인증 데이터 조회 성공: memberId={}, count={}", memberId, bankData.size());
                return bankData;
            }

            List<ElectronicReceiptRecord> records = electronicReceiptRecordRepository
                .findByMember_MemberIdOrderByReceiptDateDesc(memberId);
            
            List<ElectronicReceiptRecordResponse> responses = records.stream()
                .map(ElectronicReceiptRecordResponse::from)
                .toList();

            return responses;
            
        } catch (Exception e) {
            List<ElectronicReceiptRecord> records = electronicReceiptRecordRepository
                .findByMember_MemberIdOrderByReceiptDateDesc(memberId);
            
            return records.stream()
                .map(ElectronicReceiptRecordResponse::from)
                .toList();
        }
    }

    public List<ElectronicReceiptRecordResponse> getElectronicReceiptRecordsByDateRange(
            Long memberId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ElectronicReceiptRecord> records = electronicReceiptRecordRepository
            .findByMemberAndDateRange(memberId, startDate, endDate);
        
        return records.stream()
            .map(ElectronicReceiptRecordResponse::from)
            .toList();
    }

    public ElectronicReceiptStats getElectronicReceiptStats(Long memberId) {
        long totalCount = electronicReceiptRecordRepository.countByMember_MemberId(memberId);

        Integer totalPoints = electronicReceiptRecordRepository.sumPointsByMemberId(memberId);

        ElectronicReceiptStats stats = ElectronicReceiptStats.builder()
            .totalCount(totalCount)
            .totalPoints(totalPoints != null ? totalPoints : 0)
            .build();

        return stats;
    }

    @Transactional
    public ElectronicReceiptRecord createElectronicReceiptRecord(Long memberId, String transactionId,
                                                                String transactionType, Long transactionAmount,
                                                                String branchName, LocalDateTime receiptDate) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));

        ElectronicReceiptRecord record = ElectronicReceiptRecord.builder()
            .member(member)
            .transactionId(transactionId)
            .transactionType(transactionType)
            .transactionAmount(transactionAmount)
            .branchName(branchName)
            .receiptDate(receiptDate)
            .build();

        ElectronicReceiptRecord savedRecord = electronicReceiptRecordRepository.save(record);

        return savedRecord;
    }


    @Transactional(readOnly = true)
    public List<ElectronicReceiptRecordResponse> getElectronicReceiptsFromBank(Long memberId) {
        try {
            // 회원 정보 조회
            Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));
            
            // CI 추출 및 토큰 생성
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                // CI가 없으면 목데이터용 CI 생성
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
            }
            
            // CI를 Base64 인코딩하여 customerInfoToken 생성
            String customerInfoToken = Base64.getEncoder().encodeToString(ci.getBytes());
            String internalServiceToken = groupIntegrationService.generateInternalServiceToken();
            
            // 하나은행 전자영수증 전용 API 호출
            String url = bankServiceUrl + "/api/integration/electronic-receipts";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", internalServiceToken);
            
            // 요청 바디 생성 (CI 기반 방식)
            Map<String, String> requestBody = Map.of(
                "customerInfoToken", customerInfoToken,
                "requestingService", "GREEN_WORLD",
                "consentToken", "CONSENT_" + memberId
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("하나은행 전자확인증 조회 요청: URL={}, 회원ID={}", url, memberId);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                
                // 응답에서 전자확인증 데이터 추출
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> bankData = (List<Map<String, Object>>) responseBody.get("data");
                    log.info("하나은행 전자확인증 조회 성공: count={}", bankData.size());
                    
                    // 하나은행 데이터를 ElectronicReceiptRecordResponse로 변환
                    return bankData.stream()
                        .map(this::convertBankDataToResponse)
                        .toList();
                }
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("하나은행 전자확인증 조회 실패: memberId={}, error={}", memberId, e.getMessage(), e);
            throw new RuntimeException("하나은행 전자확인증 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private ElectronicReceiptRecordResponse convertBankDataToResponse(Map<String, Object> bankData) {
        return ElectronicReceiptRecordResponse.builder()
            .recordId(((Number) bankData.get("receiptId")).longValue())
            .transactionId((String) bankData.get("transactionId"))
            .transactionType((String) bankData.get("transactionType"))
            .transactionAmount(((Number) bankData.get("transactionAmount")).longValue())
            .branchName((String) bankData.get("branchName"))
            .receiptDate(LocalDateTime.parse(((String) bankData.get("receiptDate")).replace("T", "T")))
            .pointsEarned(3) // 전자확인증당 3포인트
            .createdAt(LocalDateTime.parse(((String) bankData.get("createdAt")).replace("T", "T")))
            .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class ElectronicReceiptStats {
        private final long totalCount;
        private final int totalPoints;
    }
}
