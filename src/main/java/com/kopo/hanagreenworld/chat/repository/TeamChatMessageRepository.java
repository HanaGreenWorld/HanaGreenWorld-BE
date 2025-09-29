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
    
    /**
     * 팀의 최근 메시지 조회 (삭제되지 않은 것만)
     */
    Page<TeamChatMessage> findByTeamIdAndIsDeletedFalseOrderByCreatedAtDesc(Long teamId, Pageable pageable);
    
    /**
     * 팀의 특정 시간 이후 메시지 조회
     */
    @Query("SELECT tcm FROM TeamChatMessage tcm " +
           "WHERE tcm.team.id = :teamId " +
           "AND tcm.isDeleted = false " +
           "AND tcm.createdAt > :since " +
           "ORDER BY tcm.createdAt ASC")
    List<TeamChatMessage> findRecentMessagesByTeamId(@Param("teamId") Long teamId, @Param("since") LocalDateTime since);
    
    /**
     * 팀의 메시지 수 조회
     */
    @Query("SELECT COUNT(tcm) FROM TeamChatMessage tcm " +
           "WHERE tcm.team.id = :teamId " +
           "AND tcm.isDeleted = false")
    Long countMessagesByTeamId(@Param("teamId") Long teamId);
    
    /**
     * Redis 메시지 ID로 메시지 조회
     */
    Optional<TeamChatMessage> findByRedisMessageId(String redisMessageId);
    
    /**
     * 팀의 오래된 메시지 삭제 (보관 기간 초과)
     */
    @Query("DELETE FROM TeamChatMessage tcm " +
           "WHERE tcm.team.id = :teamId " +
           "AND tcm.createdAt < :cutoffDate")
    void deleteOldMessagesByTeamId(@Param("teamId") Long teamId, @Param("cutoffDate") LocalDateTime cutoffDate);
}

