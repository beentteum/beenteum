package com.beentteum.common.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 서버 기동 직후, 필수 환경변수가 설정되었는지 한 번에 검사한다.
 * <p>
 * Spring Boot 는 해석되지 않은 ${DB_URL} 같은 값을 문자열 그대로 바인딩하기 때문에
 * 미설정 시 "'url' must start with jdbc" 처럼 원인을 알기 어려운 오류가 난다.
 * 이를 막기 위해 누락된 환경변수 이름을 모아 명확한 메시지로 기동을 중단한다.
 * <p>
 * 필수 항목을 추가/삭제할 때는 docs/env.example 과 함께 수정한다.
 */
public class RequiredEnvironmentValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.data.redis.host",
            "jwt.secret",
            "cors.allowed-origins"
    );

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        List<String> missing = new ArrayList<>();
        for (String key : REQUIRED_PROPERTIES) {
            try {
                // 값 안의 ${ENV} 가 해석되지 않으면 IllegalArgumentException
                String value = env.getProperty(key);
                if (value == null || value.isBlank()) {
                    missing.add(key + " (값이 비어 있음)");
                }
            } catch (IllegalArgumentException e) {
                missing.add(key + " <- " + e.getMessage());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("""
                    필수 환경변수가 설정되지 않아 서버를 시작할 수 없습니다. (docs/env.example 참고)
                      - %s""".formatted(String.join("\n  - ", missing)));
        }
    }
}
