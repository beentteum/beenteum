package com.beentteum.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

class JwtTokenProviderTest {

    // 테스트용 더미 키 (Base64, 40byte)
    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLXVuaXQtdGVzdHMtMTIzNA==";
    private static final String OTHER_SECRET = "b3RoZXItdGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLXRlc3RzLTU2Nzg=";

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, Duration.ofMinutes(30)));

    @Test
    void 토큰_생성_후_검증하면_userId와_권한이_복원된다() {
        String token = provider.createAccessToken("42", Role.ADMIN);

        Authentication auth = provider.getAuthentication(token);

        assertThat(auth.getPrincipal()).isEqualTo("42");
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void 만료된_토큰은_ExpiredJwtException() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(new JwtProperties(SECRET, Duration.ofSeconds(-1)));
        String token = expiredProvider.createAccessToken("42", Role.USER);

        assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 다른_키로_서명된_토큰은_거부된다() {
        JwtTokenProvider other = new JwtTokenProvider(new JwtProperties(OTHER_SECRET, Duration.ofMinutes(30)));
        String token = other.createAccessToken("42", Role.USER);

        assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void 형식이_잘못된_토큰은_거부된다() {
        assertThatThrownBy(() -> provider.getAuthentication("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void 짧은_키는_생성_시점에_실패한다() {
        // "short" (5byte) → 256bit 미만
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties("c2hvcnQ=", Duration.ofMinutes(30))))
                .isInstanceOf(JwtException.class);
    }
}
