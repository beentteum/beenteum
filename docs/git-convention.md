# BEENTEUM Git 협업 규칙

팀 전체(FE / BE / DB)가 같이 쓰는 GitHub 작업 규칙이다. 초안이며 임원진 확인 후 확정한다.

저장소: https://github.com/beentteum/beenteum

---

## 1. 브랜치 구조

```text
main
└─ dev
   └─ feature/*
```

| 브랜치 | 용도 | 직접 push |
|--------|------|-----------|
| `main` | 최종 제출/배포 기준. 안정 버전만 | 금지 |
| `dev` | 개발 통합. 모든 기능이 여기로 모임 | 금지 |
| `feature/*` 등 | 개인 작업 | 가능 |

- `main`, `dev` 는 GitHub Branch protection 으로 막혀 있다 (PR + 승인 1명 필요, 관리자 포함, force push·삭제 금지).
- 모든 작업은 **`dev` 에서 새 브랜치를 만들어** 시작하고, 끝나면 **`dev` 로 PR** 을 올린다.
- `dev` → `main` 병합은 통합 점검(12/06), 기능 Freeze(12/13), 최종 점검(12/14) 시점에 임원진이 함께 진행한다.

## 2. 브랜치 이름

`종류/내용/내이름` 형식, 영문 소문자와 하이픈만 사용한다. **맨 뒤에 본인 이름(영문)을 붙인다.** 누구 브랜치인지 바로 알 수 있고, 같은 기능을 여러 명이 작업해도 이름이 겹치지 않는다.

| 종류 | 용도 | 예시 |
|------|------|------|
| `feature/` | 새 기능 | `feature/cafe-detail/yeonseung`, `feature/report-admin/yunseo` |
| `fix/` | 버그 수정 | `fix/login-token-expire/yunseo` |
| `docs/` | 문서만 수정 | `docs/auth-guide/yunseo` |
| `chore/` | 설정·빌드·의존성 | `chore/gradle-update/yunseo` |

- 이름은 본인 GitHub ID 또는 영문 이름 중 하나로 정해 계속 같은 것을 쓴다.
- `feature/cafe-detail` 브랜치가 이미 있으면 `feature/cafe-detail/이름` 은 만들 수 없다 (Git 제한). 내용 부분을 다르게 쓴다.

- 한 브랜치에는 **한 가지 작업**만 담는다. (여러 기능을 섞지 않는다)
- 작업이 끝나 병합된 브랜치는 삭제한다.

## 3. 작업 순서

```bash
git switch dev
git pull --ff-only origin dev           # 최신 dev 받기
git switch -c feature/cafe-detail/yunseo   # 내 브랜치 만들기 (맨 뒤에 내 이름)
# ... 작업 ...
git add <변경한 파일>                    # git add . 대신 파일/폴더를 지정
git status --short                      # 올라갈 파일 확인
git commit -m "feat: 카페 상세 API 구현"
git push -u origin feature/cafe-detail/yunseo
```

그다음 GitHub 에서 **Compare & pull request** → base `dev` ← compare 내 브랜치.

- 작업 중 `dev` 가 앞서 나가면 `git pull --ff-only origin dev` 로 받은 뒤 내 브랜치에 합친다 (`git merge dev`). 충돌이 나면 혼자 덮어쓰지 말고 해당 파일 작성자와 같이 해결한다.
- 다른 사람이 수정 중인 공통 파일(`SecurityConfig.java`, `application.yml`, `build.gradle.kts`, `docs/env.example` 등)을 바꿔야 하면 먼저 팀에 공유한다.

## 4. 커밋 메시지

```text
종류: 한 줄 요약
```

| 종류 | 용도 |
|------|------|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 |
| `refactor` | 동작 변경 없는 코드 정리 |
| `test` | 테스트 추가·수정 |
| `chore` | 설정, 의존성, 빌드 |

예시:

```text
feat: 관리자 제보 상태 변경 API 구현
fix: 만료된 토큰이 403으로 응답되는 문제 수정
docs: Backend 인증 가이드 추가
```

- 요약은 한 줄, 무엇을 했는지 알 수 있게 쓴다.
- 커밋은 의미 단위로 나눈다. ("수정", "작업중" 같은 메시지 금지)

## 5. Pull Request

- base 는 항상 `dev` (직접 `main` 으로 올리지 않는다).
- 제목은 커밋 메시지와 같은 형식 (`feat: ...`).
- 본문은 `.github/pull_request_template.md` 양식을 채운다.
- PR 은 가능하면 작게 (한 기능, 변경 파일 수십 개 이내) 올린다.
- **올리기 전에 직접 확인한다.**
  - Backend: `cd backend && ./gradlew build` 가 **DB·Redis 없이도** 통과하는지
  - Frontend: `npm run lint`, `npm run build` 통과
- 설정·환경변수·API 형식을 바꿨다면 관련 문서(`docs/`)도 같이 수정한다.

## 6. 리뷰와 병합

- 승인은 **작성자 본인이 아닌 팀원 1명 이상** 이 한다. (임원진 우선 요청)
- 리뷰어는 아래를 본다.
  - 변경이 PR 설명과 일치하는가 / 다른 담당 영역을 침범하지 않았는가
  - 빌드·테스트가 통과하는가
  - Secret, `.env`, 개인 PC 경로가 포함되지 않았는가
  - 공통 규격(응답·에러·인증)을 따르는가
- 수정 요청(Request changes)이 있으면 같은 브랜치에 커밋을 추가로 push 한다. 새 승인이 필요하다.
- 승인 후 **작성자가 직접 병합**한다. 병합 방식은 `Squash and merge` 를 기본으로 한다. (커밋 기록을 기능 단위로 유지)
- 병합 후 브랜치를 삭제한다.

## 7. 커밋하면 안 되는 것

| 항목 | 이유 |
|------|------|
| `.env`, `.env.local`, 비밀번호, API Key, JWT Secret | 유출 위험. 예시는 `docs/env.example`, `frontend/.env.example` 에만 둔다 |
| `node_modules/`, `build/`, `dist/`, `.gradle/` | 빌드 결과물 |
| `.idea/`, `.vscode/`, OS 임시 파일 | 개인 환경 |
| DB 덤프, 개인정보가 담긴 데이터 | 개인정보 |

- 실수로 Secret 을 push 했다면 **삭제 커밋만으로는 기록에 남는다.** 즉시 임원진에게 알리고 해당 키를 새로 발급한다.
- 커밋 전에 `git status --short` 로 올라가는 파일을 확인하는 습관을 들인다.

## 8. DB 변경 규칙

- 스키마는 Flyway 마이그레이션(`backend/src/main/resources/db/migration/`)으로 관리한다.
- 이미 병합된 `V1__...sql` 은 수정하지 않는다. 변경은 `V2__...sql` 처럼 **새 버전 파일**로 추가한다.
- 마이그레이션 번호가 겹치지 않도록 DB 담당자(원주현)와 먼저 조율한다.

## 9. 작업 상태 (WBS)

```text
진행 전 → 개발 중 → 연결 대기 → 리뷰 중 → 완료 / 이슈
```

- PR 을 올리면 WBS 를 `리뷰 중` 으로, `dev` 병합 후 `완료` 로 바꾼다.
- 문제가 있으면 `이슈` 로 바꾸고 팀에 공유한다.

## 10. 자주 쓰는 상황

| 상황 | 방법 |
|------|------|
| 엉뚱한 브랜치에서 작업을 시작했다 | 커밋 전이면 `git switch -c feature/올바른내용/내이름` (변경 내용이 그대로 따라옴) |
| `main`/`dev` 에 push 했더니 거부된다 | 정상이다. feature 브랜치를 만들어 PR 로 올린다 |
| PR base 가 `main` 으로 되어 있다 | PR 화면의 Edit 에서 `dev` 로 변경 |
| 방금 커밋 메시지를 고치고 싶다 | push 전이면 `git commit --amend`. 이미 push 했다면 그대로 두고 다음 커밋에서 반영 |
| 충돌이 났다 | 파일을 열어 `<<<<<<<` 표시 부분을 양쪽 작성자와 확인해 정리 후 `git add`, `git commit` |
