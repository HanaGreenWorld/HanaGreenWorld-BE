package com.kopo.hanagreenworld.auth.service;

import com.kopo.hanagreenworld.auth.domain.RefreshToken;
import com.kopo.hanagreenworld.auth.repository.RefreshTokenRepository;
import com.kopo.hanagreenworld.common.util.JwtUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String generateAndSaveTokens(Member member) {
        // 새 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(member.getMemberId(), member.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId(), member.getEmail());
        
        // 기존 토큰이 있는지 확인
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByMemberAndIsActiveTrue(member);
        
        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 업데이트
            RefreshToken tokenEntity = existingToken.get();
            tokenEntity.updateToken(refreshToken);
            refreshTokenRepository.save(tokenEntity);
            log.info("기존 JWT 토큰 업데이트 완료: memberId={}", member.getMemberId());
        } else {
            // 기존 토큰이 없으면 새로 생성
            RefreshToken tokenEntity = RefreshToken.builder()
                    .member(member)
                    .refreshToken(refreshToken)
                    .build();
            
            refreshTokenRepository.save(tokenEntity);
            log.info("새로운 JWT 토큰 생성 및 저장 완료: memberId={}", member.getMemberId());
        }
        
        return accessToken;
    }

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        // DB에서 refresh token 검증
        Optional<RefreshToken> tokenEntity = refreshTokenRepository.findByRefreshTokenAndIsActiveTrue(refreshToken);
        
        if (tokenEntity.isEmpty()) {
            throw new RuntimeException("유효하지 않은 refresh token입니다.");
        }
        
        RefreshToken token = tokenEntity.get();
        Member member = token.getMember();
        
        // 새 access token 생성
        String newAccessToken = jwtUtil.generateAccessToken(member.getMemberId(), member.getEmail());
        
        log.info("Access token 갱신 완료: memberId={}", member.getMemberId());
        return newAccessToken;
    }

    @Transactional
    public void logout(String refreshToken) {
        // Refresh token 비활성화
        refreshTokenRepository.deactivateByToken(refreshToken);
        log.info("로그아웃 처리 완료: refreshToken={}", refreshToken.substring(0, Math.min(20, refreshToken.length())) + "...");
    }

    @Transactional
    public void logoutAll(Long memberId) {
        // 해당 사용자의 모든 토큰 비활성화
        refreshTokenRepository.deactivateAllByMember(
            refreshTokenRepository.findById(memberId)
                .map(RefreshToken::getMember)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."))
        );
        log.info("사용자 모든 토큰 비활성화 완료: memberId={}", memberId);
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        return refreshTokenRepository.findByRefreshTokenAndIsActiveTrue(refreshToken).isPresent();
    }
}
