package com.kopo.hanagreenworld.member.service;

import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final MemberProfileRepository memberProfileRepository;
    private final ObjectMapper objectMapper;

    private String getRankingForUser(String rankingJson, Long memberId, Long totalPoints) {
        try {
            Map<String, Object> rankingData = objectMapper.readValue(rankingJson, Map.class);
            long totalUsers = ((Number) rankingData.get("totalUsers")).longValue();
            long averagePoints = ((Number) rankingData.get("averagePoints")).longValue();
            long maxPoints = ((Number) rankingData.get("maxPoints")).longValue();

            int percentile;
            long rank;
            
            if (totalPoints >= maxPoints) {
                percentile = 1; // 상위 1%
                rank = 1;
            } else if (totalPoints >= averagePoints * 1.5) {
                percentile = 10; // 상위 10%
                rank = totalUsers / 10;
            } else if (totalPoints >= averagePoints) {
                percentile = 25; // 상위 25%
                rank = totalUsers / 4;
            } else if (totalPoints >= averagePoints * 0.5) {
                percentile = 50; // 상위 50%
                rank = totalUsers / 2;
            } else {
                percentile = 75; // 상위 75%
                rank = totalUsers * 3 / 4;
            }
            
            Map<String, Object> userRanking = new HashMap<>();
            userRanking.put("percentile", percentile);
            userRanking.put("totalUsers", totalUsers);
            userRanking.put("rank", rank);
            userRanking.put("userPoints", totalPoints);
            userRanking.put("averagePoints", averagePoints);
            
            String result = objectMapper.writeValueAsString(userRanking);
            
            return result;
            
        } catch (Exception e) {
            return getDefaultRanking();
        }
    }

    private String getDefaultRanking() {
        try {
            Map<String, Object> defaultRanking = new HashMap<>();
            defaultRanking.put("percentile", 50);
            defaultRanking.put("totalUsers", 1000);
            defaultRanking.put("rank", 500);
            defaultRanking.put("userPoints", 0);
            defaultRanking.put("averagePoints", 500);
            
            return objectMapper.writeValueAsString(defaultRanking);
        } catch (Exception e) {
            return "{\"percentile\":50,\"totalUsers\":1000,\"rank\":500,\"userPoints\":0,\"averagePoints\":500}";
        }
    }
}

