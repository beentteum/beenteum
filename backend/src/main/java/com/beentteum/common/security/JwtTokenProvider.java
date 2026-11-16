package com.beentteum.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * JWT 생성/검증 담당.
 * <p>
 * - subject: 사용자 식별자(userId). 타입은 User 테이블 확정 전이라 문자열로 둔다.
 * - role claim: {@link Role} 이름(USER/ADMIN)
 * <p>
 * 검증은 서명 + 만료(exp)만 확인하며 DB 를 조회하지 않는다(Stateless).
 */
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        // 키가 256bit 미만이면 jjwt 가 WeakKeyException 을 던져 서버 기동이 실패한다
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.properties = properties;
    }

    /**
     * Access Token 생성.
     * TODO(auth): 로그인 API 구현 시, DB 에서 사용자 확인 후 이 메서드로 토큰을 발급한다.
     */
    public String createAccessToken(String userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenExpiration())))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰을 검증하고 Spring Security Authentication 으로 변환한다.
     * principal 은 userId(String), 권한은 ROLE_USER / ROLE_ADMIN.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료된 토큰
     * @throws JwtException                        서명 불일치, 형식 오류 등 유효하지 않은 토큰
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Role role = parseRole(claims.get(ROLE_CLAIM, String.class));
        return UsernamePasswordAuthenticationToken.authenticated(
                claims.getSubject(), null, List.of(new SimpleGrantedAuthority(role.authority())));
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JwtException("Invalid role claim: " + value, e);
        }
    }
}
