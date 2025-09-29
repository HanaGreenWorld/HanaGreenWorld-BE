package com.kopo.hanagreenworld.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_join_requests")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TeamJoinRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "team_id", nullable = false)
    private Long teamId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "message", length = 100)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "processed_by")
    private Long processedBy;
    
    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
    
    public void approve(Long processedBy) {
        this.status = RequestStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }
    
    public void reject(Long processedBy) {
        this.status = RequestStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }
}



