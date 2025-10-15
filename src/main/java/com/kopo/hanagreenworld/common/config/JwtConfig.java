package com.kopo.hanagreenworld.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;
    private long accessTokenValidityInMinutes = 60 * 24; // 24시간
    private long refreshTokenValidityInDays = 30; // 30일
}
