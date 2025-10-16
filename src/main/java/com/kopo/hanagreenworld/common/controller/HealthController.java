package com.kopo.hanagreenworld.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Application Health Good!";
    }

    @GetMapping("/")
    public ResponseEntity<String> ok() { return ResponseEntity.ok("ok"); }
}