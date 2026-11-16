package com.beentteum;

import com.beentteum.common.config.RequiredEnvironmentValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration 제외: 인증은 JWT 필터가 담당하므로
// Spring Boot 기본 인메모리 사용자(generated password)를 만들지 않는다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
// @ConfigurationProperties record(JwtProperties, CorsProperties)를 자동 등록
@ConfigurationPropertiesScan
public class BeentteumBackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(BeentteumBackendApplication.class);
		// 필수 환경변수 누락 시 원인을 명확히 알려주고 기동 중단
		app.addListeners(new RequiredEnvironmentValidator());
		app.run(args);
	}

}
