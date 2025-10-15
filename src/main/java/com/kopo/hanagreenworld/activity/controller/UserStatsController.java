package com.kopo.hanagreenworld.activity.controller;

import com.kopo.hanagreenworld.activity.dto.UserStatsResponse;
import com.kopo.hanagreenworld.activity.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    @GetMapping("/{memberId}")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long memberId) {
        try {
            UserStatsResponse stats = userStatsService.getUserStats(memberId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{memberId}/registration-date")
    public ResponseEntity<String> getRegistrationDate(@PathVariable Long memberId) {
        try {
            String registrationDate = userStatsService.getRegistrationDate(memberId);
            return ResponseEntity.ok(registrationDate);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
