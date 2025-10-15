package com.kopo.hanagreenworld.product.controller;

import java.math.BigInteger;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import com.kopo.hanagreenworld.product.service.SavingsApplicationService;
import com.kopo.hanagreenworld.product.service.SavingsApplicationService.SavingsApplicationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/savings-applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Savings Application", description = "적금 가입 신청 관리 API")
public class SavingsApplicationController {

    private final SavingsApplicationService savingsApplicationService;

    @PostMapping
    @Operation(summary = "적금 가입 신청", description = "적금 가입 신청 정보를 처리합니다. 실제 계좌 생성은 프론트엔드에서 하나은행 API를 직접 호출해야 합니다.")
    public ResponseEntity<ApiResponse<SavingsApplicationResponse>> createApplication(
            @RequestBody CreateSavingsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        SavingsApplicationService.CreateSavingsRequest serviceRequest;
        
        // 자동이체 설정이 있는 경우
        if (request.getAutoTransferEnabled() != null && request.getAutoTransferEnabled()) {
            serviceRequest = new SavingsApplicationService.CreateSavingsRequest(
                request.getSavingProductId(),
                request.getApplicationAmount(),
                request.getWithdrawalAccountNumber(),
                request.getWithdrawalBankName(),
                request.getAutoTransferEnabled(),
                request.getTransferDay(),
                request.getMonthlyTransferAmount()
            );
        } else {
            // 자동이체 설정이 없는 경우
            serviceRequest = new SavingsApplicationService.CreateSavingsRequest(
                request.getSavingProductId(),
                request.getApplicationAmount(),
                request.getWithdrawalAccountNumber(),
                request.getWithdrawalBankName()
            );
        }

        SavingsApplicationResponse response = savingsApplicationService.processSavingsApplication(userId, serviceRequest);

        return ResponseEntity.ok(ApiResponse.success("적금 계좌가 성공적으로 생성되었습니다.", response));
    }


    public static class CreateSavingsRequest {
        private Long savingProductId;
        private BigInteger applicationAmount;
        private String withdrawalAccountNumber;
        private String withdrawalBankName;
        
        // 자동이체 관련 필드들
        private Boolean autoTransferEnabled;
        private Integer transferDay;
        private Long monthlyTransferAmount;

        public Long getSavingProductId() { return savingProductId; }
        public void setSavingProductId(Long savingProductId) { this.savingProductId = savingProductId; }
        
        public BigInteger getApplicationAmount() { return applicationAmount; }
        public void setApplicationAmount(BigInteger applicationAmount) { this.applicationAmount = applicationAmount; }
        
        public String getWithdrawalAccountNumber() { return withdrawalAccountNumber; }
        public void setWithdrawalAccountNumber(String withdrawalAccountNumber) { this.withdrawalAccountNumber = withdrawalAccountNumber; }
        
        public String getWithdrawalBankName() { return withdrawalBankName; }
        public void setWithdrawalBankName(String withdrawalBankName) { this.withdrawalBankName = withdrawalBankName; }
        
        public Boolean getAutoTransferEnabled() { return autoTransferEnabled; }
        public void setAutoTransferEnabled(Boolean autoTransferEnabled) { this.autoTransferEnabled = autoTransferEnabled; }
        
        public Integer getTransferDay() { return transferDay; }
        public void setTransferDay(Integer transferDay) { this.transferDay = transferDay; }
        
        public Long getMonthlyTransferAmount() { return monthlyTransferAmount; }
        public void setMonthlyTransferAmount(Long monthlyTransferAmount) { this.monthlyTransferAmount = monthlyTransferAmount; }
    }
}