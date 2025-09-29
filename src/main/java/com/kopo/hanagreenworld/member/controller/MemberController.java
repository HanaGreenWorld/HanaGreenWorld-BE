package com.kopo.hanagreenworld.member.controller;

import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.dto.AuthResponse;
import com.kopo.hanagreenworld.member.dto.LoginRequest;
import com.kopo.hanagreenworld.member.dto.SignupRequest;
import com.kopo.hanagreenworld.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 갱신 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: {}", request.getLoginId());
        AuthResponse response = memberService.signup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "회원 로그인을 처리합니다.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("=== 로그인 요청 시작 ===");
        log.info("요청 URL: /auth/login");
        log.info("요청 메서드: POST");
        log.info("요청 헤더: Content-Type: application/json");
        log.info("요청 바디: loginId={}, password={}", request.getLoginId(), request.getPassword() != null ? "***" : "null");
        log.info("요청 시간: {}", java.time.LocalDateTime.now());
        
        try {
            AuthResponse response = memberService.login(request);
            log.info("=== 로그인 성공 ===");
            log.info("응답 상태: 200 OK");
            log.info("응답 바디: {}", response);
            log.info("응답 시간: {}", java.time.LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== 로그인 실패 ===");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 스택: ", e);
            log.error("실패 시간: {}", java.time.LocalDateTime.now());
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        String refreshToken = authorization.replace("Bearer ", "");
        log.info("토큰 갱신 요청");
        AuthResponse response = memberService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자를 로그아웃 처리합니다.")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorization) {
        String refreshToken = authorization.replace("Bearer ", "");
        log.info("로그아웃 요청");
        memberService.logout(refreshToken);
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "모든 기기 로그아웃", description = "현재 사용자의 모든 기기에서 로그아웃 처리합니다.")
    public ResponseEntity<String> logoutAll() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        log.info("모든 기기 로그아웃 요청: memberId={}", memberId);
        memberService.logoutAll(memberId);
        return ResponseEntity.ok("모든 기기에서 로그아웃이 완료되었습니다.");
    }

    @GetMapping("/test")
    @Operation(summary = "인증 테스트", description = "토큰이 유효한지 테스트합니다.")
    public ResponseEntity<String> testAuth() {
        return ResponseEntity.ok("인증이 성공했습니다!");
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보", description = "현재 로그인된 사용자의 정보를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            log.info("=== 현재 사용자 정보 조회 요청 시작 ===");
            log.info("SecurityContext: {}", SecurityContextHolder.getContext());
            log.info("Authentication: {}", SecurityContextHolder.getContext().getAuthentication());
            
            Member member = SecurityUtil.getCurrentMember();
            log.info("조회된 Member: {}", member);
            
            if (member == null) {
                log.error("❌ 현재 인증된 사용자를 찾을 수 없습니다.");
                Map<String, Object> error = new HashMap<>();
                error.put("error", "인증된 사용자를 찾을 수 없습니다.");
                return ResponseEntity.status(401).body(error);
            }
            
            log.info("사용자 정보 조회 성공: {}", member.getMemberId());
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", member.getMemberId());
            userInfo.put("loginId", member.getLoginId());
            userInfo.put("email", member.getEmail());
            userInfo.put("name", member.getName());
            userInfo.put("role", member.getRole().name());
            userInfo.put("status", member.getStatus().name());
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "사용자 정보를 조회하는 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
