package com.kopo.hanagreenworld.common.util;

import com.kopo.hanagreenworld.member.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityUtil {

    public static Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        log.info("🔐 SecurityUtil.getCurrentMember() 호출");
        log.info("🔐 Authentication: {}", authentication);
        log.info("🔐 Authentication isAuthenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
        
        if (authentication == null) {
            log.error("❌ Authentication이 null입니다!");
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }
        
        if (!authentication.isAuthenticated()) {
            log.error("❌ Authentication이 인증되지 않았습니다!");
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();
        log.info("🔐 Principal: {}", principal);
        log.info("🔐 Principal type: {}", principal != null ? principal.getClass().getName() : "null");
        
        if (principal instanceof Member) {
            Member member = (Member) principal;
            log.info("✅ 인증된 사용자: ID = {}, 이름 = {}", member.getMemberId(), member.getName());
            return member;
        } else {
            log.error("❌ Principal이 Member 타입이 아닙니다! 타입: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }
    }

    public static Long getCurrentUserId() {
        return getCurrentMemberId();
    }

    public static Long getCurrentMemberId() {
        log.info("🔐 SecurityUtil.getCurrentMemberId() 호출");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔐 Authentication: {}", authentication);
        log.info("🔐 Authentication isAuthenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("❌ Authentication이 null이거나 인증되지 않았습니다!");
            return null; // null 반환으로 변경하여 500 에러 방지
        }

        Object principal = authentication.getPrincipal();
        log.info("🔐 Principal: {}", principal);
        log.info("🔐 Principal type: {}", principal != null ? principal.getClass().getName() : "null");
        
        // JWT 인증의 경우 principal이 String (memberId)일 수 있음
        if (principal instanceof String) {
            try {
                Long memberId = Long.parseLong((String) principal);
                log.info("✅ String Principal에서 memberId 추출: {}", memberId);
                return memberId;
            } catch (NumberFormatException e) {
                log.error("❌ Principal이 유효한 memberId가 아닙니다: {}", principal);
                return null; // null 반환으로 변경하여 500 에러 방지
            }
        }
        
        // Member 객체인 경우
        if (principal instanceof Member) {
            try {
                Long memberId = ((Member) principal).getMemberId();
                log.info("✅ Member Principal에서 memberId 추출: {}", memberId);
                return memberId;
            } catch (Exception e) {
                log.error("❌ Member 객체에서 memberId 추출 실패: {}", e.getMessage());
                return null; // null 반환으로 변경하여 500 에러 방지
            }
        }
        
        log.error("❌ Principal 타입을 인식할 수 없습니다: {}", principal != null ? principal.getClass().getName() : "null");
        return null; // null 반환으로 변경하여 500 에러 방지
    }

    public static String getCurrentMemberEmail() {
        return getCurrentMember().getEmail();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}