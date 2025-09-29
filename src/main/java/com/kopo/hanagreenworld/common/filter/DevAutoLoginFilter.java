package com.kopo.hanagreenworld.common.filter;

import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
@Profile("dev") // 개발 환경에서만 활성화
public class DevAutoLoginFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 인증이 설정되지 않았으면 자동으로 테스트 사용자로 로그인
        // JWT가 실패했거나 Authorization 헤더가 없는 경우에 작동
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("인증 정보가 없음 - 자동 로그인 시도");
            Member testMember = memberRepository.findByLoginId("testuser").orElse(null);
            
            if (testMember != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        testMember,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + testMember.getRole().name()))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("개발 환경 자동 로그인 성공: {} (ID: {})", testMember.getLoginId(), testMember.getMemberId());
            } else {
                log.error("테스트 사용자(testuser)를 찾을 수 없습니다!");
            }
        } else {
            log.info("이미 인증된 사용자: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        }

        filterChain.doFilter(request, response);
    }
}
