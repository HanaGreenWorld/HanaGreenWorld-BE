package com.kopo.hanagreenworld.common.config;

import com.kopo.hanagreenworld.common.filter.JwtAuthenticationFilter;
import com.kopo.hanagreenworld.common.filter.DevAutoLoginFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired(required = false)
    private DevAutoLoginFilter devAutoLoginFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll() // Health check 허용
                .requestMatchers("/challenges/**", "/merchants/**").permitAll() // 챌린지와 가맹점은 인증 없이 접근
                .requestMatchers("/admin/**").permitAll() // 관리자 API는 인증 없이 접근 허용
                .requestMatchers("/ws/**", "/stomp/**").permitAll() // WebSocket 엔드포인트 허용
                .requestMatchers("/api/v1/integration/**").permitAll() // 통합 API 허용 (내부 서비스 간 통신)
                .requestMatchers("/quiz/**", "/eco-seeds/**", "/walking/**").permitAll() // 개발환경에서는 모든 API 허용
                .anyRequest().permitAll() // 개발환경에서는 모든 요청 허용
            )
            .addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        // 개발 환경에서만 자동 로그인 필터 추가
        if (devAutoLoginFilter != null) {
            http.addFilterBefore(devAutoLoginFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 모든 도메인 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 요청 로깅 필터
    public static class RequestLoggingFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
                throws ServletException, IOException {
            
            log.info("=== HTTP 요청 로그 ===");
            log.info("요청 URI: {}", request.getRequestURI());
            log.info("요청 메서드: {}", request.getMethod());
            log.info("요청 헤더: {}", getHeaders(request));
            log.info("요청 IP: {}", request.getRemoteAddr());
            log.info("요청 시간: {}", java.time.LocalDateTime.now());
            
            long startTime = System.currentTimeMillis();
            
            try {
                filterChain.doFilter(request, response);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("=== HTTP 응답 로그 ===");
                log.info("응답 상태: {}", response.getStatus());
                log.info("응답 시간: {}", java.time.LocalDateTime.now());
                log.info("처리 시간: {}ms", duration);
                
            } catch (Exception e) {
                log.error("=== HTTP 요청 처리 실패 ===");
                log.error("에러 메시지: {}", e.getMessage());
                log.error("에러 스택: ", e);
                throw e;
            }
        }
        
        private String getHeaders(HttpServletRequest request) {
            StringBuilder headers = new StringBuilder();
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.append(headerName).append(": ").append(headerValue).append(", ");
            }
            return headers.toString();
        }
    }
}
