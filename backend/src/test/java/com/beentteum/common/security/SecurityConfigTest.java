package com.beentteum.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SecurityFilterChain 의 공개/인증/ADMIN 경로 구분, JWT 필터, CORS 동작 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigTest.TestEndpoints.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    JwtProperties jwtProperties;

    @Test
    void 공개_경로는_토큰_없이_접근_가능() throws Exception {
        mockMvc.perform(get("/api/auth/test")).andExpect(status().isOk());
    }

    @Test
    void 인증_경로는_토큰_없으면_401() throws Exception {
        mockMvc.perform(get("/api/test/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_토큰이면_principal_로_userId가_주입된다() throws Exception {
        mockMvc.perform(get("/api/test/me").header("Authorization", bearer("7", Role.USER)))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void 위조된_토큰은_401() throws Exception {
        mockMvc.perform(get("/api/test/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_토큰은_401() throws Exception {
        String expired = new JwtTokenProvider(new JwtProperties(jwtProperties.secret(), Duration.ofSeconds(-1)))
                .createAccessToken("7", Role.USER);
        mockMvc.perform(get("/api/test/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void Bearer_접두사가_없으면_인증되지_않는다() throws Exception {
        String token = jwtTokenProvider.createAccessToken("7", Role.USER);
        mockMvc.perform(get("/api/test/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void USER는_ADMIN_경로_403() throws Exception {
        mockMvc.perform(get("/api/admin/test").header("Authorization", bearer("7", Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ADMIN은_ADMIN_경로_접근_가능() throws Exception {
        mockMvc.perform(get("/api/admin/test").header("Authorization", bearer("1", Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void 허용된_Origin의_Preflight는_통과() throws Exception {
        mockMvc.perform(options("/api/test/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void 허용되지_않은_Origin의_Preflight는_403() throws Exception {
        mockMvc.perform(options("/api/test/me")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private String bearer(String userId, Role role) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId, role);
    }

    /** 테스트 전용 Endpoint (운영 코드에 포함되지 않음) */
    @TestConfiguration
    static class TestEndpoints {
        @RestController
        static class Endpoints {
            @GetMapping("/api/auth/test")
            String publicEndpoint() {
                return "ok";
            }

            @GetMapping("/api/test/me")
            String me(@AuthenticationPrincipal String userId) {
                return userId;
            }

            @GetMapping("/api/admin/test")
            String admin() {
                return "admin";
            }
        }
    }
}
