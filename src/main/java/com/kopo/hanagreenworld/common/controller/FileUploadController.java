package com.kopo.hanagreenworld.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload Controller", description = "파일 업로드 관련 API")
public class FileUploadController {

    private static final String UPLOAD_DIR = "challenge_images/";
    
    @Value("${server.url}")
    private String serverUrl;
    
    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/image")
    @Operation(summary = "이미지 업로드", description = "챌린지 인증용 이미지를 업로드합니다.")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 파일명 생성 (UUID + 원본 확장자)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;
            
            // 파일 저장
            Path filePath = uploadPath.resolve(filename);
            log.info("파일 저장 경로: {}", filePath.toAbsolutePath());
            
            long bytesCopied = Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {} bytes 복사됨", bytesCopied);
            
            // 저장된 파일 크기 확인
            long actualFileSize = Files.size(filePath);
            log.info("실제 저장된 파일 크기: {} bytes", actualFileSize);
            
            // 응답 데이터 생성
            log.info("서버 URL 설정: serverUrl={}, serverPort={}", serverUrl, serverPort);
            
            // 안전한 URL 생성
            String baseUrl;
            if (serverUrl != null && serverPort != null) {
                baseUrl = serverUrl + ":" + serverPort + "/";
            } else {
                // 기본값 사용
                baseUrl = "http://localhost:8080/";
                log.warn("서버 URL 설정이 없어 기본값 사용: {}", baseUrl);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", filename);
            response.put("url", baseUrl + "challenge_images/" + filename);
            response.put("localPath", "challenge_images/" + filename);
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());
            
            log.info("이미지 업로드 성공: {}", filename);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "파일 업로드 중 오류가 발생했습니다.");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예상치 못한 오류: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "서버에 문제가 발생하였습니다.");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "파일 업로드 컨트롤러 헬스체크", description = "파일 업로드 컨트롤러가 정상적으로 작동하는지 확인합니다.")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File Upload Controller is working!");
    }
}
