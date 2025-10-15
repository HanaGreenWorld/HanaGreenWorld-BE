package com.kopo.hanagreenworld.activity.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageMetadataService {

    public ImageMetadataResult validateImageMetadata(String imageUrl, LocalDateTime challengeParticipationDate) {
        try {
            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                return ImageMetadataResult.builder()
                        .isValid(false)
                        .confidence(0.0)
                        .reason("이미지를 다운로드할 수 없습니다.")
                        .build();
            }

            Metadata metadata = extractMetadata(imageBytes);
            if (metadata == null) {
                return ImageMetadataResult.builder()
                        .isValid(false)
                        .confidence(0.3)
                        .reason("EXIF 데이터를 추출할 수 없습니다.")
                        .build();
            }

            // 메타데이터 분석
            return analyzeMetadata(metadata, imageUrl, challengeParticipationDate);

        } catch (Exception e) {
            log.error("이미지 메타데이터 검증 중 오류 발생: {}", e.getMessage(), e);
            return ImageMetadataResult.builder()
                    .isValid(false)
                    .confidence(0.0)
                    .reason("메타데이터 검증 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    private Metadata extractMetadata(byte[] imageBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            return ImageMetadataReader.readMetadata(inputStream);
        } catch (ImageProcessingException | IOException e) {
            log.warn("EXIF 데이터 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    private ImageMetadataResult analyzeMetadata(Metadata metadata, String imageUrl, LocalDateTime challengeParticipationDate) {
        Map<String, Object> metadataInfo = new HashMap<>();
        double confidence = 0.5; // 기본 신뢰도
        StringBuilder reasons = new StringBuilder();

        // 1. 촬영 시간 검증 (챌린지 참여 신청 후 촬영된 사진인지 확인)
        LocalDateTime captureTime = extractCaptureTime(metadata);
        if (captureTime != null) {
            metadataInfo.put("captureTime", captureTime);
            metadataInfo.put("challengeParticipationDate", challengeParticipationDate);
            
            // 촬영 시간이 챌린지 참여 신청 날짜 이후인지 확인
            if (captureTime.isBefore(challengeParticipationDate)) {
                confidence -= 0.4;
                reasons.append("촬영 시간이 챌린지 참여 신청 이전입니다. ");
                log.warn("부정 행위 의심: 촬영시간({}) < 참여신청시간({})", captureTime, challengeParticipationDate);
            } else {
                // 촬영 시간이 참여 신청 후인 경우, 적절한 시간 범위인지 확인
                long hoursAfterParticipation = java.time.Duration.between(challengeParticipationDate, captureTime).toHours();
                
                if (hoursAfterParticipation > 72) { // 3일 이상 지난 경우
                    confidence -= 0.1;
                    reasons.append("촬영 시간이 참여 신청 후 3일 이상 지났습니다. ");
                } else if (hoursAfterParticipation < 0) {
                    confidence -= 0.3;
                    reasons.append("미래 시간으로 설정된 촬영 시간입니다. ");
                } else {
                    confidence += 0.15;
                    reasons.append("촬영 시간이 챌린지 참여 신청 후 적절한 시점입니다. ");
                }
            }
        } else {
            confidence -= 0.1;
            reasons.append("촬영 시간 정보가 없습니다. ");
        }

        // 2. GPS 위치 정보 검증
        Map<String, Double> gpsInfo = extractGpsInfo(metadata);
        if (gpsInfo != null && !gpsInfo.isEmpty()) {
            metadataInfo.put("gpsInfo", gpsInfo);
            confidence += 0.1;
            reasons.append("GPS 위치 정보가 있습니다. ");
        } else {
            confidence -= 0.05;
            reasons.append("GPS 위치 정보가 없습니다. ");
        }

        // 3. 카메라 정보 검증
        String cameraInfo = extractCameraInfo(metadata);
        if (cameraInfo != null && !cameraInfo.isEmpty()) {
            metadataInfo.put("cameraInfo", cameraInfo);
            
            // 스마트폰 카메라인지 확인
            if (cameraInfo.toLowerCase().contains("iphone") || 
                cameraInfo.toLowerCase().contains("samsung") ||
                cameraInfo.toLowerCase().contains("android")) {
                confidence += 0.1;
                reasons.append("스마트폰으로 촬영된 것으로 보입니다. ");
            } else {
                confidence += 0.05;
                reasons.append("카메라 정보가 있습니다. ");
            }
        } else {
            confidence -= 0.05;
            reasons.append("카메라 정보가 없습니다. ");
        }

        // 4. 이미지 크기 및 해상도 검증
        Map<String, Integer> imageSize = extractImageSize(metadata);
        if (imageSize != null && !imageSize.isEmpty()) {
            metadataInfo.put("imageSize", imageSize);
            
            int width = imageSize.get("width");
            int height = imageSize.get("height");
            
            // 너무 작거나 큰 이미지 검증
            if (width < 200 || height < 200) {
                confidence -= 0.2;
                reasons.append("이미지 해상도가 너무 낮습니다. ");
            } else if (width > 8000 || height > 8000) {
                confidence -= 0.1;
                reasons.append("이미지 해상도가 비정상적으로 큽니다. ");
            } else {
                confidence += 0.05;
                reasons.append("이미지 해상도가 적절합니다. ");
            }
        }

        // 5. 편집 여부 검증
        boolean isEdited = checkIfEdited(metadata);
        if (isEdited) {
            confidence -= 0.2;
            reasons.append("이미지가 편집된 것으로 보입니다. ");
        } else {
            confidence += 0.05;
            reasons.append("원본 이미지로 보입니다. ");
        }

        // 신뢰도 범위 조정 (0.0 ~ 1.0)
        confidence = Math.max(0.0, Math.min(1.0, confidence));

        boolean isValid = confidence >= 0.4; // 40% 이상이면 유효

        return ImageMetadataResult.builder()
                .isValid(isValid)
                .confidence(confidence)
                .reason(reasons.toString())
                .metadataInfo(metadataInfo)
                .build();
    }

    private LocalDateTime extractCaptureTime(Metadata metadata) {
        try {
            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null && exifDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                java.util.Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.debug("촬영 시간 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, Double> extractGpsInfo(Metadata metadata) {
        try {
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                Map<String, Double> gpsInfo = new HashMap<>();
                
                if (gpsDirectory.containsTag(GpsDirectory.TAG_LATITUDE) && 
                    gpsDirectory.containsTag(GpsDirectory.TAG_LONGITUDE)) {
                    gpsInfo.put("latitude", gpsDirectory.getGeoLocation().getLatitude());
                    gpsInfo.put("longitude", gpsDirectory.getGeoLocation().getLongitude());
                    return gpsInfo;
                }
            }
        } catch (Exception e) {
            log.debug("GPS 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractCameraInfo(Metadata metadata) {
        try {
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifDirectory != null) {
                String make = exifDirectory.getString(ExifIFD0Directory.TAG_MAKE);
                String model = exifDirectory.getString(ExifIFD0Directory.TAG_MODEL);
                
                if (make != null && model != null) {
                    return make + " " + model;
                } else if (make != null) {
                    return make;
                } else if (model != null) {
                    return model;
                }
            }
        } catch (Exception e) {
            log.debug("카메라 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, Integer> extractImageSize(Metadata metadata) {
        try {
            for (Directory directory : metadata.getDirectories()) {
                if (directory.containsTag(0x0100) && directory.containsTag(0x0101)) { // ImageWidth, ImageLength
                    Map<String, Integer> size = new HashMap<>();
                    size.put("width", directory.getInt(0x0100));
                    size.put("height", directory.getInt(0x0101));
                    return size;
                }
            }
        } catch (Exception e) {
            log.debug("이미지 크기 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private boolean checkIfEdited(Metadata metadata) {
        try {
            // 소프트웨어 정보 확인
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifDirectory != null && exifDirectory.containsTag(ExifIFD0Directory.TAG_SOFTWARE)) {
                String software = exifDirectory.getString(ExifIFD0Directory.TAG_SOFTWARE);
                if (software != null) {
                    String lowerSoftware = software.toLowerCase();
                    // 편집 소프트웨어 키워드 확인
                    return lowerSoftware.contains("photoshop") || 
                           lowerSoftware.contains("gimp") || 
                           lowerSoftware.contains("lightroom") ||
                           lowerSoftware.contains("paint") ||
                           lowerSoftware.contains("editor");
                }
            }
        } catch (Exception e) {
            log.debug("편집 여부 확인 실패: {}", e.getMessage());
        }
        return false;
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

    /**
     * 이미지 메타데이터 검증 결과 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ImageMetadataResult {
        private boolean isValid;
        private double confidence;
        private String reason;
        private Map<String, Object> metadataInfo;
    }
}
