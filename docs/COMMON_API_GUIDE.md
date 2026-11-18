
# BEENTEUM 백엔드 공통 API 사용 가이드

## 1. 개요

백엔드 API의 성공 응답과 오류 응답을 일관되게 관리하기 위한 가이드입니다.

- 성공 응답: `ApiResponse<T>`
- 오류 응답: `ProblemDetail` (RFC 9457)
- 비즈니스 예외: `BusinessException`
- 오류 코드: `ErrorCode`
- 전역 예외 처리: `GlobalExceptionHandler`

기본 패키지: `com.beentteum.common`

---

## 2. 성공 응답

### 2.1 기본 구조

```json
{
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "name": "테스트"
  }
}
```

### 2.2 사용 방법

**데이터와 메시지를 함께 반환**

```java
return ResponseEntity.ok(
    ApiResponse.success("조회 성공", data)
);
```

**데이터만 반환**

```java
return ResponseEntity.ok(
    ApiResponse.success(data)
);
```

**메시지만 반환**

```java
return ResponseEntity.ok(
    ApiResponse.success("삭제 완료")
);
```

### 2.3 참고 사항

- `message`: 처리 결과 안내 문구
- `data`: 응답 데이터
- 데이터가 없으면 `null` 반환
- HTTP 상태 코드는 `ResponseEntity`로 지정

---

## 3. 오류 응답

오류 응답은 Spring의 `ProblemDetail`을 사용합니다.

### 3.1 기본 구조

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "요청한 리소스를 찾을 수 없습니다.",
  "instance": "/api/example/999",
  "code": "RESOURCE_NOT_FOUND"
}
```

`instance`는 요청 처리 환경에 따라 포함 여부와 값이 달라질 수 있습니다.

### 3.2 비즈니스 예외 발생

```java
throw new BusinessException(
    ErrorCode.RESOURCE_NOT_FOUND
);
```

컨트롤러나 서비스에서 예외를 발생시키면 `GlobalExceptionHandler`가 공통 오류 응답으로 변환합니다.

---

## 4. 오류 코드

현재 정의된 오류 코드는 다음과 같습니다.

| HTTP 상태 | 오류 코드 | 설명 |
|---|---|---|
| 400 | INVALID_INPUT_VALUE | 잘못된 입력값 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | ACCESS_DENIED | 접근 권한 없음 |
| 404 | RESOURCE_NOT_FOUND | 리소스 없음 |
| 409 | RESOURCE_CONFLICT | 리소스 충돌 |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

추가로 전역 예외 처리기에서 다음 코드를 사용합니다.

| HTTP 상태 | 오류 코드 | 설명 |
|---|---|---|
| 400 | INVALID_REQUEST_BODY | 요청 본문 파싱 실패 |
| 405 | METHOD_NOT_ALLOWED | 지원하지 않는 HTTP 메서드 |

새로운 오류 유형이 필요한 경우 팀원들과 협의하여 공통 오류 코드를 확장합니다.

---

## 5. 입력값 검증

DTO에 Jakarta Validation 어노테이션을 적용합니다.

```java
public record ExampleRequest(
    @NotBlank(message = "이름은 필수입니다.")
    String name
) {}
```

컨트롤러에서 `@Valid`를 사용합니다.

```java
@PostMapping("/example")
public ResponseEntity<ApiResponse<String>> create(
        @Valid @RequestBody ExampleRequest request) {

    return ResponseEntity.ok(
        ApiResponse.success("등록 완료", request.name())
    );
}
```

### 검증 실패 응답 예시

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "잘못된 입력값입니다.",
  "code": "INVALID_INPUT_VALUE",
   "errors": [
    {
      "field": "name",
      "reason": "이름은 필수입니다."
    }
  ]
}
```

---

## 6. 예외 처리 정책

| 상황 | 처리 방식 |
|---|---|
| 정상 요청 | `ApiResponse` 반환 |
| 비즈니스 규칙 위반 | `BusinessException` 발생 |
| DTO 검증 실패 | 400 + 필드별 오류 |
| 잘못된 JSON | 400 + INVALID_REQUEST_BODY |
| 지원하지 않는 HTTP 메서드 | 405 + METHOD_NOT_ALLOWED |
| 예상하지 못한 서버 예외 | 500 + INTERNAL_SERVER_ERROR |

예상하지 못한 서버 오류의 상세 정보는 클라이언트에 노출하지 않고 서버 로그에 기록합니다.

401/403 보안 오류는 Spring Security 필터 단계에서 처리될 수 있으므로 인증·인가 담당자의 구현과 응답 형식을 별도로 맞춰야 합니다.

---

## 7. 팀 개발 규칙

1. 성공 응답은 `ApiResponse`를 사용합니다.
2. 비즈니스 오류는 `BusinessException`과 `ErrorCode`를 사용합니다.
3. 컨트롤러마다 별도의 오류 응답 형식을 만들지 않습니다.
4. 요청 DTO에 적절한 검증 어노테이션을 적용합니다.
5. 내부 예외 메시지나 스택 트레이스를 클라이언트에 노출하지 않습니다.
6. 새로운 오류 코드가 필요하면 공통 정의를 먼저 확인합니다.

---

## 8. 테스트

공통 응답과 예외 처리는 다음 테스트로 검증합니다.

- `ApiResponseTest`
- `GlobalExceptionHandlerTest`

전체 백엔드 테스트:

```cmd
gradlew.bat clean test
```

개별 예외 처리 테스트:

```cmd
gradlew.bat test --tests "com.beentteum.common.exception.GlobalExceptionHandlerTest"
```

---

## 9. 관련 파일

```text
backend/
├── docs/
│   └── COMMON_API_GUIDE.md
└── src/
    ├── main/java/com/beentteum/common/
    │   ├── response/
    │   │   └── ApiResponse.java
    │   └── exception/
    │       ├── ErrorCode.java
    │       ├── BusinessException.java
    │       ├── ValidationError.java
    │       └── GlobalExceptionHandler.java
    └── test/java/com/beentteum/common/
        ├── response/
        │   └── ApiResponseTest.java
        └── exception/
            └── GlobalExceptionHandlerTest.java
```
