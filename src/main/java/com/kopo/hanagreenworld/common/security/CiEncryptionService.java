package com.kopo.hanagreenworld.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class CiEncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public String encryptCi(String ci) {
        try {
            log.debug("CI 암호화 시작: {}", maskCi(ci));
            
            SecretKeySpec secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(ci.getBytes(StandardCharsets.UTF_8));
            String encryptedCi = Base64.getEncoder().encodeToString(encryptedBytes);
            
            log.debug("CI 암호화 완료: {}", maskEncryptedCi(encryptedCi));
            return encryptedCi;
            
        } catch (Exception e) {
            log.error("CI 암호화 실패", e);
            throw new RuntimeException("CI 암호화에 실패했습니다.", e);
        }
    }

    public String decryptCi(String encryptedCi) {
        try {
            log.debug("CI 복호화 시작: {}", maskEncryptedCi(encryptedCi));
            
            SecretKeySpec secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedCi);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            String decryptedCi = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            log.debug("CI 복호화 완료: {}", maskCi(decryptedCi));
            return decryptedCi;
            
        } catch (Exception e) {
            log.error("CI 복호화 실패", e);
            throw new RuntimeException("CI 복호화에 실패했습니다.", e);
        }
    }

    public boolean validateCi(String ci) {
        if (ci == null || ci.trim().isEmpty()) {
            return false;
        }
        
        // CI 형식 검증 (예: 32자리 해시값)
        return ci.matches("^[a-fA-F0-9]{32}$");
    }

    private String maskCi(String ci) {
        if (ci == null || ci.length() < 8) {
            return "****";
        }
        return ci.substring(0, 4) + "****" + ci.substring(ci.length() - 4);
    }


    private String maskEncryptedCi(String encryptedCi) {
        if (encryptedCi == null || encryptedCi.length() < 8) {
            return "****";
        }
        return encryptedCi.substring(0, 8) + "****" + encryptedCi.substring(encryptedCi.length() - 8);
    }
}
