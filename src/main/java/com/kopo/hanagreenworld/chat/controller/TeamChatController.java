package com.kopo.hanagreenworld.chat.controller;

import com.kopo.hanagreenworld.chat.dto.ChatMessageRequest;
import com.kopo.hanagreenworld.chat.dto.ChatMessageResponse;
import com.kopo.hanagreenworld.chat.dto.PresenceEvent;
import com.kopo.hanagreenworld.chat.service.TeamChatService;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TeamChatController {

    private final TeamChatService teamChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberRepository memberRepository;

    /**
     * 메시지 전송
     */
    @MessageMapping("/chat.send.{teamId}")
    public void sendMessage(@DestinationVariable Long teamId, 
                           @Payload ChatMessageRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("📨 메시지 전송 요청: 팀 ID = {}, 메시지 = {}", teamId, request.getMessageText());
            log.info("📨 세션 ID: {}, 헤더: {}", headerAccessor.getSessionId(), headerAccessor.toMap());
            
            // WebSocket 세션에서 사용자 정보 추출
            Member currentMember = getCurrentMemberFromSession(headerAccessor);
            if (currentMember == null) {
                log.error("❌ 세션에서 사용자 정보를 찾을 수 없습니다. 세션 ID: {}", headerAccessor.getSessionId());
                log.error("❌ 세션 속성: {}", headerAccessor.getSessionAttributes());
                throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
            }
            
            log.info("✅ 메시지 전송자: ID = {}, 이름 = {}", currentMember.getMemberId(), currentMember.getName());
            
            // 메시지 전송 (사용자 정보를 직접 전달)
            ChatMessageResponse response = teamChatService.sendMessage(request, currentMember);
            
            // 팀 채팅방에 브로드캐스트
            String destination = "/topic/team/" + teamId;
            log.info("📤 메시지 브로드캐스트: destination = {}, response = {}", destination, response);
            messagingTemplate.convertAndSend(destination, response);
            
            log.info("✅ 메시지 전송 완료: 팀 ID = {}, 메시지 ID = {}", teamId, response.getMessageId());
            
        } catch (Exception e) {
            log.error("메시지 전송 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
            
            // 에러 메시지를 발신자에게만 전송
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "unknown";
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", 
                "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * WebSocket 세션에서 현재 사용자 정보 추출
     */
    private Member getCurrentMemberFromSession(SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("🔍 세션에서 사용자 정보 추출 시도: 세션 ID = {}", headerAccessor.getSessionId());
            log.info("🔍 세션 속성 키들: {}", headerAccessor.getSessionAttributes().keySet());
            
            // 세션에서 저장된 사용자 정보 가져오기
            Object memberObj = headerAccessor.getSessionAttributes().get("MEMBER");
            log.info("🔍 MEMBER 객체: {}", memberObj != null ? memberObj.getClass().getName() : "null");
            
            if (memberObj instanceof Member) {
                Member member = (Member) memberObj;
                log.info("✅ 세션에서 Member 정보 추출 성공: ID = {}, 이름 = {}", member.getMemberId(), member.getName());
                return member;
            }
            
            // Principal에서 사용자 정보 가져오기
            Principal principal = headerAccessor.getUser();
            log.info("🔍 Principal 객체: {}", principal != null ? principal.getClass().getName() : "null");
            
            if (principal != null && principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // Principal이 UserDetails인 경우 Member로 변환
                // 이 경우는 일반적이지 않으므로 로그만 남김
                log.warn("Principal이 UserDetails 타입입니다: {}", principal.getClass().getName());
            }
            
            log.warn("❌ 세션에서 Member 정보를 찾을 수 없습니다. 세션 ID: {}", headerAccessor.getSessionId());
            return null;
            
        } catch (Exception e) {
            log.error("❌ 세션에서 사용자 정보 추출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 팀 참여
     */
    @MessageMapping("/chat.join.{teamId}")
    public void joinTeam(@DestinationVariable Long teamId, 
                        SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("팀 참여 요청: 팀 ID = {}", teamId);
            
            // WebSocket 세션에서 사용자 정보 추출
            Member currentMember = getCurrentMemberFromSession(headerAccessor);
            if (currentMember == null) {
                log.error("세션에서 사용자 정보를 찾을 수 없습니다. 세션 ID: {}", headerAccessor.getSessionId());
                throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
            }
            
            // 팀 참여 처리 (사용자 정보를 직접 전달)
            teamChatService.joinTeam(teamId, currentMember);
            
            // 참여 이벤트 생성
            PresenceEvent joinEvent = PresenceEvent.join(teamId, currentMember.getMemberId(), currentMember.getName());
            
            // 팀 채팅방에 참여 알림 브로드캐스트
            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/presence", joinEvent);
            
            log.info("팀 참여 완료: 팀 ID = {}, 사용자 = {}", teamId, currentMember.getName());
            
        } catch (Exception e) {
            log.error("팀 참여 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }


    /**
     * 팀 떠나기
     */
    @MessageMapping("/chat.leave.{teamId}")
    public void leaveTeam(@DestinationVariable Long teamId, 
                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("팀 떠나기 요청: 팀 ID = {}", teamId);
            
            // 현재 사용자 정보 가져오기
            Member currentMember = SecurityUtil.getCurrentMember();
            if (currentMember != null) {
                // 팀 떠나기 처리
                teamChatService.leaveTeam(teamId);
                
                // 떠나기 이벤트 생성
                PresenceEvent leaveEvent = PresenceEvent.leave(teamId, currentMember.getMemberId(), currentMember.getName());
                
                // 팀 채팅방에 떠나기 알림 브로드캐스트
                messagingTemplate.convertAndSend("/topic/team/" + teamId + "/presence", leaveEvent);
                
                log.info("팀 떠나기 완료: 팀 ID = {}, 사용자 = {}", teamId, currentMember.getName());
            }
            
        } catch (Exception e) {
            log.error("팀 떠나기 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }

    /**
     * 온라인 사용자 목록 요청
     */
    @MessageMapping("/chat.online.{teamId}")
    public void getOnlineUsers(@DestinationVariable Long teamId) {
        try {
            log.info("온라인 사용자 목록 요청: 팀 ID = {}", teamId);
            
            // 온라인 사용자 목록 조회
            var onlineUsers = teamChatService.getOnlineUsers(teamId);
            
            // 요청자에게 온라인 사용자 목록 전송
            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/online", onlineUsers);
            
            log.info("온라인 사용자 목록 전송 완료: 팀 ID = {}, 사용자 수 = {}", teamId, onlineUsers.size());
            
        } catch (Exception e) {
            log.error("온라인 사용자 목록 조회 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }

    /**
     * 메시지 삭제
     */
    @MessageMapping("/chat.delete.{teamId}")
    public void deleteMessage(@DestinationVariable Long teamId, 
                             @Payload Long messageId,
                             SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("메시지 삭제 요청: 팀 ID = {}, 메시지 ID = {}", teamId, messageId);
            
            // 메시지 삭제
            teamChatService.deleteMessage(messageId);
            
            // 팀 채팅방에 삭제 알림 브로드캐스트
            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/delete", messageId);
            
            log.info("메시지 삭제 완료: 팀 ID = {}, 메시지 ID = {}", teamId, messageId);
            
        } catch (Exception e) {
            log.error("메시지 삭제 실패: 팀 ID = {}, 메시지 ID = {}, 에러 = {}", teamId, messageId, e.getMessage(), e);
            
            // 에러 메시지를 발신자에게만 전송
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "unknown";
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", 
                "메시지 삭제에 실패했습니다: " + e.getMessage());
        }
    }
}
