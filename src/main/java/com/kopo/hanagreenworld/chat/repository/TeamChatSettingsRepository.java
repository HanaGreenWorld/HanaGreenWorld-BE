package com.kopo.hanagreenworld.chat.repository;

import com.kopo.hanagreenworld.chat.domain.TeamChatSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamChatSettingsRepository extends JpaRepository<TeamChatSettings, Long> {
    
    /**
     * 팀 ID로 채팅 설정 조회
     */
    Optional<TeamChatSettings> findByTeamId(Long teamId);
    
    /**
     * 활성화된 채팅 설정만 조회
     */
    Optional<TeamChatSettings> findByTeamIdAndIsChatActiveTrue(Long teamId);
}

