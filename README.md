# 빈틈 (BEENTEUM)

카페 혼잡도 정보 서비스.

## 저장소 구조

```text
beenteum/
├─ backend/   Spring Boot Backend (모듈형 모놀리식, Security + JWT, PostgreSQL/PostGIS, Redis)
├─ docs/      개발 문서
└─ README.md
```

Frontend(Vite + React)는 FE 담당자가 별도로 셋업한다.

## Backend 빠른 시작

```bash
cd backend
cp ../docs/env.example .env   # 값 채우기 (실제 Secret 은 커밋 금지)
./gradlew bootRun
```

자세한 내용: [docs/backend-guide.md](docs/backend-guide.md)
