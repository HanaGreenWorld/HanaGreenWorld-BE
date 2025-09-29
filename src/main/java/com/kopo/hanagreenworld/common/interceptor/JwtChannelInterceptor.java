package com.kopo.hanagreenworld.common.interceptor;

import com.kopo.hanagreenworld.common.util.JwtUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * WebSocket STOMP 메시지에서 JWT 토큰을 처리하는 인터셉터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            log.info("🔍 STOMP 메시지 처리: Command = {}, Destination = {}, SessionId = {}", 
                accessor.getCommand(), accessor.getDestination(), accessor.getSessionId());
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.info("🔗 CONNECT 명령 처리 시작");
                // CONNECT 시 JWT 토큰으로 인증하고 세션에 저장
                handleConnect(accessor);
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                log.info("📤 SEND 명령 처리 시작");
                // SEND 시 세션에서 인증 정보 복원
                handleSend(accessor);
            }
        } else {
            log.warn("⚠️ StompHeaderAccessor가 null입니다!");
        }
        
        return message;
    }

    /**
     * CONNECT 명령 처리: JWT 토큰으로 인증하고 세션에 저장
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = getTokenFromHeaders(accessor);
        
        if (StringUtils.hasText(token)) {
            log.debug("WebSocket CONNECT - JWT 토큰 발견: {}", token.substring(0, Math.min(20, token.length())) + "...");
            
            if (jwtUtil.validateToken(token)) {
                try {
                    Long memberId = jwtUtil.getMemberIdFromToken(token);
                    Member member = memberRepository.findById(memberId).orElse(null);
                    
                    if (member != null && member.getStatus().name().equals("ACTIVE")) {
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                member,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
                            );
                        
                        // 세션에 인증 정보 저장
                        accessor.getSessionAttributes().put("SPRING_SECURITY_CONTEXT", SecurityContextHolder.createEmptyContext());
                        accessor.getSessionAttributes().put("USER_AUTHENTICATION", authentication);
                        accessor.getSessionAttributes().put("MEMBER_ID", memberId);
                        accessor.getSessionAttributes().put("MEMBER", member);
                        accessor.getSessionAttributes().put("token", token); // 토큰도 세션에 저장
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                        
                        log.info("WebSocket CONNECT 인증 성공: 사용자 ID = {}, 이름 = {}, 세션 ID = {}", 
                            memberId, member.getName(), accessor.getSessionId());
                    } else {
                        log.warn("WebSocket CONNECT 인증 실패: 유효하지 않은 사용자 (ID: {})", memberId);
                    }
                } catch (Exception e) {
                    log.error("WebSocket CONNECT JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket CONNECT 인증 실패: 유효하지 않은 JWT 토큰");
            }
        } else {
            log.warn("WebSocket CONNECT에 JWT 토큰이 없습니다");
        }
    }

    /**
     * SEND 명령 처리: 세션에서 인증 정보 복원
     */
    private void handleSend(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        
        // 세션에서 인증 정보 가져오기
        Object authObj = accessor.getSessionAttributes().get("USER_AUTHENTICATION");
        Object memberObj = accessor.getSessionAttributes().get("MEMBER");
        Object memberIdObj = accessor.getSessionAttributes().get("MEMBER_ID");
        
        if (authObj instanceof UsernamePasswordAuthenticationToken && memberObj instanceof Member) {
            UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) authObj;
            Member member = (Member) memberObj;
            Long memberId = (Long) memberIdObj;
            
            // SecurityContext에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("WebSocket SEND 인증 복원 성공: 사용자 ID = {}, 이름 = {}, 세션 ID = {}", 
                memberId, member.getName(), sessionId);
        } else {
            log.warn("WebSocket SEND 인증 실패: 세션에 인증 정보가 없습니다. 세션 ID = {}", sessionId);
            
            // 헤더에서 토큰을 다시 시도
            String token = getTokenFromHeaders(accessor);
            if (StringUtils.hasText(token)) {
                log.info("WebSocket SEND - 헤더에서 토큰 재시도: {}", token.substring(0, Math.min(20, token.length())) + "...");
                handleTokenAuthentication(accessor, token);
            }
        }
    }

    /**
     * 토큰으로 직접 인증 처리 (SEND 시 fallback)
     */
    private void handleTokenAuthentication(StompHeaderAccessor accessor, String token) {
        if (jwtUtil.validateToken(token)) {
            try {
                Long memberId = jwtUtil.getMemberIdFromToken(token);
                Member member = memberRepository.findById(memberId).orElse(null);
                
                if (member != null && member.getStatus().name().equals("ACTIVE")) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            member,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("WebSocket SEND 토큰 인증 성공: 사용자 ID = {}, 이름 = {}", 
                        memberId, member.getName());
                } else {
                    log.warn("WebSocket SEND 토큰 인증 실패: 유효하지 않은 사용자 (ID: {})", memberId);
                }
            } catch (Exception e) {
                log.error("WebSocket SEND 토큰 처리 중 오류 발생: {}", e.getMessage());
            }
        } else {
            log.warn("WebSocket SEND 인증 실패: 유효하지 않은 JWT 토큰");
        }
    }

    /**
     * STOMP 헤더에서 JWT 토큰 추출
     */
    private String getTokenFromHeaders(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 토큰 추출
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        
        // authorization 헤더에서도 시도 (소문자)
        authHeaders = accessor.getNativeHeader("authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        
        // 쿼리 파라미터에서 토큰 추출 (SockJS 연결 시)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object tokenObj = sessionAttributes.get("token");
            if (tokenObj instanceof String) {
                String token = (String) tokenObj;
                if (StringUtils.hasText(token)) {
                    return token;
                }
            }
        }
        
        return null;
    }
}
