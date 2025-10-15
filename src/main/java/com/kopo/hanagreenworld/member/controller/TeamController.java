package com.kopo.hanagreenworld.member.controller;

import com.kopo.hanagreenworld.member.dto.*;
import com.kopo.hanagreenworld.member.service.TeamService;
import com.kopo.hanagreenworld.common.exception.BusinessException;
import com.kopo.hanagreenworld.common.exception.ErrorCode;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "팀 API", description = "팀 관련 API (팀 정보, 랭킹, 가입/탈퇴)")
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/my-team")
    @Operation(summary = "내 팀 정보 조회", description = "현재 사용자가 속한 팀의 상세 정보를 조회합니다.")
    public ResponseEntity<?> getMyTeam() {
        try {
            TeamResponse response = teamService.getMyTeam();
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TEAM_NOT_FOUND) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "TEAM_NOT_FOUND",
                    "message", "팀에 속하지 않습니다. 팀에 가입해주세요.",
                    "code", "TM_001"
                ));
            }
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/ranking")
    @Operation(summary = "팀 랭킹 조회", description = "전체 팀 랭킹과 내 팀의 순위를 조회합니다.")
    public ResponseEntity<?> getTeamRanking() {
        log.info("팀 랭킹 조회 요청");
        try {
            TeamRankingResponse response = teamService.getTeamRanking();
            
            // 팀이 없을 때
            if (response.getMyTeam() != null && response.getMyTeam().getTeamId() == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("data", response);
                result.put("message", "팀을 생성하거나 가입해서 함께 환경보호를 시작해보세요!");
                result.put("hasTeam", false);
                result.put("suggestion", "친구들과 함께 팀을 만들어 더 많은 포인트를 획득해보세요!");
                return ResponseEntity.ok(result);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/{teamId}/stats")
    @Operation(summary = "팀 통계 조회", description = "특정 팀의 상세 통계 정보를 조회합니다.")
    public ResponseEntity<TeamResponse.TeamStatsResponse> getTeamStats(@PathVariable Long teamId) {
        try {
            TeamResponse.TeamStatsResponse response = teamService.getTeamStats(teamId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }


    @DeleteMapping("/{teamId}/leave")
    @Operation(summary = "팀 탈퇴", description = "현재 팀에서 탈퇴합니다.")
    public ResponseEntity<Void> leaveTeam(@PathVariable Long teamId) {
        try {
            teamService.leaveTeam(teamId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/{teamId}/invite-code")
    @Operation(summary = "팀 초대 코드 생성", description = "새로운 팀 초대 코드를 생성합니다.")
    public ResponseEntity<TeamInviteCodeResponse> generateInviteCode(@PathVariable Long teamId) {
        try {
            String inviteCode = "GG-" + teamId.toString().substring(0, 4).toUpperCase();
            TeamInviteCodeResponse response = new TeamInviteCodeResponse(inviteCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/validate-invite-code")
    @Operation(summary = "초대코드 검증", description = "초대코드가 유효한지 검증하고 팀 정보를 반환합니다.")
    public ResponseEntity<?> validateInviteCode(@Valid @RequestBody TeamJoinRequest request) {
        try {
            TeamResponse response = teamService.validateInviteCode(request.getInviteCode());
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INVALID_INVITE_CODE) {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "INVALID_INVITE_CODE",
                    "message", "유효하지 않은 초대코드입니다.",
                    "code", "TM_004"
                ));
            }
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/list")
    @Operation(summary = "팀 목록 조회", description = "가입 가능한 팀 목록을 조회합니다.")
    public ResponseEntity<?> getTeamList() {
        try {
            var response = teamService.getTeamList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/create")
    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다.")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        try {
            TeamResponse response = teamService.createTeam(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }


    @GetMapping("/{teamId}/members")
    @Operation(summary = "팀 멤버 목록 조회", description = "특정 팀의 멤버 목록을 조회합니다.")
    public ResponseEntity<TeamMembersResponse> getTeamMembers(@PathVariable Long teamId) {
        try {
            TeamMembersResponse response = teamService.getTeamMembers(teamId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    
    @PostMapping("/request-join")
    @Operation(summary = "팀 가입 신청", description = "초대코드를 사용하여 팀 가입을 신청합니다.")
    public ResponseEntity<?> requestJoinTeam(@Valid @RequestBody TeamJoinRequest request) {
        try {
            teamService.requestJoinTeam(request);
            return ResponseEntity.ok(Map.of(
                "message", "팀 가입 신청이 완료되었습니다. 방장의 승인을 기다려주세요.",
                "status", "success"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
    
    @GetMapping("/{teamId}/join-requests")
    @Operation(summary = "팀 가입 신청 목록 조회", description = "팀의 가입 신청 목록을 조회합니다. (방장만 가능)")
    public ResponseEntity<?> getJoinRequests(@PathVariable Long teamId) {
        try {
            var response = teamService.getJoinRequests(teamId);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
    
    @PostMapping("/join-requests/{requestId}")
    @Operation(summary = "가입 신청 승인/거절", description = "가입 신청을 승인하거나 거절합니다. (방장만 가능)")
    public ResponseEntity<?> handleJoinRequest(@PathVariable Long requestId, @RequestBody Map<String, Boolean> body) {
        boolean approve = body.getOrDefault("approve", false);
        try {
            teamService.handleJoinRequest(requestId, approve);
            String action = approve ? "승인" : "거절";
            return ResponseEntity.ok(Map.of(
                "message", "가입 신청을 " + action + "했습니다.",
                "status", "success"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
    
    @GetMapping("/my-join-requests")
    @Operation(summary = "내 가입 신청 내역 조회", description = "현재 사용자가 보낸 팀 가입 신청 내역을 조회합니다.")
    public ResponseEntity<?> getMyJoinRequests() {
        log.info("내 가입 신청 내역 조회 요청");
        try {
            List<MyJoinRequestResponse> requests = teamService.getMyJoinRequests();
            log.info("내 가입 신청 내역 조회 성공: {} 건", requests.size());
            return ResponseEntity.ok(requests);
        } catch (BusinessException e) {
            log.error("내 가입 신청 내역 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("내 가입 신청 내역 조회 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/{teamId}/kick")
    @Operation(summary = "팀원 강퇴", description = "팀원을 강퇴합니다. (방장만 가능)")
    public ResponseEntity<?> kickMember(@PathVariable Long teamId, @RequestBody Map<String, Long> body) {
        Long memberId = body.get("memberId");
        try {
            teamService.kickMember(teamId, memberId);
            return ResponseEntity.ok(Map.of(
                "message", "팀원을 강퇴했습니다.",
                "status", "success"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
    
    @PostMapping("/leave")
    @Operation(summary = "팀 탈퇴", description = "현재 팀에서 탈퇴합니다.")
    public ResponseEntity<?> leaveTeam() {
        log.info("팀 탈퇴 요청");
        try {
            teamService.leaveCurrentTeam();
            return ResponseEntity.ok(Map.of(
                "message", "팀을 탈퇴했습니다.",
                "status", "success"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
    
    @PostMapping("/{teamId}/transfer-leadership")
    @Operation(summary = "방장 권한 이양", description = "방장 권한을 다른 멤버에게 이양합니다. (방장만 가능)")
    public ResponseEntity<?> transferLeadership(@PathVariable Long teamId, @RequestBody Map<String, Long> body) {
        Long newLeaderId = body.get("newLeaderId");
        try {
            teamService.transferLeadership(teamId, newLeaderId);
            return ResponseEntity.ok(Map.of(
                "message", "방장 권한을 이양했습니다.",
                "status", "success"
            ));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getErrorCode().name(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            throw e;
        }
    }
}

