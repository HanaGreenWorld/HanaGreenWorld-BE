package com.kopo.hanagreenworld.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.time.Duration;
import java.util.List;

/**
 * 하나금융그룹 통합 연동 설정
 */
@Configuration
public class IntegrationConfig {

    /**
     * 관계사 연동용 RestTemplate 설정
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .additionalInterceptors(groupAuthInterceptor())
                .build();
    }

    /**
     * 그룹 내부 인증 인터셉터
     */
    @Bean
    public ClientHttpRequestInterceptor groupAuthInterceptor() {
        return (request, body, execution) -> {
            // 그룹 내부 서비스 식별 헤더 추가
            request.getHeaders().add("X-Group-Service", "HANA_GREEN_WORLD");
            request.getHeaders().add("X-API-Version", "1.0");
            request.getHeaders().add("X-Request-ID", java.util.UUID.randomUUID().toString());
            
            return execution.execute(request, body);
        };
    }
}





