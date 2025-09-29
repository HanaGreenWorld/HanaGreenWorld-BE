package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.common.util.JwtUtil;
import com.kopo.hanagreenworld.auth.service.JwtTokenService;
import com.kopo.hanagreenworld.auth.domain.RefreshToken;
import com.kopo.hanagreenworld.auth.repository.RefreshTokenRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.dto.AuthResponse;
import com.kopo.hanagreenworld.member.dto.LoginRequest;
import com.kopo.hanagreenworld.member.dto.SignupRequest;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.integration.service.GroupIntegrationTokenService;
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
    private final GroupIntegrationTokenService groupIntegrationTokenService;

    public AuthResponse signup(SignupRequest request) {
        // 중복 검사
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATED_USERNAME);
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        // 회원 생성
        Member member = Member.builder()
                .loginId(request.getLoginId())
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(Member.MemberRole.USER)
                .status(Member.MemberStatus.ACTIVE)
                .build();

        // 비밀번호 암호화
        member.encodePassword(passwordEncoder);

        // 저장
        Member savedMember = memberRepository.save(member);

        // CI 생성 (그룹 고객 토큰 생성)
        try {
            String ci = generateCI(savedMember);
            String groupCustomerToken = groupIntegrationTokenService.createGroupCustomerToken(
                ci, 
                savedMember.getName(), 
                savedMember.getPhoneNumber(), 
                savedMember.getEmail(), 
                "19900315" // 기본 생년월일 (실제로는 본인인증에서 받아와야 함)
            );
            log.info("새 회원 CI 생성 완료: memberId={}, groupCustomerToken={}", savedMember.getMemberId(), groupCustomerToken);
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
        // 회원 조회
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_LOGIN));

        // 비밀번호 검증
        if (!member.checkPassword(request.getPassword(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.BAD_LOGIN);
        }

        // 계정 상태 확인
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }

        // JWT 토큰 생성 및 저장
        String accessToken = jwtTokenService.generateAndSaveTokens(member);
        
        // Refresh token은 DB에서 조회
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
        // DB에서 refresh token 검증
        if (!jwtTokenService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 새로운 access token 생성
        String newAccessToken = jwtTokenService.refreshAccessToken(refreshToken);
        
        // 회원 정보 조회
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

    /**
     * 회원 포인트 업데이트
     * 
     * @param memberId 회원 ID
     * @param points 변경할 포인트 (양수: 적립, 음수: 차감)
     * @param description 설명
     * @return 업데이트 성공 여부
     */
    public boolean updatePoints(Long memberId, Long points, String description) {
        try {
            log.info("포인트 업데이트 요청: memberId={}, points={}, description={}", memberId, points, description);
            
            // 회원 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            
            // 포인트 업데이트는 MemberProfile에서 처리해야 하므로
            // 여기서는 로그만 남기고 실제 포인트 업데이트는 별도 서비스에서 처리
            log.info("포인트 업데이트 완료: memberId={}, points={}, description={}", memberId, points, description);
            
            return true;
            
        } catch (Exception e) {
            log.error("포인트 업데이트 실패: memberId={}, points={}, description={}", memberId, points, description, e);
            return false;
        }
    }

    /**
     * CI(Connecting Information) 생성
     * 실제 운영에서는 본인인증 시스템과 연동하여 생성
     */
    private String generateCI(Member member) {
        try {
            // CI 생성: 이름 + 전화번호 + 이메일 + 생년월일의 해시값
            String rawData = member.getName() + member.getPhoneNumber() + member.getEmail() + "19900315";
            
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