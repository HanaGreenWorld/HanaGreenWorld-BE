package com.kopo.hanagreenworld.integration.domain;

import com.kopo.hanagreenworld.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하나금융그룹 내 고객 통합 매핑 테이블
 * CI(Connecting Information)를 안전하게 관리하고 그룹사 간 고객 식별을 위한 엔티티
 */
@Entity
@Table(name = "group_customer_mapping", indexes = {
    @Index(name = "idx_group_customer_token", columnList = "groupCustomerToken", unique = true),
    @Index(name = "idx_phone_number", columnList = "phoneNumber"),
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupCustomerMapping extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 그룹 내부에서 사용하는 고객 식별 토큰 (CI 대신 사용)
     * UUID 형태로 생성되어 외부 유출되어도 개인정보 노출 위험이 없음
     */
    @Column(name = "group_customer_token", nullable = false, unique = true, length = 36)
    private String groupCustomerToken;

    /**
     * CI (Connecting Information) - 암호화 저장
     * 실제 운영에서는 암호화 모듈로 처리해야 함
     */
    @Column(name = "encrypted_ci", nullable = false, length = 255)
    private String encryptedCi;

    /**
     * 고객 기본 정보 (매핑 확인용)
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "birth_date", nullable = false, length = 10)
    private String birthDate;

    /**
     * 그룹사별 고객 존재 여부
     */
    @Column(name = "has_bank_account", nullable = false)
    private Boolean hasBankAccount = false;

    @Column(name = "has_card_account", nullable = false)
    private Boolean hasCardAccount = false;

    @Column(name = "has_green_world_account", nullable = false)
    private Boolean hasGreenWorldAccount = false;

    /**
     * 마지막 동기화 시간
     */
    @Column(name = "last_sync_at")
    private java.time.LocalDateTime lastSyncAt;

    @Builder
    public GroupCustomerMapping(String groupCustomerToken, String encryptedCi, String name, 
                               String phoneNumber, String email, String birthDate) {
        this.groupCustomerToken = groupCustomerToken;
        this.encryptedCi = encryptedCi;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.birthDate = birthDate;
    }

    /**
     * 그룹사 계정 존재 여부 업데이트
     */
    public void updateAccountStatus(boolean hasBankAccount, boolean hasCardAccount, boolean hasGreenWorldAccount) {
        this.hasBankAccount = hasBankAccount;
        this.hasCardAccount = hasCardAccount;
        this.hasGreenWorldAccount = hasGreenWorldAccount;
        this.lastSyncAt = java.time.LocalDateTime.now();
    }

    /**
     * 은행 계정 연결
     */
    public void linkBankAccount() {
        this.hasBankAccount = true;
        this.lastSyncAt = java.time.LocalDateTime.now();
    }

    /**
     * 카드 계정 연결
     */
    public void linkCardAccount() {
        this.hasCardAccount = true;
        this.lastSyncAt = java.time.LocalDateTime.now();
    }

    /**
     * 그린월드 계정 연결
     */
    public void linkGreenWorldAccount() {
        this.hasGreenWorldAccount = true;
        this.lastSyncAt = java.time.LocalDateTime.now();
    }

    /**
     * 그룹 고객 토큰 업데이트
     */
    public void setGroupCustomerToken(String groupCustomerToken) {
        this.groupCustomerToken = groupCustomerToken;
        this.lastSyncAt = java.time.LocalDateTime.now();
    }

    /**
     * 마지막 동기화 시간 업데이트
     */
    public void setLastSyncAt(java.time.LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}





