package com.beentteum.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 jwt.* 설정.
 * 값이 비어 있으면 서버 기동 시 바인딩 오류로 즉시 실패한다(필수값 누락을 런타임 전에 발견).
 *
 * @param secret                 Base64 인코딩된 HMAC-SHA 비밀키 (디코딩 후 32byte 이상)
 * @param accessTokenExpiration  Access Token 유효기간
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTokenExpiration
) {
}
