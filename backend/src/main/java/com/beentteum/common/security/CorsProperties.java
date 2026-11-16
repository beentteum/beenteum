package com.beentteum.common.security;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 cors.* 설정.
 * Frontend(Vite+React)가 별도 Origin 에서 실행되므로 허용 Origin 을 환경별로 주입한다.
 *
 * @param allowedOrigins 허용할 Frontend Origin 목록 (환경변수에서는 콤마 구분 문자열)
 */
@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins
) {
}
