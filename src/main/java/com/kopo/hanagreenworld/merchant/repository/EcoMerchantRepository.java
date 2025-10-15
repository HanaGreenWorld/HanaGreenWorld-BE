package com.kopo.hanagreenworld.merchant.repository;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcoMerchantRepository extends JpaRepository<EcoMerchant, Long> {
    
    // 반경 내 가맹점 검색 (안전한 버전)
    @Query("""
        SELECT e, 
               (6371 * ACOS(
                   GREATEST(-1.0, LEAST(1.0,
                       COS(RADIANS(:latitude)) * COS(RADIANS(e.latitude)) * COS(RADIANS(e.longitude) - RADIANS(:longitude)) +
                       SIN(RADIANS(:latitude)) * SIN(RADIANS(e.latitude))
                   ))
               )) as distance
        FROM EcoMerchant e 
        WHERE e.isActive = true 
        AND (6371 * ACOS(
            GREATEST(-1.0, LEAST(1.0,
                COS(RADIANS(:latitude)) * COS(RADIANS(e.latitude)) * COS(RADIANS(e.longitude) - RADIANS(:longitude)) +
                SIN(RADIANS(:latitude)) * SIN(RADIANS(e.latitude))
            ))
        )) <= :radius
        ORDER BY distance
        """)
    List<Object[]> findNearbyMerchants(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("radius") Integer radius
    );
    
    // 카테고리별 검색
    List<EcoMerchant> findByCategoryAndIsActiveTrue(EcoMerchant.MerchantCategory category);
    
    // 검증된 가맹점만 조회
    List<EcoMerchant> findByIsVerifiedTrueAndIsActiveTrue();
    
    // 가맹점명으로 검색
    List<EcoMerchant> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    // 카테고리와 검증 상태로 조회
    List<EcoMerchant> findByCategoryAndIsVerifiedAndIsActiveTrue(
        EcoMerchant.MerchantCategory category, 
        Boolean isVerified
    );
    
    // 사업자 번호로 활성 가맹점 조회 (친환경 가맹점 매칭용)
    Optional<EcoMerchant> findByBusinessNumberAndIsActiveTrue(String businessNumber);
    
    // 모든 활성 가맹점 조회
    List<EcoMerchant> findByIsActiveTrue();
}
