package com.kopo.hanagreenworld.chat.repository;

import com.kopo.hanagreenworld.chat.domain.TeamChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamChatMessageRepository extends JpaRepository<TeamChatMessage, Long> {

    Page<TeamChatMessage> findByTeamIdAndIsDeletedFalseOrderByCreatedAtDesc(Long teamId, Pageable pageable);
}

