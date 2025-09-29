package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.dto.ChallengeListResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationRequest;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeRecordResponse;
import com.kopo.hanagreenworld.activity.service.ChallengeService;
import com.kopo.hanagreenworld.common.response.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Challenge Controller", description = "에코챌린지 관련 API")
@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "에코챌린지 목록 조회", description = "활성화된 모든 에코챌린지를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeListResponse>>> getActiveChallenges() {
        List<ChallengeListResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(ApiResponse.success(challenges, "에코챌린지 목록을 조회했습니다."));
    }

    @Operation(summary = "에코챌린지 상세 조회", description = "특정 에코챌린지의 상세 정보를 조회합니다.")
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Challenge>> getChallengeDetail(@PathVariable Long challengeId) {
        Challenge challenge = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(ApiResponse.success(challenge, "에코챌린지 상세 정보를 조회했습니다."));
    }

    @Operation(summary = "에코챌린지 참여", description = "에코챌린지에 참여하고 인증 정보를 제출합니다.")
    @PostMapping("/{challengeId}/participate")
    public ResponseEntity<ApiResponse<ChallengeParticipationResponse>> participateInChallenge(
            @PathVariable Long challengeId,
            @RequestBody ChallengeParticipationRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChallengeParticipationResponse response = challengeService.participateInChallenge(memberId, challengeId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "에코챌린지 참여가 완료되었습니다."));
    }

    @Operation(summary = "사용자 챌린지 참여 이력 조회", description = "현재 사용자의 챌린지 참여 이력을 조회합니다.")
    @GetMapping("/my-participations")
    public ResponseEntity<ApiResponse<List<ChallengeRecordResponse>>> getMyChallengeParticipations() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<ChallengeRecord> participations = challengeService.getMemberChallengeParticipations(memberId);
        List<ChallengeRecordResponse> responseList = participations.stream()
                .map(ChallengeRecordResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responseList, "챌린지 참여 이력을 조회했습니다."));
    }

    @Operation(summary = "특정 챌린지 참여 상태 조회", description = "현재 사용자의 특정 챌린지 참여 상태를 조회합니다.")
    @GetMapping("/{challengeId}/participation-status")
    public ResponseEntity<ApiResponse<ChallengeRecordResponse>> getChallengeParticipationStatus(@PathVariable Long challengeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChallengeRecord participation = challengeService.getMemberChallengeParticipation(memberId, challengeId);
        ChallengeRecordResponse response = ChallengeRecordResponse.from(participation);
        return ResponseEntity.ok(ApiResponse.success(response, "챌린지 참여 상태를 조회했습니다."));
    }

    @Operation(summary = "챌린지 활동 내역 저장", description = "이미지와 함께 챌린지 활동 내역을 저장합니다.")
    @PostMapping("/{challengeId}/activity")
    public ResponseEntity<ApiResponse<ChallengeRecordResponse>> saveChallengeActivity(
            @PathVariable Long challengeId,
            @RequestBody Map<String, Object> request) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            
            // 요청 데이터 추출 및 안전한 캐스팅
            String imageUrl = (String) request.get("imageUrl");
            String activityDate = (String) request.get("activityDate");
            String challengeTitle = (String) request.get("challengeTitle");
            Object pointsObj = request.get("points");
            Integer points = pointsObj != null ? ((Number) pointsObj).intValue() : null;
            String challengeType = (String) request.get("challengeType");
            
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이미지 URL이 필요합니다."));
            }
            
            // ChallengeParticipationRequest 생성 (Builder 패턴 사용)
            ChallengeParticipationRequest participationRequest = ChallengeParticipationRequest.builder()
                    .imageUrl(imageUrl)
                    .build();
            
            // 챌린지 참여 처리
            ChallengeParticipationResponse response = challengeService.participateInChallenge(memberId, challengeId, participationRequest);
            
            // ChallengeRecord 조회하여 반환
            ChallengeRecord record = challengeService.getMemberChallengeParticipation(memberId, challengeId);
            ChallengeRecordResponse recordResponse = ChallengeRecordResponse.from(record);
            return ResponseEntity.ok(ApiResponse.success(recordResponse, "챌린지 활동 내역이 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.error("챌린지 활동 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}