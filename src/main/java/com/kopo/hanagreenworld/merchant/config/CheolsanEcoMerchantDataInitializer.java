package com.kopo.hanagreenworld.merchant.config;

import com.kopo.hanagreenworld.merchant.domain.EcoMerchant;
import com.kopo.hanagreenworld.merchant.repository.EcoMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 철산역 근처 친환경 가맹점 데이터 초기화
 * 철산역 좌표: 37.5551, 126.8368
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheolsanEcoMerchantDataInitializer implements CommandLineRunner {

    private final EcoMerchantRepository ecoMerchantRepository;

    @Override
    public void run(String... args) throws Exception {
        if (ecoMerchantRepository.count() > 0) {
            log.info("친환경 가맹점 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("철산역 근처 친환경 가맹점 데이터 초기화 시작...");

        List<EcoMerchant> cheolsanMerchants = Arrays.asList(
            // 1. 친환경 식품/매장
            EcoMerchant.builder()
                .businessNumber("123-45-67890")
                .name("그린마트 철산점")
                .category(EcoMerchant.MerchantCategory.ECO_FOOD)
                .description("유기농 채소, 친환경 육류, 무농약 과일을 판매하는 친환경 마트입니다.")
                .address("서울특별시 광진구 아차산로 123")
                .latitude(new BigDecimal("37.5551"))
                .longitude(new BigDecimal("126.8368"))
                .phoneNumber("02-1234-5678")
                .websiteUrl("https://greenmart.co.kr")
                .businessHours("08:00-22:00")
                .holiday("매월 둘째, 넷째 일요일")
                .ecoCertifications("[\"유기농인증\", \"친환경농산물인증\"]")
                .ecoPractices("[\"비닐봉지 사용 최소화\", \"재활용 포장재 사용\", \"지역 농산물 우선 판매\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 2. 전기차 충전소
            EcoMerchant.builder()
                .businessNumber("234-56-78901")
                .name("테슬라 충전소 철산역점")
                .category(EcoMerchant.MerchantCategory.EV_CHARGING)
                .description("테슬라 수퍼차저와 일반 전기차 충전이 가능한 친환경 충전소입니다.")
                .address("서울특별시 광진구 아차산로 456")
                .latitude(new BigDecimal("37.5555"))
                .longitude(new BigDecimal("126.8370"))
                .phoneNumber("02-2345-6789")
                .websiteUrl("https://tesla.com")
                .businessHours("24시간")
                .holiday("연중무휴")
                .ecoCertifications("[\"친환경에너지인증\", \"재생에너지인증\"]")
                .ecoPractices("[\"태양광 발전\", \"재생에너지 사용\", \"탄소중립 운영\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 3. 재활용/제로웨이스트
            EcoMerchant.builder()
                .businessNumber("345-67-89012")
                .name("제로웨이스트샵 철산")
                .category(EcoMerchant.MerchantCategory.RECYCLING_STORE)
                .description("리필 용기, 친환경 생활용품, 제로웨이스트 제품을 판매합니다.")
                .address("서울특별시 광진구 아차산로 789")
                .latitude(new BigDecimal("37.5548"))
                .longitude(new BigDecimal("126.8365"))
                .phoneNumber("02-3456-7890")
                .websiteUrl("https://zerowaste-shop.co.kr")
                .businessHours("10:00-20:00")
                .holiday("매주 월요일")
                .ecoCertifications("[\"제로웨이스트인증\", \"친환경제품인증\"]")
                .ecoPractices("[\"리필 용기 제공\", \"비닐 포장재 사용 금지\", \"재활용 포장재 사용\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 4. 친환경 뷰티
            EcoMerchant.builder()
                .businessNumber("456-78-90123")
                .name("네이처뷰티 철산점")
                .category(EcoMerchant.MerchantCategory.GREEN_BEAUTY)
                .description("천연 성분, 동물실험 없는 친환경 화장품을 판매합니다.")
                .address("서울특별시 광진구 아차산로 101")
                .latitude(new BigDecimal("37.5553"))
                .longitude(new BigDecimal("126.8362"))
                .phoneNumber("02-4567-8901")
                .websiteUrl("https://naturebeauty.co.kr")
                .businessHours("10:00-21:00")
                .holiday("매주 화요일")
                .ecoCertifications("[\"천연화장품인증\", \"동물실험금지인증\"]")
                .ecoPractices("[\"천연 성분 사용\", \"동물실험 금지\", \"친환경 포장재 사용\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 5. 친환경 쇼핑
            EcoMerchant.builder()
                .businessNumber("567-89-01234")
                .name("에코스토어 철산")
                .category(EcoMerchant.MerchantCategory.ECO_SHOPPING)
                .description("친환경 의류, 생활용품, 가전제품을 판매하는 종합 친환경 스토어입니다.")
                .address("서울특별시 광진구 아차산로 202")
                .latitude(new BigDecimal("37.5549"))
                .longitude(new BigDecimal("126.8369"))
                .phoneNumber("02-5678-9012")
                .websiteUrl("https://ecostore.co.kr")
                .businessHours("09:00-21:00")
                .holiday("매주 수요일")
                .ecoCertifications("[\"친환경제품인증\", \"공정무역인증\"]")
                .ecoPractices("[\"공정무역 제품 판매\", \"친환경 소재 사용\", \"재활용 포장재 사용\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 6. 유기농 카페
            EcoMerchant.builder()
                .businessNumber("678-90-12345")
                .name("오가닉카페 철산역점")
                .category(EcoMerchant.MerchantCategory.ORGANIC_CAFE)
                .description("유기농 원두, 친환경 디저트를 제공하는 카페입니다.")
                .address("서울특별시 광진구 아차산로 303")
                .latitude(new BigDecimal("37.5550"))
                .longitude(new BigDecimal("126.8367"))
                .phoneNumber("02-6789-0123")
                .websiteUrl("https://organiccafe.co.kr")
                .businessHours("07:00-22:00")
                .holiday("매주 목요일")
                .ecoCertifications("[\"유기농인증\", \"친환경농산물인증\"]")
                .ecoPractices("[\"유기농 원두 사용\", \"친환경 디저트 제공\", \"재사용 컵 할인\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 7. 친환경 식품/매장 (추가)
            EcoMerchant.builder()
                .businessNumber("789-01-23456")
                .name("헬시푸드 철산점")
                .category(EcoMerchant.MerchantCategory.ECO_FOOD)
                .description("건강한 유기농 식품과 친환경 건강식품을 판매합니다.")
                .address("서울특별시 광진구 아차산로 404")
                .latitude(new BigDecimal("37.5552"))
                .longitude(new BigDecimal("126.8364"))
                .phoneNumber("02-7890-1234")
                .websiteUrl("https://healthyfood.co.kr")
                .businessHours("09:00-21:00")
                .holiday("매주 금요일")
                .ecoCertifications("[\"유기농인증\", \"건강식품인증\"]")
                .ecoPractices("[\"무농약 농산물 판매\", \"친환경 포장재 사용\", \"지역 농산물 우선 판매\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 8. 전기차 충전소 (추가)
            EcoMerchant.builder()
                .businessNumber("890-12-34567")
                .name("EV플러스 충전소 철산")
                .category(EcoMerchant.MerchantCategory.EV_CHARGING)
                .description("빠른 충전이 가능한 전기차 충전소입니다.")
                .address("서울특별시 광진구 아차산로 505")
                .latitude(new BigDecimal("37.5547"))
                .longitude(new BigDecimal("126.8366"))
                .phoneNumber("02-8901-2345")
                .websiteUrl("https://evplus.co.kr")
                .businessHours("24시간")
                .holiday("연중무휴")
                .ecoCertifications("[\"친환경에너지인증\"]")
                .ecoPractices("[\"재생에너지 사용\", \"탄소중립 운영\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 9. 재활용/제로웨이스트 (추가)
            EcoMerchant.builder()
                .businessNumber("901-23-45678")
                .name("리사이클샵 철산")
                .category(EcoMerchant.MerchantCategory.RECYCLING_STORE)
                .description("재활용품 수집, 분리수거 교육, 친환경 제품 판매를 합니다.")
                .address("서울특별시 광진구 아차산로 606")
                .latitude(new BigDecimal("37.5554"))
                .longitude(new BigDecimal("126.8363"))
                .phoneNumber("02-9012-3456")
                .websiteUrl("https://recycleshop.co.kr")
                .businessHours("09:00-18:00")
                .holiday("매주 토요일")
                .ecoCertifications("[\"재활용인증\", \"친환경제품인증\"]")
                .ecoPractices("[\"재활용품 수집\", \"분리수거 교육\", \"친환경 제품 판매\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 10. 친환경 뷰티 (추가)
            EcoMerchant.builder()
                .businessNumber("012-34-56789")
                .name("그린스킨 철산점")
                .category(EcoMerchant.MerchantCategory.GREEN_BEAUTY)
                .description("천연 성분 기반의 친환경 스킨케어 제품을 판매합니다.")
                .address("서울특별시 광진구 아차산로 707")
                .latitude(new BigDecimal("37.5546"))
                .longitude(new BigDecimal("126.8368"))
                .phoneNumber("02-0123-4567")
                .websiteUrl("https://greenskin.co.kr")
                .businessHours("10:00-20:00")
                .holiday("매주 일요일")
                .ecoCertifications("[\"천연화장품인증\", \"친환경제품인증\"]")
                .ecoPractices("[\"천연 성분 사용\", \"친환경 포장재 사용\", \"동물실험 금지\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 11. 친환경 쇼핑 (추가)
            EcoMerchant.builder()
                .businessNumber("123-45-67891")
                .name("에코패션 철산")
                .category(EcoMerchant.MerchantCategory.ECO_SHOPPING)
                .description("친환경 소재 의류와 액세서리를 판매합니다.")
                .address("서울특별시 광진구 아차산로 808")
                .latitude(new BigDecimal("37.5555"))
                .longitude(new BigDecimal("126.8365"))
                .phoneNumber("02-1234-5679")
                .websiteUrl("https://ecofashion.co.kr")
                .businessHours("10:00-21:00")
                .holiday("매주 월요일")
                .ecoCertifications("[\"친환경의류인증\", \"공정무역인증\"]")
                .ecoPractices("[\"친환경 소재 사용\", \"공정무역 제품 판매\", \"재활용 포장재 사용\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 12. 유기농 카페 (추가)
            EcoMerchant.builder()
                .businessNumber("234-56-78902")
                .name("네이처브루 철산점")
                .category(EcoMerchant.MerchantCategory.ORGANIC_CAFE)
                .description("유기농 원두와 친환경 디저트를 제공하는 브런치 카페입니다.")
                .address("서울특별시 광진구 아차산로 909")
                .latitude(new BigDecimal("37.5548"))
                .longitude(new BigDecimal("126.8367"))
                .phoneNumber("02-2345-6790")
                .websiteUrl("https://naturebrew.co.kr")
                .businessHours("08:00-21:00")
                .holiday("매주 화요일")
                .ecoCertifications("[\"유기농인증\", \"친환경농산물인증\"]")
                .ecoPractices("[\"유기농 원두 사용\", \"친환경 디저트 제공\", \"재사용 컵 할인\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 13. 친환경 식품/매장 (추가)
            EcoMerchant.builder()
                .businessNumber("345-67-89013")
                .name("오가닉마켓 철산")
                .category(EcoMerchant.MerchantCategory.ECO_FOOD)
                .description("유기농 채소, 친환경 육류, 무농약 과일을 판매하는 마켓입니다.")
                .address("서울특별시 광진구 아차산로 1010")
                .latitude(new BigDecimal("37.5551"))
                .longitude(new BigDecimal("126.8369"))
                .phoneNumber("02-3456-7901")
                .websiteUrl("https://organicmarket.co.kr")
                .businessHours("08:00-22:00")
                .holiday("매주 수요일")
                .ecoCertifications("[\"유기농인증\", \"친환경농산물인증\"]")
                .ecoPractices("[\"비닐봉지 사용 최소화\", \"재활용 포장재 사용\", \"지역 농산물 우선 판매\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 14. 전기차 충전소 (추가)
            EcoMerchant.builder()
                .businessNumber("456-78-90124")
                .name("그린차지 철산역점")
                .category(EcoMerchant.MerchantCategory.EV_CHARGING)
                .description("빠른 충전과 편리한 이용이 가능한 전기차 충전소입니다.")
                .address("서울특별시 광진구 아차산로 1111")
                .latitude(new BigDecimal("37.5553"))
                .longitude(new BigDecimal("126.8364"))
                .phoneNumber("02-4567-8012")
                .websiteUrl("https://greencharge.co.kr")
                .businessHours("24시간")
                .holiday("연중무휴")
                .ecoCertifications("[\"친환경에너지인증\", \"재생에너지인증\"]")
                .ecoPractices("[\"태양광 발전\", \"재생에너지 사용\", \"탄소중립 운영\"]")
                .isActive(true)
                .isVerified(true)
                .build(),

            // 15. 재활용/제로웨이스트 (추가)
            EcoMerchant.builder()
                .businessNumber("567-89-01235")
                .name("에코리사이클 철산")
                .category(EcoMerchant.MerchantCategory.RECYCLING_STORE)
                .description("재활용품 수집, 분리수거 교육, 친환경 제품 판매를 합니다.")
                .address("서울특별시 광진구 아차산로 1212")
                .latitude(new BigDecimal("37.5549"))
                .longitude(new BigDecimal("126.8366"))
                .phoneNumber("02-5678-9123")
                .websiteUrl("https://ecorecycle.co.kr")
                .businessHours("09:00-18:00")
                .holiday("매주 목요일")
                .ecoCertifications("[\"재활용인증\", \"친환경제품인증\"]")
                .ecoPractices("[\"재활용품 수집\", \"분리수거 교육\", \"친환경 제품 판매\"]")
                .isActive(true)
                .isVerified(true)
                .build()
        );

        ecoMerchantRepository.saveAll(cheolsanMerchants);
        log.info("철산역 근처 친환경 가맹점 {}개 데이터 초기화 완료!", cheolsanMerchants.size());
    }
}
