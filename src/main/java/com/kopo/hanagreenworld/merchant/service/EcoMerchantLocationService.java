package com.kopo.hanagreenworld.merchant.service;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import com.kopo.hanagreenworld.merchant.dto.EcoMerchantLocationDto;
import com.kopo.hanagreenworld.merchant.dto.LocationSearchRequest;
import com.kopo.hanagreenworld.merchant.repository.EcoMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class EcoMerchantLocationService {
    
    private final EcoMerchantRepository merchantRepository;
    
    // 주변 친환경 가맹점 검색
    public List<EcoMerchantLocationDto> findNearbyMerchants(LocationSearchRequest request) {
        log.info("주변 가맹점 검색 요청: lat={}, lng={}, radius={}km", 
                request.getLatitude(), request.getLongitude(), request.getRadius());
        
        List<Object[]> results = merchantRepository.findNearbyMerchants(
            request.getLatitude(),
            request.getLongitude(),
            request.getRadius()
        );
        
        List<EcoMerchantLocationDto> merchants = results.stream()
            .map(this::mapToLocationDto)
            .collect(Collectors.toList());
        
        // 추가 필터링 적용
        merchants = applyAdditionalFilters(merchants, request);
        
        log.info("검색 결과: {}개의 가맹점 발견", merchants.size());
        return merchants;
    }
    
    // 카테고리별 가맹점 검색
    public List<EcoMerchantLocationDto> findMerchantsByCategory(EcoMerchant.MerchantCategory category) {
        log.info("카테고리별 가맹점 검색: {}", category);
        
        return merchantRepository.findByCategoryAndIsActiveTrue(category)
            .stream()
            .map(EcoMerchantLocationDto::from)
            .collect(Collectors.toList());
    }
    
    // 검증된 가맹점만 조회
    public List<EcoMerchantLocationDto> findVerifiedMerchants() {
        log.info("검증된 가맹점 조회");
        
        return merchantRepository.findByIsVerifiedTrueAndIsActiveTrue()
            .stream()
            .map(EcoMerchantLocationDto::from)
            .collect(Collectors.toList());
    }
    
    // 가맹점명으로 검색
    public List<EcoMerchantLocationDto> searchMerchantsByName(String keyword) {
        log.info("가맹점명 검색: {}", keyword);
        
        return merchantRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword)
            .stream()
            .map(EcoMerchantLocationDto::from)
            .collect(Collectors.toList());
    }
    
    // 모든 활성 가맹점 조회
    public List<EcoMerchantLocationDto> findAllActiveMerchants() {
        log.info("모든 활성 가맹점 조회");
        
        return merchantRepository.findByIsActiveTrue()
            .stream()
            .map(EcoMerchantLocationDto::from)
            .collect(Collectors.toList());
    }
    
    // Object[] 결과를 DTO로 변환
    private EcoMerchantLocationDto mapToLocationDto(Object[] result) {
        EcoMerchant merchant = (EcoMerchant) result[0];
        Double distance = (Double) result[1];
        
        return EcoMerchantLocationDto.from(merchant, distance / 1000.0); // 미터를 킬로미터로 변환
    }
    
    // 추가 필터링 적용
    private List<EcoMerchantLocationDto> applyAdditionalFilters(
            List<EcoMerchantLocationDto> merchants, 
            LocationSearchRequest request) {
        
        // 검증된 가맹점만 필터링
        if (request.getVerifiedOnly() != null && request.getVerifiedOnly()) {
            merchants = merchants.stream()
                .filter(EcoMerchantLocationDto::getIsVerified)
                .collect(Collectors.toList());
        }
        
        // 카테고리 필터링
        if (request.getCategory() != null) {
            merchants = merchants.stream()
                .filter(m -> m.getCategory().equals(request.getCategory().name()))
                .collect(Collectors.toList());
        }
        
        // 키워드 검색
        if (request.getSearchKeyword() != null && !request.getSearchKeyword().trim().isEmpty()) {
            String keyword = request.getSearchKeyword().toLowerCase();
            merchants = merchants.stream()
                .filter(m -> m.getName().toLowerCase().contains(keyword) ||
                           m.getAddress().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }
        
        return merchants;
    }
}

