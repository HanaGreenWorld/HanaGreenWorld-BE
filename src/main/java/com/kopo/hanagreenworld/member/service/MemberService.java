package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.common.util.JwtUtil;
import com.kopo.hanagreenworld.auth.service.JwtTokenService;
import com.kopo.hanagreenworld.auth.domain.RefreshToken;
import com.kopo.hanagreenworld.auth.repository.RefreshTokenRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberStatus;
import com.kopo.hanagreenworld.member.dto.AuthResponse;
import com.kopo.hanagreenworld.member.dto.LoginRequest;
import com.kopo.hanagreenworld.member.dto.SignupRequest;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthResponse signup(SignupRequest request) {
        // 중복 검사
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATED_USERNAME);
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        Member member = Member.builder()
                .loginId(request.getLoginId())
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(Member.MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        // 비밀번호 암호화
        member.encodePassword(passwordEncoder);

        Member savedMember = memberRepository.save(member);

        // CI 생성 및 저장
        try {
            String ci = generateCI(savedMember);
            savedMember.setCi(ci);
            memberRepository.save(savedMember);

            log.info("새 회원 CI 생성 및 저장 완료: memberId={}, CI={}", savedMember.getMemberId(), ci);
        } catch (Exception e) {
            log.error("CI 생성 실패: memberId={}", savedMember.getMemberId(), e);
            // CI 생성 실패해도 회원가입은 진행
        }

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(savedMember.getMemberId(), savedMember.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(savedMember.getMemberId(), savedMember.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(savedMember.getMemberId())
                .email(savedMember.getEmail())
                .name(savedMember.getName())
                .message("회원가입이 완료되었습니다.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_LOGIN));

        // 비밀번호 검증
        if (!member.checkPassword(request.getPassword(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.BAD_LOGIN);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        String accessToken = jwtTokenService.generateAndSaveTokens(member);

        String refreshToken = refreshTokenRepository.findByMemberAndIsActiveTrue(member)
                .map(RefreshToken::getRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token 생성에 실패했습니다."));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .message("로그인이 완료되었습니다.")
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String newAccessToken = jwtTokenService.refreshAccessToken(refreshToken);

        Long memberId = jwtUtil.getMemberIdFromToken(newAccessToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다."));

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // refresh token은 그대로 유지
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .message("토큰이 갱신되었습니다.")
                .build();
    }

    public void logout(String refreshToken) {
        jwtTokenService.logout(refreshToken);
    }

    public void logoutAll(Long memberId) {
        jwtTokenService.logoutAll(memberId);
    }

    private String generateCI(Member member) {
        try {
            // CI 생성: 이름 + 전화번호 + 이메일 해시값
            String rawData = member.getName() + member.getPhoneNumber() + member.getEmail();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            
            // 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("CI 생성 실패", e);
            throw new RuntimeException("CI 생성에 실패했습니다.", e);
        }
    }
}