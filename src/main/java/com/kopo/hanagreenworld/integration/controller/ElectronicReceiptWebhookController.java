package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import com.kopo.hanagreenworld.point.service.EcoSeedService;
import com.kopo.hanagreenworld.point.dto.EcoSeedEarnRequest;
import com.kopo.hanagreenworld.point.domain.PointCategory;
import com.kopo.hanagreenworld.point.repository.PointTransactionRepository;
import com.kopo.hanagreenworld.point.domain.PointTransaction;
import com.kopo.hanagreenworld.activity.service.ElectronicReceiptRecordService;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;

@RestController
@RequestMapping("/api/integration/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Electronic Receipt Webhook", description = "전자영수증 웹훅 API")
public class ElectronicReceiptWebhookController {

    private final MemberRepository memberRepository;
    private final MemberProfileService memberProfileService;
    private final EcoSeedService ecoSeedService;
    private final PointTransactionRepository pointTransactionRepository;
    private final ElectronicReceiptRecordService electronicReceiptRecordService;

    @PostMapping("/electronic-receipt")
    @Operation(summary = "전자영수증 웹훅", description = "하나은행에서 전자영수증 발급 시 자동으로 포인트를 적립합니다.")
    public ResponseEntity<ApiResponse<String>> handleElectronicReceiptWebhook(@RequestBody Map<String, Object> request) {
        try {
            // 웹훅 데이터 파싱
            String ciToken = (String) request.get("ciToken");
            String transactionId = (String) request.get("transactionId");
            String transactionType = (String) request.get("transactionType");
            Long transactionAmount = Long.valueOf(request.get("transactionAmount").toString());
            String branchName = (String) request.get("branchName");
            
            // transactionDate가 ArrayList로 전달되는 경우 처리
            LocalDateTime transactionDate;
            Object transactionDateObj = request.get("transactionDate");
            if (transactionDateObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> dateList = (List<Integer>) transactionDateObj;
                if (dateList.size() >= 6) {
                    transactionDate = LocalDateTime.of(
                        dateList.get(0), // 년
                        dateList.get(1), // 월
                        dateList.get(2), // 일
                        dateList.get(3), // 시
                        dateList.get(4), // 분
                        dateList.get(5)  // 초
                    );
                } else {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("transactionDate 형식이 올바르지 않습니다."));
                }
            } else {
                String transactionDateStr = (String) transactionDateObj;
                transactionDate = LocalDateTime.parse(transactionDateStr);
            }

            if (ciToken == null || transactionId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("필수 데이터가 누락되었습니다."));
            }

            // CI 토큰에서 CI 추출
            String ci = extractCiFromToken(ciToken);

            Optional<Member> memberOpt = memberRepository.findByCi(ci);
            
            if (memberOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("하나그린세상 회원을 찾을 수 없습니다."));
            }

            Member member = memberOpt.get();
            Long memberId = member.getMemberId();

            Optional<PointTransaction> existingTransaction = pointTransactionRepository
                .findByMember_MemberIdAndDescriptionContaining(memberId, "거래ID: " + transactionId);
            
            if (existingTransaction.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(
                    "이미 처리된 전자영수증입니다.",
                    "transactionId: " + transactionId
                ));
            }

            // 포인트 적립
            EcoSeedEarnRequest earnRequest = EcoSeedEarnRequest.builder()
                .category(PointCategory.ELECTRONIC_RECEIPT)
                .pointsAmount(3)
                .description(String.format("전자영수증 발급", 
                    transactionId, transactionType, transactionAmount))
                .build();

            ecoSeedService.earnEcoSeedsForWebhook(memberId, earnRequest);

            memberProfileService.updateMemberActivityWithCarbon(memberId, 0.0005);

            // 전자확인증 기록 저장
            electronicReceiptRecordService.createElectronicReceiptRecord(
                memberId, transactionId, transactionType, transactionAmount, branchName, transactionDate
            );

            return ResponseEntity.ok(ApiResponse.success(
                "전자영수증 포인트 적립이 완료되었습니다.",
                "회원ID: " + memberId + ", 거래ID: " + transactionId + ", 포인트: 3P"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("전자영수증 웹훅 처리에 실패했습니다: " + e.getMessage()));
        }
    }

    private String extractCiFromToken(String ciToken) {
        try {
            // Base64 디코딩 시도
            String decodedToken;
            try {
                decodedToken = new String(Base64.getDecoder().decode(ciToken));
                return decodedToken;
            } catch (Exception e) {
                // Base64 디코딩 실패 시 원본 토큰 사용
                return ciToken;
            }
            
        } catch (Exception e) {
            log.error("CI 토큰에서 CI 추출 실패", e);
            return ciToken;
        }
    }
}