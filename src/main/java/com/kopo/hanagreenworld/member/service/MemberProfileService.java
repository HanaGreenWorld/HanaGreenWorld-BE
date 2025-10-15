package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void updateMemberCarbonSaved(Long memberId, Double carbonSaved) {
        if (carbonSaved != null && carbonSaved > 0) {
            try {
                // MemberProfile 조회 및 업데이트
                Optional<MemberProfile> profileOpt = memberProfileRepository.findByMember_MemberId(memberId);
                if (profileOpt.isPresent()) {
                    MemberProfile profile = profileOpt.get();
                    
                    // 탄소절약량 업데이트 (총 탄소절약량과 이번달 탄소절약량 모두 증가)
                    profile.updateCarbonSaved(carbonSaved);
                    
                    // 활동 횟수 증가
                    profile.incrementActivityCount();
                    
                    // 프로필 저장
                    memberProfileRepository.save(profile);
                    
                    log.info("탄소절약량 업데이트 완료: memberId={}, totalCarbonSaved={}, currentMonthCarbonSaved={}", 
                        memberId, profile.getTotalCarbonSaved(), profile.getCurrentMonthCarbonSaved());
                } else {
                    log.warn("MemberProfile을 찾을 수 없습니다: memberId={}", memberId);
                }
            } catch (Exception e) {
                log.error("탄소절약량 업데이트 실패: memberId={}, carbonSaved={}, error={}", 
                    memberId, carbonSaved, e.getMessage(), e);
            }
        }
    }

    @Transactional
    public MemberProfile getOrCreateMemberProfile(Long memberId) {
        log.info("getOrCreateMemberProfile 호출 - memberId: {}", memberId);
        
        if (memberId == null) {
            log.error("memberId가 null입니다!");
            throw new IllegalArgumentException("memberId는 null일 수 없습니다.");
        }
        
        return memberProfileRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> {
                    log.info("MemberProfile이 없음 - 새로 생성 시작");
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> {
                                log.error("Member를 찾을 수 없습니다 - memberId: {}", memberId);
                                return new IllegalArgumentException("Member를 찾을 수 없습니다: " + memberId);
                            });
                    
                    MemberProfile profile = MemberProfile.builder()
                            .member(member)
                            .nickname(member.getName())
                            .build();
                    
                    return memberProfileRepository.save(profile);
                });
    }

    @Transactional(readOnly = true)
    public Optional<MemberProfile> getMemberProfile(Long memberId) {
        return memberProfileRepository.findByMember_MemberId(memberId);
    }

    @Transactional
    public void updateMemberPoints(Long memberId, Long points) {
        if (points != null && points > 0) {
            try {
                MemberProfile profile = getOrCreateMemberProfile(memberId);
                profile.updateCurrentPoints(points);
                memberProfileRepository.save(profile);
                
                log.info("포인트 업데이트 완료: memberId={}, addedPoints={}, totalPoints={}", 
                    memberId, points, profile.getCurrentPoints());
            } catch (Exception e) {
                log.error("포인트 업데이트 실패: memberId={}, points={}, error={}", 
                    memberId, points, e.getMessage(), e);
            }
        }
    }


    @Transactional
    public void resetCurrentMonthData(Long memberId) {
        try {
            Optional<MemberProfile> profileOpt = memberProfileRepository.findByMember_MemberId(memberId);
            if (profileOpt.isPresent()) {
                MemberProfile profile = profileOpt.get();
                profile.resetCurrentMonthData();
                memberProfileRepository.save(profile);
                
                log.info("이번달 데이터 초기화 완료: memberId={}", memberId);
            }
        } catch (Exception e) {
            log.error("이번달 데이터 초기화 실패: memberId={}, error={}", memberId, e.getMessage(), e);
        }
    }
}
