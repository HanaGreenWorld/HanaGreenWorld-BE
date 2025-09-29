package com.kopo.hanagreenworld.member.controller;

import com.kopo.hanagreenworld.common.response.ApiResponse;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.service.MemberService;
import com.kopo.hanagreenworld.integration.service.HanamoneyCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Integration", description = "하나카드 연동을 위한 회원 관리 API")
public class MemberIntegrationController {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberService memberService;
    private final HanamoneyCardService hanamoneyCardService;

    @Operation(summary = "전화번호로 회원 조회", description = "하나카드에서 전화번호로 하나그린세상 회원을 조회합니다.")
    @PostMapping("/find-by-phone")
    public ResponseEntity<Map<String, Object>> findMemberByPhone(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            log.info("전화번호로 회원 조회 요청: {}", phoneNumber);

            Optional<Member> memberOpt = memberRepository.findByPhoneNumber(phoneNumber);
            
            if (memberOpt.isEmpty()) {
                log.warn("전화번호로 회원을 찾을 수 없습니다: {}", phoneNumber);
                return ResponseEntity.notFound().build();
            }

            Member member = memberOpt.get();
            Optional<MemberProfile> profileOpt = memberProfileRepository.findByMember_MemberId(member.getMemberId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("memberId", member.getMemberId());
            response.put("name", member.getName());
            response.put("email", member.getEmail());
            response.put("phoneNumber", member.getPhoneNumber());
            
            if (profileOpt.isPresent()) {
                MemberProfile profile = profileOpt.get();
                response.put("currentPoints", profile.getCurrentPoints());
                response.put("ecoLevel", profile.getEcoLevel());
                response.put("nickname", profile.getNickname());
            } else {
                response.put("currentPoints", 0L);
                response.put("ecoLevel", "BEGINNER");
                response.put("nickname", member.getName());
            }

            log.info("회원 조회 성공: memberId={}", member.getMemberId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("전화번호로 회원 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "하나머니 잔액 업데이트", description = "하나카드에서 하나머니 거래 시 하나그린세상의 잔액을 동기화합니다.")
    @PostMapping("/update-hana-money")
    public ResponseEntity<ApiResponse<String>> updateHanaMoney(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = (String) request.get("phoneNumber");
            Long amount = Long.valueOf(request.get("amount").toString());
            String transactionType = (String) request.get("transactionType");
            String description = (String) request.get("description");

            log.info("하나머니 업데이트 요청: phoneNumber={}, amount={}, type={}, description={}", 
                    phoneNumber, amount, transactionType, description);

            Optional<Member> memberOpt = memberRepository.findByPhoneNumber(phoneNumber);
            
            if (memberOpt.isEmpty()) {
                log.warn("전화번호로 회원을 찾을 수 없습니다: {}", phoneNumber);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("회원을 찾을 수 없습니다."));
            }

            Member member = memberOpt.get();
            
            // 로컬에서 포인트 업데이트 처리 (순환 호출 방지)
            boolean success = false;
            if ("EARN".equals(transactionType.toUpperCase())) {
                success = memberService.updatePoints(member.getMemberId(), amount, "하나머니 적립: " + description);
            } else if ("SPEND".equals(transactionType.toUpperCase())) {
                // SPEND의 경우 음수로 전달하여 차감 처리
                success = memberService.updatePoints(member.getMemberId(), -amount, "하나머니 사용: " + description);
            } else {
                log.warn("알 수 없는 거래 타입: {}", transactionType);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("알 수 없는 거래 타입입니다."));
            }

            if (!success) {
                log.error("포인트 업데이트 실패: memberId={}, amount={}, type={}", 
                        member.getMemberId(), amount, transactionType);
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("하나머니 업데이트에 실패했습니다."));
            }

            log.info("하나머니 업데이트 완료: memberId={}, amount={}, type={}", 
                    member.getMemberId(), amount, transactionType);

            return ResponseEntity.ok(ApiResponse.success("하나머니가 성공적으로 업데이트되었습니다.", null));
            
        } catch (Exception e) {
            log.error("하나머니 업데이트 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("하나머니 업데이트에 실패했습니다."));
        }
    }

    @Operation(summary = "회원 ID로 하나머니 조회", description = "회원 ID로 하나머니 잔액을 조회합니다.")
    @GetMapping("/{memberId}/hana-money")
    public ResponseEntity<Map<String, Object>> getHanaMoneyByMemberId(@PathVariable Long memberId) {
        try {
            log.info("회원 ID로 하나머니 조회 요청: {}", memberId);

            Optional<Member> memberOpt = memberRepository.findById(memberId);
            if (memberOpt.isEmpty()) {
                log.warn("회원을 찾을 수 없습니다: memberId={}", memberId);
                return ResponseEntity.notFound().build();
            }

            Optional<MemberProfile> profileOpt = memberProfileRepository.findByMember_MemberId(memberId);
            if (profileOpt.isEmpty()) {
                log.warn("회원 프로필을 찾을 수 없습니다: memberId={}", memberId);
                return ResponseEntity.notFound().build();
            }

            MemberProfile profile = profileOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("memberId", memberId);
            response.put("currentPoints", profile.getCurrentPoints());
            response.put("ecoLevel", profile.getEcoLevel());
            response.put("nickname", profile.getNickname());
            
            // 하나머니 정보는 하나카드 서버에서 조회해야 하므로 별도 API 호출 필요
            // 현재는 기본값으로 설정
            response.put("hanaMoney", 0L);
            response.put("message", "하나머니 정보는 하나카드 서버에서 조회하세요");

            log.info("회원 정보 조회 성공: memberId={}, currentPoints={}", memberId, profile.getCurrentPoints());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("회원 정보 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
