package com.kopo.hanagreenworld.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedCustomerInfoResponse {

    private CustomerSummary customerSummary;

    private BankInfo bankInfo;

    private CardInfo cardInfo;

    private IntegratedBenefits integratedBenefits;

    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary {
        private String name;
        private String email;
        private String phoneNumber;
        private String overallGrade; // 그룹 전체 고객 등급
        private boolean hasBankAccount;
        private boolean hasCardAccount;
        private boolean hasGreenWorldAccount;
        private LocalDateTime firstJoinDate; // 그룹 최초 가입일
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInfo {
        private boolean isAvailable;
        private String customerGrade;
        private int accountCount;
        private int productCount;
        private BigDecimal totalBalance; // 총 잔고 (마스킹된)
        private List<String> mainProducts; // 주요 상품 목록
        private List<ProductDetail> productDetails; // 상품별 상세 정보
        private List<AccountInfo> accounts; // 계좌별 상세 정보
        private String errorMessage; // 조회 실패 시 오류 메시지
        
        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AccountInfo {
            private String accountNumber;
            private String accountType;
            private String accountName;
            private BigDecimal balance;
            private String currency;
            private LocalDateTime openDate;
            private boolean isActive;
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductDetail {
            private String productCode; // 상품 코드 (계좌번호)
            private String productName;
            private String productType;
            private BigDecimal amount;
            private String status;
            private BigDecimal interestRate; // 적용금리
            private BigDecimal baseRate; // 기본금리
            private BigDecimal preferentialRate; // 우대금리
            private LocalDateTime subscriptionDate; // 가입일
            private LocalDateTime maturityDate; // 만기일
            // 대출 관련 추가 필드들
            private BigDecimal remainingAmount; // 잔여금액 (대출용)
            private BigDecimal monthlyPayment; // 월상환금 (대출용)
            private LocalDateTime startDate; // 대출 시작일
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {
        private boolean isAvailable;
        private String customerGrade;
        private int cardCount;
        private BigDecimal totalCreditLimit;
        private BigDecimal availableCredit;
        private boolean hasHanamoney;
        private BigDecimal hanamoneyPoints;
        private String hanamoneyLevel;
        private List<String> mainCards; // 주요 카드 목록
        private String errorMessage; // 조회 실패 시 오류 메시지
        private java.util.Map<String, Object> cardData; // 실제 카드 데이터
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegratedBenefits {
        private String groupCustomerLevel; // 그룹 통합 고객 등급
        private List<String> availableBenefits; // 사용 가능한 혜택
        private BigDecimal totalPoints; // 통합 포인트
        private List<String> recommendedProducts; // 추천 상품
        private boolean eligibleForPremiumService; // 프리미엄 서비스 자격 여부
    }
}





