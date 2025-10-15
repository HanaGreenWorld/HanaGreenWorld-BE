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

    @MessageMapping("/chat.send.{teamId}")
    public void sendMessage(@DestinationVariable Long teamId, 
                           @Payload ChatMessageRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        try {
            // WebSocket 세션에서 사용자 정보 추출
            Member currentMember = getCurrentMemberFromSession(headerAccessor);
            if (currentMember == null) {
                throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
            }

            ChatMessageResponse response = teamChatService.sendMessage(request, currentMember);
            
            // 팀 채팅방에 브로드캐스트
            String destination = "/topic/team/" + teamId;
            messagingTemplate.convertAndSend(destination, response);

        } catch (Exception e) {
            // 에러 메시지를 발신자에게만 전송
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "unknown";
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", 
                "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    private Member getCurrentMemberFromSession(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 세션에서 저장된 사용자 정보 가져오기
            Object memberObj = headerAccessor.getSessionAttributes().get("MEMBER");

            if (memberObj instanceof Member) {
                Member member = (Member) memberObj;
                return member;
            }
            
            // Principal에서 사용자 정보 가져오기
            Principal principal = headerAccessor.getUser();
            
            if (principal != null && principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                log.warn("Principal이 UserDetails 타입입니다: {}", principal.getClass().getName());
            }
            
            log.warn("세션에서 Member 정보를 찾을 수 없습니다. 세션 ID: {}", headerAccessor.getSessionId());
            return null;
            
        } catch (Exception e) {
            log.error("세션에서 사용자 정보 추출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    @MessageMapping("/chat.join.{teamId}")
    public void joinTeam(@DestinationVariable Long teamId, 
                        SimpMessageHeaderAccessor headerAccessor) {
        try {
            Member currentMember = getCurrentMemberFromSession(headerAccessor);
            if (currentMember == null) {
                throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
            }

            teamChatService.joinTeam(teamId, currentMember);

            PresenceEvent joinEvent = PresenceEvent.join(teamId, currentMember.getMemberId(), currentMember.getName());

            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/presence", joinEvent);

        } catch (Exception e) {
            log.error("팀 참여 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.leave.{teamId}")
    public void leaveTeam(@DestinationVariable Long teamId, 
                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 현재 사용자 정보 가져오기
            Member currentMember = SecurityUtil.getCurrentMember();
            if (currentMember != null) {
                // 팀 떠나기 처리
                teamChatService.leaveTeam(teamId);
                
                // 떠나기 이벤트 생성
                PresenceEvent leaveEvent = PresenceEvent.leave(teamId, currentMember.getMemberId(), currentMember.getName());
                
                // 팀 채팅방에 떠나기 알림 브로드캐스트
                messagingTemplate.convertAndSend("/topic/team/" + teamId + "/presence", leaveEvent);
            }
            
        } catch (Exception e) {
            log.error("팀 떠나기 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.online.{teamId}")
    public void getOnlineUsers(@DestinationVariable Long teamId) {
        try {
            var onlineUsers = teamChatService.getOnlineUsers(teamId);

            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/online", onlineUsers);
        } catch (Exception e) {
            log.error("온라인 사용자 목록 조회 실패: 팀 ID = {}, 에러 = {}", teamId, e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.delete.{teamId}")
    public void deleteMessage(@DestinationVariable Long teamId, 
                             @Payload Long messageId,
                             SimpMessageHeaderAccessor headerAccessor) {
        try {
            teamChatService.deleteMessage(messageId);

            messagingTemplate.convertAndSend("/topic/team/" + teamId + "/delete", messageId);
        } catch (Exception e) {
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "unknown";
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", 
                "메시지 삭제에 실패했습니다: " + e.getMessage());
        }
    }
}
