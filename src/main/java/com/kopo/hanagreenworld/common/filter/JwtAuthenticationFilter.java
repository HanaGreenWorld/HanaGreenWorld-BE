package com.kopo.hanagreenworld.common.filter;

import com.kopo.hanagreenworld.common.util.JwtUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                // JWT 토큰 검증
                if (jwtUtil.validateToken(token)) {
                    Long memberId = jwtUtil.getMemberIdFromToken(token);
                    Member member = memberRepository.findById(memberId).orElse(null);

                    if (member != null && member.getStatus() == Member.MemberStatus.ACTIVE) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                member,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT 인증 성공: {}", memberId);
                    }
                } else {
                    // JWT 토큰이 유효하지 않으면 로그만 남기고 다음 필터로 진행
                    // DevAutoLoginFilter에서 자동 로그인을 처리할 수 있도록 함
                    log.debug("JWT 토큰이 유효하지 않음, 다음 필터로 진행: {}", token);
                }
            } catch (Exception e) {
                log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
                // 오류가 발생해도 다음 필터로 진행하여 DevAutoLoginFilter가 처리할 수 있도록 함
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
