# BEENTEUM 인증 / 권한 가이드 (Backend)

JWT 인증, USER / ADMIN 권한, 비회원 접근 규칙을 정리한다.
코드 위치: `backend/src/main/java/com/beentteum/common/security/`

---

## 1. 사용자 유형과 권한

| 사용자 | 토큰 | Spring Security 권한 | 접근 가능 |
|--------|------|------|-----------|
| 비회원 | 없음 | - | 공개 경로만 |
| 회원 | JWT (`role: USER`) | `ROLE_USER` | 공개 경로 + 인증 경로 |
| 관리자 | JWT (`role: ADMIN`) | `ROLE_ADMIN` | 공개 경로 + 인증 경로 + `/api/admin/**` |

- 회원 로그인과 관리자 로그인은 화면만 다르고, Backend 는 같은 JWT 구조를 쓴다. 차이는 토큰의 `role` 값뿐이다.
- 관리자 로그인 API 에서 `ADMIN` 이 아닌 계정은 로그인을 거부하도록 auth 구현 시 처리한다.

## 2. 경로별 접근 규칙

`SecurityConfig` 에서 위에서부터 순서대로 매칭한다.

| 경로 | 접근 | 설정 위치 |
|------|------|-----------|
| `/api/auth/**`, `/error` | 누구나 (토큰 불필요) | `PUBLIC_ENDPOINTS` |
| `/api/admin/**` | `ADMIN` 만 | `ADMIN_ENDPOINTS` |
| 그 외 모든 경로 | 로그인 사용자 (`USER`, `ADMIN`) | `anyRequest().authenticated()` |

### 비회원 조회 API (확정 필요)

기획상 비회원도 HOME, 공간 탐색, 카페 상세, 카페 비교를 볼 수 있다.
그런데 현재 규칙에서는 `/api/auth/**` 외 모든 경로가 로그인을 요구한다.

카페 조회 API 경로가 API 규격 문서에서 확정되면, 비회원 조회용 경로를 `PUBLIC_ENDPOINTS` 에 추가한다.

```java
// 예시: 조회(GET)만 공개하는 경우 - HttpMethod 를 지정해 등록/수정/삭제는 계속 보호
.requestMatchers(HttpMethod.GET, "/api/cafes/**").permitAll()
```

- 공개 경로에서도 토큰이 있으면 JWT 필터가 인증 정보를 넣는다. 그래서 같은 API 안에서 "로그인 사용자면 즐겨찾기 여부 표시" 같은 분기가 가능하다 (`@AuthenticationPrincipal String userId` 가 비회원이면 `null`).
- 제보, 즐겨찾기, QR 인증, 마이페이지는 회원 전용이므로 공개 경로에 넣지 않는다.

## 3. JWT 인증 흐름

```text
요청 ─▶ JwtAuthenticationFilter ─▶ AuthorizationFilter(경로 권한 검사) ─▶ Controller
          │ Authorization: Bearer {token} 추출
          │ JwtTokenProvider.getAuthentication(token)
          │   - 서명 검증, 만료(exp) 검증
          │   - 성공: SecurityContext 에 인증 저장 (principal = userId, 권한 = ROLE_USER/ROLE_ADMIN)
          │   - 실패: 인증 없이 통과 → 보호된 경로면 401
```

| 클래스 | 역할 |
|--------|------|
| `JwtTokenProvider` | 토큰 생성(`createAccessToken`), 검증 + Authentication 변환(`getAuthentication`) |
| `JwtAuthenticationFilter` | Bearer 토큰 추출 및 SecurityContext 설정 |
| `SecurityConfig` | 공개/인증/ADMIN 경로 규칙, Stateless, CORS, 필터 등록 |
| `JwtProperties` | `jwt.secret`, `jwt.access-token-expiration` 바인딩 |
| `Role` | `USER`, `ADMIN` |

토큰 Payload:

```json
{ "sub": "<userId>", "role": "USER", "iat": 1700000000, "exp": 1700001800 }
```

- 서명: HMAC-SHA (키 길이에 따라 HS256~HS512). 키는 환경변수 `JWT_SECRET` (Base64, 32byte 이상)
- 유효기간: `JWT_ACCESS_TOKEN_EXPIRATION` (기본 30m)
- 검증 시 DB 를 조회하지 않는다(Stateless). 탈퇴/정지 사용자 즉시 차단이 필요하면 추후 검토.
- Refresh Token 은 아직 없다. 필요하면 auth 기능 개발 시 추가한다.

## 4. Authorization Header 사용법

```http
GET /api/favorites HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiIs...
```

- 형식: `Bearer` + 공백 1칸 + 토큰
- 응답 코드
  - 토큰 없음 / 위조 / 만료 / 형식 오류 → `401 Unauthorized`
  - 인증됐지만 권한 부족 (USER 가 `/api/admin/**`) → `403 Forbidden`
- 현재 401/403 응답 Body 는 비어 있다. 공통 Error 규격이 확정되면 그 형식으로 바꾼다 (6장).

Frontend (Axios 예시):

```js
axios.get(`${API_BASE_URL}/api/favorites`, {
  headers: { Authorization: `Bearer ${accessToken}` },
});
```

## 5. Backend 에서 로그인 사용자 사용하기

```java
@GetMapping("/api/favorites")
public List<FavoriteResponse> myFavorites(@AuthenticationPrincipal String userId) {
    return favoriteService.findByUser(userId);
}
```

- `userId` 타입은 User 테이블 확정 전이라 `String`. PK 타입이 정해지면 변환해서 사용한다.
- 메서드 단위 권한이 필요하면 `@EnableMethodSecurity` 를 추가하고 `@PreAuthorize("hasRole('ADMIN')")` 를 사용한다 (현재 미사용).

### JWT 와 visitToken 은 다르다

| | JWT (Access Token) | visitToken (FieldAuthSession) |
|--|--|--|
| 목적 | 회원/관리자 로그인 인증 | 현장 제보 자격 (QR + GPS 100m 인증 성공) |
| 담당 패키지 | `common.security`, `auth` | `fieldauth`, `report` |
| 유효기간 | 30분 (설정) | 15분 |
| 사용 횟수 | 만료 전 계속 사용 | 성공 제보 1회 후 재사용 불가 |
| 저장 | 서버 저장 없음 | Redis (TTL) 예정 |

제보 API 는 JWT(회원 로그인) + visitToken 을 둘 다 확인해야 한다. visitToken 은 JWT 와 별개로 `fieldauth` 기능에서 구현한다.

## 6. 남은 작업 (다른 담당자 결과 필요)

| 작업 | 필요한 결과 | 연결 위치 |
|------|-------------|-----------|
| 로그인 / 회원가입 / 관리자 로그인 API | DB: User 테이블, 권한 컬럼 | `auth` 패키지, `JwtTokenProvider.createAccessToken` |
| 비밀번호 암호화 | DB: User 테이블 | 로그인 구현 시 `PasswordEncoder` Bean (BCrypt) 추가 |
| `Role` 값 일치 | DB: 권한 컬럼 값 | `Role.java` TODO |
| 401/403 응답 Body | API 공통 규격: Error Response | `SecurityConfig` TODO (`AuthenticationEntryPoint`, `AccessDeniedHandler`) |
| 만료/위조 토큰 에러 코드 구분 | API 공통 규격: Error Code | `JwtAuthenticationFilter` TODO |
| 비회원 조회 경로 공개 | API 규격: 카페 조회 URL | `SecurityConfig.PUBLIC_ENDPOINTS` |
| 토큰 저장 위치 / 401 시 재로그인 흐름 | Frontend 합의 | - |
