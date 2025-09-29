package com.kopo.hanagreenworld.chat.controller;

import com.kopo.hanagreenworld.chat.dto.ChatMessageResponse;
import com.kopo.hanagreenworld.chat.service.TeamChatService;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamChatApiController {

    private final TeamChatService teamChatService;
    private final MemberRepository memberRepository;

    /**
     * 팀 채팅 메시지 조회 (REST API)
     */
    @GetMapping("/{teamId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getTeamMessages(@PathVariable Long teamId) {
        try {
            log.info("🚀 팀 메시지 조회 요청 시작: 팀 ID = {}", teamId);
            
            // JWT에서 memberId 추출
            log.info("🔐 SecurityUtil.getCurrentMemberId() 호출 시작");
            Long memberId = SecurityUtil.getCurrentMemberId();
            log.info("🔐 SecurityUtil.getCurrentMemberId() 결과: {}", memberId);
            
            if (memberId == null) {
                log.error("❌ 인증된 사용자 ID를 찾을 수 없습니다.");
                return ResponseEntity.status(401).body(null);
            }
            
            // Member 객체 조회
            log.info("👤 Member 조회 시작: memberId = {}", memberId);
            Member currentMember = memberRepository.findById(memberId)
                    .orElse(null);
            
            if (currentMember == null) {
                log.error("❌ 사용자를 찾을 수 없습니다: {}", memberId);
                return ResponseEntity.status(401).body(null);
            }
            
            log.info("✅ 현재 사용자: ID = {}, 이름 = {}", currentMember.getMemberId(), currentMember.getName());
            
            // 팀 메시지 조회
            log.info("📨 팀 메시지 조회 시작: teamId = {}, memberId = {}", teamId, memberId);
            List<ChatMessageResponse> messages = teamChatService.getTeamMessages(teamId, currentMember);
            
            log.info("✅ 팀 메시지 조회 성공: 팀 ID = {}, 메시지 수 = {}", teamId, messages != null ? messages.size() : 0);
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            log.error("❌ 팀 메시지 조회 실패: 팀 ID = {}, 에러 메시지 = {}", teamId, e.getMessage());
            log.error("❌ 팀 메시지 조회 실패: 스택 트레이스", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
