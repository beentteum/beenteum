# BEENTEUM Backend 개발 가이드

Backend 공통 개발 기반(Spring Boot + Security + JWT + 환경설정) 사용법을 정리한 문서다.
비즈니스 기능(카페/제보/즐겨찾기 등)은 이 기반 위에서 각 담당자가 구현한다.

---

## 1. 개발환경 요구사항

| 항목 | 버전 / 비고 |
|------|------|
| JDK | 21 (Gradle toolchain 이 21 을 요구) |
| Gradle | 별도 설치 불필요. `backend/gradlew` (Wrapper, Gradle 9.9.1) 사용 |
| Spring Boot | 4.1.2 |
| PostgreSQL + PostGIS | DB 담당자 결과 확정 후 사용. **DB 가 없어도 서버 기동은 가능** (첫 쿼리 시 연결) |
| Redis | 캐시 기능 구현 시 필요. 없어도 서버 기동은 가능 (Redis 사용 시점에 연결) |
| IDE | IntelliJ 권장. `backend/` 폴더를 Gradle 프로젝트로 열기 |

## 2. 프로젝트 실행 방법

```bash
cd backend
cp ../docs/env.example .env    # 값 채우기 (아래 4장 참고)
./gradlew bootRun              # Windows: gradlew.bat bootRun
```

- 기본 포트: `8080`
- 테스트: `./gradlew test`
- 빌드(테스트 포함): `./gradlew build` → `build/libs/backend-0.0.1-SNAPSHOT.jar`
- Jar 실행: `java -jar build/libs/backend-0.0.1-SNAPSHOT.jar` (backend 폴더에서 실행해야 `.env` 를 읽음)

필수 환경변수가 없으면 서버가 아래처럼 **누락된 변수 이름을 출력하고 종료**한다.

```text
java.lang.IllegalStateException: 필수 환경변수가 설정되지 않아 서버를 시작할 수 없습니다. (docs/env.example 참고)
  - spring.datasource.url <- Could not resolve placeholder 'DB_URL' in value "${DB_URL}"
  - jwt.secret <- Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
```

> DB 접속정보가 아직 확정되지 않았다면, 로컬에서는 임시 값(예: `DB_URL=jdbc:postgresql://localhost:5432/beentteum`)을 넣어도 서버는 뜬다.
> DB 를 사용하는 API 를 호출하는 순간 연결 오류가 난다.

## 3. 폴더 / 패키지 구조

```text
beenteum/
├── backend/                         Spring Boot 프로젝트
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/beentteum/
│       │   ├── BeentteumBackendApplication.java
│       │   └── common/
│       │       ├── config/          공통 설정 (필수 환경변수 검사)
│       │       └── security/        Security, JWT, CORS, Role
│       ├── main/resources/
│       │   ├── application.yml          공통 설정
│       │   ├── application-local.yml    로컬
│       │   └── application-prod.yml     운영
│       └── test/                    테스트 (application-test.yml 포함)
├── docs/
│   ├── backend-guide.md             이 문서
│   └── env.example                  환경변수 예시
└── (frontend/)                      별도 Vite + React 프로젝트 (이번 셋업 범위 아님)
```

### 도메인 패키지 (모듈형 모놀리식)

빈 패키지/파일은 미리 만들지 않았다. 기능 개발을 시작할 때 아래 이름으로 만든다.

| 패키지 | 담당 영역 |
|--------|-----------|
| `com.beentteum.common` | 공통 설정, Security, (예정) 공통 Response/Error |
| `com.beentteum.auth` | 로그인/회원가입/토큰 발급 |
| `com.beentteum.user` | 사용자 정보 |
| `com.beentteum.cafe` | 카페 정보, 위치 검색(PostGIS) |
| `com.beentteum.congestion` | 혼잡도 집계/예상 혼잡도 |
| `com.beentteum.fieldauth` | 현장 인증 (QR, 위치) |
| `com.beentteum.report` | 혼잡도 제보 |
| `com.beentteum.favorite` | 즐겨찾기 |
| `com.beentteum.admin` | 관리자 기능 (`/api/admin/**`) |
| `com.beentteum.audit` | 감사 로그 |
| `com.beentteum.external` | Kakao Local API, 공공데이터 등 외부 API Client |

각 도메인 내부는 필요한 것만 만든다.

```text
com.beentteum.report/
├── controller/   @RestController - 요청/응답 변환만
├── service/      비즈니스 로직, @Transactional
├── repository/   Spring Data JPA Repository
├── dto/          요청/응답 DTO
└── entity/       JPA Entity (DB 담당 결과 확정 후)
```

의존 방향: `Controller → Service → Repository`. 다른 도메인의 데이터가 필요하면 그 도메인의 **Service** 를 호출한다(다른 도메인의 Repository 직접 호출 지양).

## 4. Spring Profile / 환경변수

| Profile | 사용 | 특징 |
|---------|------|------|
| `local` | 로컬 개발 (기본값) | Redis host `localhost`, CORS `http://localhost:5173` 기본값, SQL 로그 출력 |
| `prod` | 운영 서버 | 기본값 없음. 모든 필수 환경변수 직접 설정 |
| `test` | 자동 테스트 전용 | `src/test/resources/application-test.yml` 의 더미 값 사용 |

Profile 지정: 환경변수 `SPRING_PROFILES_ACTIVE=prod` (미지정 시 `local`).

환경변수 설정 방법 (택 1):
1. **`backend/.env` 파일** (로컬 권장): `docs/env.example` 을 복사해 값 입력. 서버가 자동으로 읽는다.
2. IntelliJ Run Configuration → Environment variables
3. OS / 컨테이너 환경변수 (운영)

| 변수 | local | prod | 설명 |
|------|-------|------|------|
| `DB_URL` | 필수 | 필수 | `jdbc:postgresql://host:5432/db` |
| `DB_USERNAME` | 필수 | 필수 | |
| `DB_PASSWORD` | 필수 | 필수 | |
| `REDIS_HOST` | 기본 localhost | 필수 | |
| `REDIS_PORT` | 기본 6379 | 기본 6379 | |
| `REDIS_PASSWORD` | 선택 | 선택 | |
| `JWT_SECRET` | 필수 | 필수 | Base64, 32byte 이상. `openssl rand -base64 32` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 기본 30m | 기본 30m | |
| `CORS_ALLOWED_ORIGINS` | 기본 http://localhost:5173 | 필수 | 콤마 구분 |
| `KAKAO_REST_API_KEY` | 선택 | 선택 | 기능 구현 시 필요 |
| `PUBLIC_DATA_SERVICE_KEY` | 선택 | 선택 | 기능 구현 시 필요 |

**Secret 규칙**
- 실제 값은 코드/yml/문서에 쓰지 않는다. `.env` 는 `.gitignore` 대상.
- `JWT_SECRET` 이 32byte 미만이면 서버 기동이 실패한다 (`WeakKeyException`).
- 필수 환경변수를 추가하면 `application.yml`, `RequiredEnvironmentValidator`, `docs/env.example`, 이 문서를 함께 수정한다.

## 5. JWT 인증 구조

```text
요청 ─▶ JwtAuthenticationFilter ─▶ AuthorizationFilter(URL 권한 검사) ─▶ Controller
          │ Authorization: Bearer {token} 추출
          │ JwtTokenProvider.getAuthentication(token)
          │   - 서명 검증, 만료(exp) 검증
          │   - 성공: SecurityContext 에 인증 저장 (principal = userId, 권한 = ROLE_USER/ROLE_ADMIN)
          │   - 실패: 인증 없이 통과 → 보호된 경로면 401
```

| 클래스 | 역할 |
|--------|------|
| `common.security.JwtTokenProvider` | 토큰 생성(`createAccessToken`), 검증 + Authentication 변환(`getAuthentication`) |
| `common.security.JwtAuthenticationFilter` | Bearer 토큰 추출 및 SecurityContext 설정 |
| `common.security.SecurityConfig` | 공개/인증/ADMIN 경로 규칙, Stateless, CORS, 필터 등록 |
| `common.security.JwtProperties` | `jwt.secret`, `jwt.access-token-expiration` 바인딩 |
| `common.security.Role` | `USER`, `ADMIN` |

토큰 Payload:

```json
{ "sub": "<userId>", "role": "USER", "iat": 1700000000, "exp": 1700001800 }
```

- 서명: HMAC-SHA (키 길이에 따라 HS256~HS512)
- 검증 시 DB 를 조회하지 않는다(Stateless). 탈퇴/정지 사용자 즉시 차단이 필요하면 추후 검토.
- Refresh Token 은 아직 없다. 필요 시 auth 담당이 추가.

### Controller 에서 로그인 사용자 꺼내기

```java
@GetMapping("/api/favorites")
public List<FavoriteResponse> myFavorites(@AuthenticationPrincipal String userId) {
    return favoriteService.findByUser(userId);
}
```

> `userId` 타입은 User 테이블 확정 전이라 `String`. PK 타입이 정해지면 변환해서 사용한다.

## 6. Authorization Header 사용법

```http
GET /api/favorites HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiIs...
```

- 형식: `Bearer` + 공백 1칸 + 토큰
- 응답 코드
  - 토큰 없음 / 위조 / 만료 / 형식 오류 → `401 Unauthorized`
  - 인증됐지만 권한 부족(USER 가 `/api/admin/**`) → `403 Forbidden`
- 현재 401/403 응답 Body 는 비어 있다 (공통 Error 규격 확정 후 연결, 9장).

Frontend (fetch 예시):

```js
fetch(`${API_BASE_URL}/api/favorites`, {
  headers: { Authorization: `Bearer ${accessToken}` },
});
```

## 7. USER / ADMIN Role 구조

| 경로 | 접근 |
|------|------|
| `/api/auth/**`, `/error` | 누구나 (토큰 불필요) |
| `/api/admin/**` | `ADMIN` 만 |
| 그 외 모든 경로 | 로그인 사용자 (`USER`, `ADMIN`) |

- 경로 규칙은 `SecurityConfig` 의 `PUBLIC_ENDPOINTS`, `ADMIN_ENDPOINTS` 에서 관리한다.
- 비로그인 조회 API(예: 지도에서 카페 목록 보기)가 기획상 필요하면 `PUBLIC_ENDPOINTS` 에 추가한다.
- 메서드 단위 권한이 필요하면 `@EnableMethodSecurity` 를 추가하고 `@PreAuthorize("hasRole('ADMIN')")` 를 사용한다 (현재 미사용).

## 8. 신규 Backend 기능 추가 시 권장 위치

| 만들 것 | 위치 | URL |
|---------|------|-----|
| 로그인/회원가입 API | `auth.controller`, `auth.service` | `/api/auth/...` (공개) |
| 카페 조회 API | `cafe.controller`, `cafe.service`, `cafe.repository` | `/api/cafes/...` |
| 혼잡도 제보 | `report.*` | `/api/reports/...` |
| 즐겨찾기 | `favorite.*` | `/api/favorites/...` |
| 관리자 기능 | `admin.*` | `/api/admin/...` (ADMIN) |
| Kakao/공공데이터 Client | `external.kakao`, `external.publicdata` | - |
| 설정 Bean | 해당 도메인 `config/` 또는 `common.config` | - |

- 설정값은 `application.yml` 에 추가하고 `@ConfigurationProperties` record 로 받는다 (예: `JwtProperties`).
  `@ConfigurationPropertiesScan` 이 켜져 있어 record 를 만들면 자동 등록된다.
- 외부 API Key 는 이미 `external.kakao.rest-api-key`, `external.public-data.service-key` 로 준비돼 있다.

## 9. 다른 담당자 결과가 나온 뒤 연결할 부분

### DB / ERD 담당 결과 이후

- [ ] `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 값 공유 → 각자 `.env` 에 설정
- [ ] 도메인별 `entity` 작성 (PostGIS 컬럼은 `org.locationtech.jts.geom.Point` 등, `hibernate-spatial` 이미 포함)
- [ ] `Role` enum 값과 사용자 권한 컬럼 값 일치시키기 (`common/security/Role.java` TODO)
- [ ] 로그인 구현: `auth.service` 에서 사용자 조회/비밀번호 검증 후 `JwtTokenProvider.createAccessToken(userId, role)` 호출 (`JwtTokenProvider` TODO)
  - 비밀번호 저장 시 `PasswordEncoder` Bean(BCrypt 등)을 이때 추가
- [ ] 스키마 관리 방식 결정 (현재 `ddl-auto: none`. Flyway 등 도입 여부)
- [ ] `application.yml` 의 `allow_jdbc_metadata_access: false` 유지 여부 재검토
  (현재는 DB 없이도 서버가 뜨도록 설정. DB 가 준비되면 기동 시 연결을 확인하도록 바꿀 수 있음)

### 공통 Response / Error 규격 담당 결과 이후

- [ ] `common` 하위에 공통 응답 객체 / `@RestControllerAdvice` 추가 (현재 없음, 임의 규격 만들지 않음)
- [ ] `SecurityConfig` 의 401/403 처리를 규격 Body 를 쓰는 `AuthenticationEntryPoint` / `AccessDeniedHandler` 로 교체 (`SecurityConfig` TODO)
- [ ] 만료 토큰과 위조 토큰을 구분한 에러 코드가 필요하면 `JwtAuthenticationFilter` 에서 원인을 request attribute 로 전달 (`JwtAuthenticationFilter` TODO)

### Frontend 이후

- [ ] 운영 Frontend 주소 확정 → 운영 서버 `CORS_ALLOWED_ORIGINS` 에 설정
- [ ] Vite 개발 서버 포트가 5173 이 아니면 로컬 `.env` 의 `CORS_ALLOWED_ORIGINS` 변경
- [ ] Frontend 에서 토큰 저장 위치와 401 처리(재로그인) 흐름 합의

### 배포 (Docker / OCI / Nginx) 이후

- [ ] Dockerfile, 운영 환경변수 주입 방식 결정
- [ ] Nginx 뒤에서 실행 시 `server.forward-headers-strategy` 설정 검토
- [ ] 헬스체크 Endpoint 필요 시 `spring-boot-starter-actuator` 추가 + `PUBLIC_ENDPOINTS` 등록
