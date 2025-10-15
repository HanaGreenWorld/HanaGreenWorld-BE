package com.kopo.hanagreenworld.integration.controller;

import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/integration/user-verification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Verification", description = "사용자 확인 API")
public class UserVerificationController {

    private final MemberRepository memberRepository;

    @PostMapping("/verify-by-ci")
    @Operation(summary = "CI로 사용자 확인", description = "하나은행에서 CI로 하나그린세상 사용자인지 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyUserByCi(@RequestBody Map<String, String> request) {
        try {
            String ci = request.get("ci");

            if (ci == null || ci.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CI가 필요합니다."));
            }

            // CI로 회원 조회
            Optional<Member> memberOpt = memberRepository.findByCi(ci);
            
            if (memberOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                    "사용자 확인 완료",
                    Map.of(
                        "isGreenWorldUser", false,
                        "ci", ci,
                        "message", "하나그린세상 사용자가 아닙니다."
                    )
                ));
            }

            Member member = memberOpt.get();

            return ResponseEntity.ok(ApiResponse.success(
                "사용자 확인 완료",
                Map.of(
                    "isGreenWorldUser", true,
                    "memberId", member.getMemberId(),
                    "name", member.getName(),
                    "email", member.getEmail(),
                    "phoneNumber", member.getPhoneNumber(),
                    "ci", member.getCi(),
                    "message", "하나그린세상 사용자입니다."
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("사용자 확인에 실패했습니다: " + e.getMessage()));
        }
    }

}

