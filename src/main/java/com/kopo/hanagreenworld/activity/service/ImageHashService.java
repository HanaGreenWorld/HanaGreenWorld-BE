package com.kopo.hanagreenworld.activity.service;

import com.kopo.hanagreenworld.activity.domain.ImageHash;
import com.kopo.hanagreenworld.activity.repository.ImageHashRepository;
import com.kopo.hanagreenworld.member.service.MemberProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImageHashService {

    private final ImageHashRepository imageHashRepository;
    private final MemberProfileService memberProfileService;

    @Transactional
    public ImageHashResult checkImageDuplicate(String imageUrl, Long memberId, Long challengeId) {
        try {
            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                return ImageHashResult.builder()
                        .isDuplicate(false)
                        .confidence(0.0)
                        .reason("이미지를 다운로드할 수 없습니다.")
                        .build();
            }

            String imageHash = calculateImageHash(imageBytes);
            if (imageHash == null) {
                return ImageHashResult.builder()
                        .isDuplicate(false)
                        .confidence(0.0)
                        .reason("이미지 해시를 계산할 수 없습니다.")
                        .build();
            }

            DuplicateCheckResult duplicateResult = checkForDuplicates(imageHash, memberId, challengeId);

            return ImageHashResult.builder()
                    .isDuplicate(duplicateResult.isDuplicate())
                    .confidence(duplicateResult.confidence())
                    .reason(duplicateResult.reason())
                    .imageHash(imageHash)
                    .duplicateType(duplicateResult.duplicateType())
                    .build();

        } catch (Exception e) {
            log.error("이미지 중복 검사 중 오류 발생: {}", e.getMessage(), e);
            return ImageHashResult.builder()
                    .isDuplicate(false)
                    .confidence(0.0)
                    .reason("이미지 중복 검사 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    private String calculateImageHash(byte[] imageBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(imageBytes);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 알고리즘을 찾을 수 없습니다: {}", e.getMessage());
            return null;
        }
    }

    private DuplicateCheckResult checkForDuplicates(String imageHash, Long memberId, Long challengeId) {
        // 1. 같은 사용자가 어떤 챌린지든 같은 해시값을 사용했는지 확인
        boolean sameUserDuplicate = imageHashRepository.existsByMemberIdAndImageHash(memberId, imageHash);
        if (sameUserDuplicate) {
            log.warn("같은 사용자의 중복 이미지 감지: 사용자 {}, 해시: {}", memberId, imageHash);
            return new DuplicateCheckResult(true, 0.0, "이전에 사용한 이미지입니다.", "SAME_USER");
        }

        // 2. 다른 사용자가 같은 해시값을 사용했는지 확인
        boolean otherUserDuplicate = imageHashRepository.existsByImageHash(imageHash);
        if (otherUserDuplicate) {
            // 몇 명의 사용자가 같은 이미지를 사용했는지 확인
            long userCount = imageHashRepository.countDistinctMembersByImageHash(imageHash);
            log.warn("다른 사용자와 중복 이미지 감지: 사용자 {}, 해시: {}, 사용자 수: {}", memberId, imageHash, userCount);
            
            String reason = String.format("다른 %d명의 사용자가 사용한 이미지입니다.", userCount);
            return new DuplicateCheckResult(true, 0.1, reason, "OTHER_USER");
        }

        // 3. 중복이 없는 경우
        log.info("중복 이미지 없음: 사용자 {}, 해시: {}", memberId, imageHash);
        return new DuplicateCheckResult(false, 0.9, "중복 이미지가 없습니다.", "NONE");
    }

    private void saveImageHash(Long memberId, Long challengeId, String imageUrl, String imageHash, long fileSize) {
        try {
            // 기존에 같은 챌린지에 대한 해시가 있는지 확인
            Optional<ImageHash> existingHash = imageHashRepository.findByMemberIdAndChallengeId(memberId, challengeId);
            
            if (existingHash.isPresent()) {
                // 기존 해시 정보 업데이트
                ImageHash hash = existingHash.get();
                hash.updateImageInfo(imageUrl, imageHash, fileSize, "image/jpeg");
                imageHashRepository.save(hash);
                log.info("📝 기존 이미지 해시 정보 업데이트: {}", imageHash);
            } else {
                // 새로운 해시 정보 저장
                ImageHash newHash = ImageHash.builder()
                        .memberId(memberId)
                        .challengeId(challengeId)
                        .imageUrl(imageUrl)
                        .imageHash(imageHash)
                        .fileSize(fileSize)
                        .contentType("image/jpeg")
                        .build();
                
                imageHashRepository.save(newHash);
                log.info("💾 새로운 이미지 해시 정보 저장: {}", imageHash);
            }
        } catch (Exception e) {
            log.error("이미지 해시 정보 저장 실패: {}", e.getMessage(), e);
        }
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            // URL에서 로컬 파일 경로 추출
            String localPath = extractLocalPath(imageUrl);
            if (localPath != null) {
                Path filePath = Paths.get(localPath);
                if (Files.exists(filePath)) {
                    log.info("로컬 파일에서 이미지 읽기: {}", localPath);
                    return Files.readAllBytes(filePath);
                }
            }
            
            // URL에서 다운로드
            URL url = new URL(imageUrl);
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", imageUrl, e);
            return null;
        }
    }

    private String extractLocalPath(String imageUrl) {
        try {
            if (imageUrl.contains("/challenge_images/")) {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/challenge_images/") + "/challenge_images/".length());
                return "challenge_images/" + fileName;
            }
            return null;
        } catch (Exception e) {
            log.warn("로컬 경로 추출 실패: {}", imageUrl);
            return null;
        }
    }

    @Transactional
    public void saveImageHashAfterVerification(String imageUrl, Long memberId, Long challengeId) {
        try {
            log.info("💾 AI 검증 성공 후 이미지 해시 저장: 사용자 {}, 챌린지 {}", memberId, challengeId);
            
            // 이미지 다운로드
            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("이미지 다운로드 실패로 해시 저장 건너뜀: {}", imageUrl);
                return;
            }

            // 이미지 해시 계산
            String imageHash = calculateImageHash(imageBytes);
            if (imageHash == null) {
                log.warn("이미지 해시 계산 실패로 저장 건너뜀: {}", imageUrl);
                return;
            }

            // 이미지 해시 정보 저장
            saveImageHash(memberId, challengeId, imageUrl, imageHash, imageBytes.length);
            
        } catch (Exception e) {
            log.error("AI 검증 후 이미지 해시 저장 실패: {}", e.getMessage(), e);
        }
    }

    public ImageHashStats getUserImageHashStats(Long memberId) {
        try {
            long totalImages = imageHashRepository.countByMemberId(memberId);
            List<ImageHash> recentImages = imageHashRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
            
            return ImageHashStats.builder()
                    .memberId(memberId)
                    .totalImages(totalImages)
                    .recentImageCount(Math.min(recentImages.size(), 10))
                    .lastImageDate(recentImages.isEmpty() ? null : recentImages.get(0).getCreatedAt())
                    .build();
        } catch (Exception e) {
            log.error("사용자 이미지 해시 통계 조회 실패: {}", e.getMessage());
            return ImageHashStats.builder()
                    .memberId(memberId)
                    .totalImages(0)
                    .recentImageCount(0)
                    .build();
        }
    }

    public record DuplicateCheckResult(
            boolean isDuplicate,
            double confidence,
            String reason,
            String duplicateType
    ) {}

    /**
     * 이미지 해시 검사 결과 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ImageHashResult {
        private boolean isDuplicate;
        private double confidence;
        private String reason;
        private String imageHash;
        private String duplicateType; // SAME_USER, OTHER_USER, NONE
    }

    /**
     * 이미지 해시 통계 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ImageHashStats {
        private Long memberId;
        private long totalImages;
        private int recentImageCount;
        private LocalDateTime lastImageDate;
    }
}
