package com.kopo.hanagreenworld.chat.service;

import com.kopo.hanagreenworld.chat.domain.TeamChatMessage;
import com.kopo.hanagreenworld.chat.dto.ChatMessageRequest;
import com.kopo.hanagreenworld.chat.dto.ChatMessageResponse;
import com.kopo.hanagreenworld.chat.repository.TeamChatMessageRepository;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.Team;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TeamChatService {

    private final TeamChatMessageRepository messageRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public TeamChatService(TeamChatMessageRepository messageRepository, 
                          TeamRepository teamRepository, 
                          MemberRepository memberRepository) {
        this.messageRepository = messageRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    private static final String CHAT_MESSAGES_KEY = "chat:team:%d:messages";
    private static final String ONLINE_USERS_KEY = "chat:team:%d:online";
    private static final String USER_SESSION_KEY = "chat:user:%d:sessions";
    private static final int MESSAGE_CACHE_SIZE = 100;
    private static final int MESSAGE_CACHE_TTL_HOURS = 24;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Member currentMember) {
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 팀이 활성화되어 있는지 확인
        if (!team.getIsActive()) {
            throw new BusinessException(ErrorCode.TEAM_NOT_ACTIVE);
        }

        // 메시지 생성
        TeamChatMessage message = TeamChatMessage.builder()
                .team(team)
                .sender(currentMember)
                .messageText(request.getMessageText())
                .messageType(TeamChatMessage.MessageType.valueOf(request.getMessageType()))
                .build();

        message = messageRepository.save(message);

        // Redis에 메시지 캐싱
        String redisMessageId = UUID.randomUUID().toString();
        message.setRedisMessageId(redisMessageId);
        messageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.from(message);
        cacheMessage(request.getTeamId(), response);

        return response;
    }

    public List<ChatMessageResponse> getTeamMessages(Long teamId, Member currentMember) {
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 팀 존재 여부 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 최근 50개 메시지 조회 (최신순)
        Pageable pageable = PageRequest.of(0, 50);
        Page<TeamChatMessage> messages = messageRepository.findByTeamIdAndIsDeletedFalseOrderByCreatedAtDesc(teamId, pageable);
        
        // 시간순으로 정렬하여 반환 (오래된 것부터)
        return messages.getContent().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public void joinTeam(Long teamId, Member currentMember) {
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (redisTemplate != null) {
            String onlineKey = String.format(ONLINE_USERS_KEY, teamId);
            String sessionKey = String.format(USER_SESSION_KEY, currentMember.getMemberId());

            // Redis에 온라인 상태 추가
            redisTemplate.opsForSet().add(onlineKey, currentMember.getMemberId().toString());
            redisTemplate.expire(onlineKey, 1, TimeUnit.HOURS);

            // 사용자 세션 정보 저장
            redisTemplate.opsForValue().set(sessionKey, teamId.toString(), 1, TimeUnit.HOURS);
        }
    }

    @Transactional
    public void leaveTeam(Long teamId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (redisTemplate != null) {
            String onlineKey = String.format(ONLINE_USERS_KEY, teamId);
            String sessionKey = String.format(USER_SESSION_KEY, currentMember.getMemberId());

            // Redis에서 온라인 상태 제거
            redisTemplate.opsForSet().remove(onlineKey, currentMember.getMemberId().toString());
            redisTemplate.delete(sessionKey);
        }
    }

    public List<Long> getOnlineUsers(Long teamId) {
        if (redisTemplate != null) {
            String onlineKey = String.format(ONLINE_USERS_KEY, teamId);
            return redisTemplate.opsForSet().members(onlineKey)
                    .stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .toList();
        }
        return List.of();
    }

    private void cacheMessage(Long teamId, ChatMessageResponse message) {
        if (redisTemplate != null) {
            String messagesKey = String.format(CHAT_MESSAGES_KEY, teamId);
            
            // 최근 메시지 리스트에 추가
            redisTemplate.opsForList().leftPush(messagesKey, message);
            
            // 리스트 크기 제한
            redisTemplate.opsForList().trim(messagesKey, 0, MESSAGE_CACHE_SIZE - 1);
            
            // TTL 설정
            redisTemplate.expire(messagesKey, MESSAGE_CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    public List<ChatMessageResponse> getCachedMessages(Long teamId) {
        if (redisTemplate != null) {
            String messagesKey = String.format(CHAT_MESSAGES_KEY, teamId);
            List<Object> cachedMessages = redisTemplate.opsForList().range(messagesKey, 0, -1);
            
            if (cachedMessages == null || cachedMessages.isEmpty()) {
                return List.of();
            }

            return cachedMessages.stream()
                    .map(obj -> (ChatMessageResponse) obj)
                    .toList();
        }
        return List.of();
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (currentMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        TeamChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        // 본인 메시지만 삭제 가능
        if (!message.getSender().getMemberId().equals(currentMember.getMemberId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        message.delete();
        messageRepository.save(message);
    }

    @Transactional
    public void saveSystemMessage(TeamChatMessage systemMessage) {
        messageRepository.save(systemMessage);
        
        // Redis에 메시지 캐싱
        String redisMessageId = UUID.randomUUID().toString();
        systemMessage.setRedisMessageId(redisMessageId);
        messageRepository.save(systemMessage);
        
        // 캐시에 메시지 추가
        ChatMessageResponse response = ChatMessageResponse.from(systemMessage);
        cacheMessage(systemMessage.getTeam().getId(), response);
    }
}
