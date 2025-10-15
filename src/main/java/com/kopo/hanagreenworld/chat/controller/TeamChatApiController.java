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

    @GetMapping("/{teamId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getTeamMessages(@PathVariable Long teamId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            if (memberId == null) {
                return ResponseEntity.status(401).body(null);
            }

            Member currentMember = memberRepository.findById(memberId)
                    .orElse(null);
            
            if (currentMember == null) {
                return ResponseEntity.status(401).body(null);
            }

            List<ChatMessageResponse> messages = teamChatService.getTeamMessages(teamId, currentMember);

            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
