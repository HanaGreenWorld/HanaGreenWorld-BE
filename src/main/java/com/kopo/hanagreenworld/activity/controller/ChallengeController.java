package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.domain.Challenge;
import com.kopo.hanagreenworld.activity.domain.ChallengeRecord;
import com.kopo.hanagreenworld.activity.dto.ChallengeListResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeDetailResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationRequest;
import com.kopo.hanagreenworld.activity.dto.ChallengeParticipationResponse;
import com.kopo.hanagreenworld.activity.dto.ChallengeRecordResponse;
import com.kopo.hanagreenworld.activity.service.ChallengeService;
import com.kopo.hanagreenworld.common.dto.ApiResponse;
import com.kopo.hanagreenworld.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Challenge Controller", description = "에코챌린지 관련 API")
@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
@Slf4j
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "에코챌린지 목록 조회", description = "활성화된 모든 에코챌린지를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeListResponse>>> getActiveChallenges() {
        List<ChallengeListResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(ApiResponse.success("에코챌린지 목록을 조회했습니다.", challenges));
    }

    @Operation(summary = "에코챌린지 상세 조회", description = "특정 에코챌린지의 상세 정보와 사용자 참여 상태를 조회합니다.")
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<ChallengeDetailResponse>> getChallengeDetail(@PathVariable Long challengeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            memberId = 1L;
        }
        
        ChallengeDetailResponse challengeDetail = challengeService.getChallengeDetail(challengeId, memberId);
        return ResponseEntity.ok(ApiResponse.success("에코챌린지 상세 정보를 조회했습니다.", challengeDetail));
    }

    @Operation(summary = "에코챌린지 참여", description = "에코챌린지에 참여하고 인증 정보를 제출합니다.")
    @PostMapping("/{challengeId}/participate")
    public ResponseEntity<ApiResponse<ChallengeParticipationResponse>> participateInChallenge(
            @PathVariable Long challengeId,
            @RequestBody ChallengeParticipationRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChallengeParticipationResponse response = challengeService.participateInChallenge(memberId, challengeId, request);
        return ResponseEntity.ok(ApiResponse.success("에코챌린지 참여가 완료되었습니다.", response));
    }

    @Operation(summary = "사용자 챌린지 참여 이력 조회", description = "현재 사용자의 챌린지 참여 이력을 조회합니다.")
    @GetMapping("/my-participations")
    public ResponseEntity<ApiResponse<List<ChallengeRecordResponse>>> getMyChallengeParticipations() {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            
            if (memberId == null) {
                memberId = 1L; // 임시로 하드코딩 (개발/테스트용)
            }
            
            List<ChallengeRecord> participations = challengeService.getMemberChallengeParticipations(memberId);
            
            List<ChallengeRecordResponse> responseList = participations.stream()
                    .map(ChallengeRecordResponse::from)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success("챌린지 참여 이력을 조회했습니다.", responseList));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("챌린지 참여 이력 조회 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "특정 챌린지 참여 상태 조회", description = "현재 사용자의 특정 챌린지 참여 상태를 조회합니다.")
    @GetMapping("/{challengeId}/participation-status")
    public ResponseEntity<ApiResponse<ChallengeRecordResponse>> getChallengeParticipationStatus(@PathVariable Long challengeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChallengeRecord participation = challengeService.getMemberChallengeParticipation(memberId, challengeId);
        ChallengeRecordResponse response = ChallengeRecordResponse.from(participation);
        return ResponseEntity.ok(ApiResponse.success("챌린지 참여 상태를 조회했습니다.", response));
    }

    @Operation(summary = "챌린지 활동 내역 저장", description = "이미지와 함께 챌린지 활동 내역을 저장합니다.")
    @PostMapping("/{challengeId}/activity")
    public ResponseEntity<ApiResponse<ChallengeRecordResponse>> saveChallengeActivity(
            @PathVariable Long challengeId,
            @RequestBody Map<String, Object> request) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

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
            ChallengeParticipationRequest participationRequest = ChallengeParticipationRequest.builder()
                    .imageUrl(imageUrl)
                    .build();

            ChallengeParticipationResponse response = challengeService.participateInChallenge(memberId, challengeId, participationRequest);

            ChallengeRecord record = challengeService.getMemberChallengeParticipation(memberId, challengeId);
            ChallengeRecordResponse recordResponse = ChallengeRecordResponse.from(record);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), recordResponse));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.error("챌린지 활동 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "AI 검증 시작", description = "사용자가 인증 완료 버튼을 눌렀을 때 AI 검증을 시작합니다.")
    @PostMapping("/{challengeId}/verify")
    public ResponseEntity<ApiResponse<ChallengeParticipationResponse>> startAiVerification(
            @PathVariable Long challengeId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();

            ChallengeRecord record = challengeService.getMemberChallengeParticipation(memberId, challengeId);
            if (record == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("챌린지 참여 기록을 찾을 수 없습니다."));
            }

            // AI 검증 시작
            ChallengeParticipationResponse response = challengeService.startAiVerification(memberId, record.getId());
            
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.error("AI 검증 시작 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "[관리자] 검토 필요 챌린지 목록 조회", description = "AI 검증에서 애매한 케이스로 분류된 챌린지 목록을 조회합니다.")
    @GetMapping("/admin/needs-review")
    public ResponseEntity<ApiResponse<List<ChallengeRecordResponse>>> getNeedsReviewChallenges() {
        List<ChallengeRecord> records = challengeService.getNeedsReviewChallenges();
        List<ChallengeRecordResponse> responseList = records.stream()
                .map(ChallengeRecordResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("검토 필요한 챌린지 목록을 조회했습니다.", responseList));
    }

    @Operation(summary = "[관리자] 챌린지 수동 승인", description = "검토 필요 상태의 챌린지를 수동으로 승인합니다.")
    @PostMapping("/admin/{recordId}/approve")
    public ResponseEntity<ApiResponse<Void>> adminApproveChallenge(@PathVariable Long recordId) {
        challengeService.adminApproveChallenge(recordId);
        return ResponseEntity.ok(ApiResponse.success("챌린지가 승인되었습니다.", null));
    }

    @Operation(summary = "[관리자] 챌린지 수동 거부", description = "검토 필요 상태의 챌린지를 수동으로 거부합니다.")
    @PostMapping("/admin/{recordId}/reject")
    public ResponseEntity<ApiResponse<Void>> adminRejectChallenge(
            @PathVariable Long recordId,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "관리자에 의해 거부되었습니다.");
        challengeService.adminRejectChallenge(recordId, reason);
        return ResponseEntity.ok(ApiResponse.success("챌린지가 거부되었습니다.", null));
    }

    @Operation(summary = "팀별 챌린지 참여 상태 조회", description = "특정 팀의 챌린지 참여 상태를 조회합니다.")
    @GetMapping("/team/{teamId}/participations")
    public ResponseEntity<ApiResponse<List<ChallengeRecordResponse>>> getTeamChallengeParticipations(@PathVariable Long teamId) {
        List<ChallengeRecord> participations = challengeService.getTeamChallengeParticipations(teamId);
        List<ChallengeRecordResponse> responseList = participations.stream()
                .map(ChallengeRecordResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("팀 챌린지 참여 상태를 조회했습니다.", responseList));
    }

    @Operation(summary = "이미지 해시 통계 조회", description = "현재 사용자의 이미지 해시 통계를 조회합니다.")
    @GetMapping("/image-hash-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getImageHashStats() {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            if (memberId == null) {
                memberId = 1L; // 임시로 하드코딩 (개발/테스트용)
            }
            
            var stats = challengeService.getImageHashStats(memberId);
            return ResponseEntity.ok(ApiResponse.success("이미지 해시 통계를 조회했습니다.", stats));
        } catch (Exception e) {
            log.error("이미지 해시 통계 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("이미지 해시 통계 조회 중 오류가 발생했습니다."));
        }
    }
}