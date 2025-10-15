package com.kopo.hanagreenworld.merchant.controller;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import com.kopo.hanagreenworld.merchant.dto.EcoMerchantLocationDto;
import com.kopo.hanagreenworld.merchant.dto.LocationSearchRequest;
import com.kopo.hanagreenworld.merchant.service.EcoMerchantLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/merchants/location")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "친환경 가맹점 위치 API", description = "친환경 가맹점 위치 검색 및 조회 API")
public class EcoMerchantLocationController {
    
    private final EcoMerchantLocationService locationService;
    
    @PostMapping("/nearby")
    @Operation(summary = "주변 친환경 가맹점 검색", 
               description = "사용자 위치 기준으로 반경 내 친환경 가맹점을 검색합니다.")
    public ResponseEntity<List<EcoMerchantLocationDto>> findNearbyMerchants(
            @Valid @RequestBody LocationSearchRequest request) {

        List<EcoMerchantLocationDto> merchants = locationService.findNearbyMerchants(request);
        
        return ResponseEntity.ok(merchants);
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 가맹점 검색", 
               description = "특정 카테고리의 친환경 가맹점을 검색합니다.")
    public ResponseEntity<List<EcoMerchantLocationDto>> findMerchantsByCategory(
            @PathVariable EcoMerchant.MerchantCategory category) {

        List<EcoMerchantLocationDto> merchants = locationService.findMerchantsByCategory(category);
        
        return ResponseEntity.ok(merchants);
    }
    
    @GetMapping("/verified")
    @Operation(summary = "검증된 가맹점 조회", 
               description = "검증된 친환경 가맹점만 조회합니다.")
    public ResponseEntity<List<EcoMerchantLocationDto>> findVerifiedMerchants() {
        
        List<EcoMerchantLocationDto> merchants = locationService.findVerifiedMerchants();
        
        return ResponseEntity.ok(merchants);
    }
    
    @GetMapping("/search")
    @Operation(summary = "가맹점명 검색", 
               description = "가맹점명으로 친환경 가맹점을 검색합니다.")
    public ResponseEntity<List<EcoMerchantLocationDto>> searchMerchantsByName(
            @RequestParam String keyword) {
        
        List<EcoMerchantLocationDto> merchants = locationService.searchMerchantsByName(keyword);
        
        return ResponseEntity.ok(merchants);
    }
    
    @GetMapping("/all")
    @Operation(summary = "모든 활성 가맹점 조회", 
               description = "모든 활성화된 친환경 가맹점을 조회합니다.")
    public ResponseEntity<List<EcoMerchantLocationDto>> findAllActiveMerchants() {
        
        List<EcoMerchantLocationDto> merchants = locationService.findAllActiveMerchants();
        
        return ResponseEntity.ok(merchants);
    }
    
    @GetMapping("/categories")
    @Operation(summary = "카테고리 목록 조회", 
               description = "사용 가능한 가맹점 카테고리 목록을 조회합니다.")
    public ResponseEntity<EcoMerchant.MerchantCategory[]> getCategories() {
        
        EcoMerchant.MerchantCategory[] categories = EcoMerchant.MerchantCategory.values();
        
        return ResponseEntity.ok(categories);
    }
}

