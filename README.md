# Weeth Server

Spring Boot 3.5.10 + Kotlin 기반 동아리 커뮤니티 플랫폼 백엔드

## 기술 스택

| 분류 | 스택 |
|------|------|
| 언어 | Kotlin 2.1.0 |
| 프레임워크 | Spring Boot 3.5.10, Gradle 8.12 (Kotlin DSL) |
| 데이터베이스 | MySQL 8.0, Redis 7.0+, Spring Data JPA |
| 스토리지 | AWS S3 (SDK v2) |
| 인증 | JWT (JJWT 0.13.0), OAuth2 (Kakao, Apple) |
| API 문서 | SpringDoc OpenAPI 3 (Swagger UI) |
| 테스트 | Kotest 5.9.1, MockK, Testcontainers |
| 코드 품질 | ktlint 1.8.0 |
| 모니터링 | Spring Actuator, Micrometer Prometheus |

## 빠른 시작

**사전 요구사항:** JDK 21, MySQL 8.0, Redis 7.0+

```bash
./gradlew clean build                                     # 빌드
./gradlew bootRun                                         # 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=dev'   # 프로파일 지정 실행
./gradlew test                                            # 전체 테스트
./gradlew ktlintFormat                                    # 자동 포맷팅
```

### 프로파일

| Profile | DDL Auto | Swagger |
|---------|----------|---------|
| `local` (기본) | `update` | 활성화 |
| `dev` | `update` | 활성화 |
| `prod` | `validate` | 비활성화 |
| `test` | `create-drop` | 비활성화 |

## 아키텍처

```
presentation → application → domain ← infrastructure
```

- **Rich Domain Model** — Entity가 비즈니스 로직, 검증, 상태 전이를 소유
- **UseCase = 오케스트레이션** — Command (`@Transactional`) / Query (`readOnly = true`)
- **Port-Adapter** — domain이 Port 인터페이스 소유, infrastructure가 구현
- **No thin wrappers** — UseCase가 Repository를 직접 호출

### 응답 코드 형식 (`XDDNN`)

| X | 분류 |
|---|------|
| 1 | 성공 |
| 2 | 도메인 에러 |
| 3 | 인프라/서버 에러 |
| 4 | 클라이언트/검증 에러 |

DD = 도메인 ID (2자리), NN = 순번 (00~99)

## 프로젝트 구조

```
src/main/kotlin/com/weeth/
├── domain/
│   ├── user/           # 사용자 관리, 소셜 로그인
│   ├── attendance/     # 출석 체크
│   ├── session/        # 스터디 세션 관리
│   ├── schedule/       # 일정 관리
│   ├── board/          # 게시판, 게시글 CRUD
│   ├── comment/        # 댓글, 대댓글
│   ├── file/           # 파일 업로드 (S3)
│   ├── penalty/        # 페널티 관리
│   ├── account/        # 회계, 영수증 관리
│   └── cardinal/       # 기수 관리
└── global/
    ├── auth/           # JWT, OAuth2, @CurrentUser
    ├── config/         # Spring 설정
    └── common/         # 공통 유틸, 응답 포맷
```

## 인프라

### 배포 아키텍처

```
GitHub Actions (CI/CD)
├── CI: PR/Push → ktlint 검사 → 빌드 및 테스트
├── Deploy-Dev: dev 브랜치 → Docker Hub → EC2 배포
└── Deploy-Prod: Release 발행 → Docker Hub → EC2 배포

EC2 (ARM64)
├── Caddy 2.8 (리버스 프록시, 자동 HTTPS)
├── Spring Boot App (Blue-Green 배포)
│   ├── app-blue  (:18081)
│   └── app-green (:18082)
├── MySQL 8.0 (Dev만, Prod는 RDS)
└── Redis 7.0
```

### Blue-Green 배포

무중단 배포를 위해 Blue-Green 방식을 사용합니다.

1. 새 컨테이너(Blue/Green) 시작
2. `/actuator/health` 헬스 체크 (20회, 3초 간격)
3. Caddy upstream 전환
4. 이전 컨테이너 종료

### CI/CD 파이프라인

| 워크플로우 | 트리거 | 동작 |
|-----------|--------|------|
| CI | PR / Push (dev, main) | ktlint 검사 → 빌드 → 테스트 |
| Deploy Dev | CI 완료 (dev) | Docker 빌드 (ARM64) → Docker Hub → EC2 배포 |
| Deploy Prod | Release 발행 | Docker 빌드 (ARM64) → Docker Hub → EC2 배포 |

### 모니터링

- `/actuator/health` — 헬스 체크 (배포 시 사용)
- `/actuator/prometheus` — Prometheus 메트릭 노출

### 인프라 파일 구조

```
infra/
├── dev/
│   ├── docker-compose.yml    # Dev 환경 (Caddy + MySQL + Redis + App)
│   ├── caddy/Caddyfile       # Caddy 리버스 프록시 설정
│   └── scripts/deploy.sh     # Blue-Green 배포 스크립트
└── prod/
    ├── docker-compose.yml    # Prod 환경 (Caddy + Redis + App, MySQL은 RDS)
    ├── caddy/Caddyfile
    └── scripts/deploy.sh
```

## 라이선스

Copyright 2024 Weeth Team
