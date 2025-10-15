package com.kopo.hanagreenworld.activity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "image_hashes", 
       indexes = {
           @Index(name = "idx_image_hash", columnList = "imageHash"),
           @Index(name = "idx_member_id", columnList = "memberId"),
           @Index(name = "idx_created_at", columnList = "createdAt")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ImageHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "image_hash", nullable = false, length = 32)
    private String imageHash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ImageHash(Long memberId, Long challengeId, String imageUrl, 
                    String imageHash, Long fileSize, String contentType) {
        this.memberId = memberId;
        this.challengeId = challengeId;
        this.imageUrl = imageUrl;
        this.imageHash = imageHash;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    /**
     * 이미지 해시 정보 업데이트
     */
    public void updateImageInfo(String imageUrl, String imageHash, Long fileSize, String contentType) {
        this.imageUrl = imageUrl;
        this.imageHash = imageHash;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }
}

