# Weeth Server

Weeth - 우리 모임 커뮤니티 서버

## 기술 스택

- **Language**: Java 17 (Kotlin 2.1.0 migration planned)
- **Framework**: Spring Boot 3.5.10
- **Database**: MySQL 8.0
- **Cache**: Redis 7.0+
- **Storage**: AWS S3
- **Auth**: JWT (RS256), OAuth2 (Kakao, Apple)
- **API Docs**: SpringDoc OpenAPI 3 (Swagger)
- **Build**: Gradle 8.12 with Kotlin DSL

## 로컬 개발 환경 설정

### 1. 사전 요구사항

- JDK 17
- MySQL 8.0
- Redis 7.0+
- Gradle 8.12 (Wrapper 포함)

### 2. 환경 변수 설정

`.env.example` 파일을 `.env`로 복사하고 실제 값으로 수정:

```bash
cp .env.example .env
# .env 파일 편집
```

### 3. 빌드

```bash
./gradlew clean build
```

### 4. 실행

```bash
./gradlew bootRun
```

### 5. API 문서

애플리케이션 실행 후 접속:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## 프로젝트 구조

```
src/main/java/com/weeth/
├── domain/               # 8개 도메인
│   ├── user/            # 사용자 관리
│   ├── attendance/      # 출석 관리
│   ├── schedule/        # 일정 관리
│   ├── board/           # 게시판
│   ├── comment/         # 댓글
│   ├── file/            # 파일 관리
│   ├── penalty/         # 페널티
│   └── account/         # 회계
└── global/              # 글로벌 설정
    ├── auth/            # 인증/인가
    ├── config/          # Spring 설정
    └── common/          # 공통 유틸
```

## 아키텍처

- **레이어**: Presentation → Application → Domain
- **패턴**: UseCase, Repository, Mapper
- **예외 처리**: ErrorCode 기반 중앙 집중식
- **문서화**: Swagger 자동 생성 (@ApiErrorCodeExample, @ApiSuccessCodeExample)

## Kotlin 마이그레이션

`.claude/rules/` 디렉토리의 가이드 문서 참고:

- `architecture.md`: 아키텍처 규칙
- `mapper-dto.md`: Mapper & DTO 패턴
- `code-style.md`: 코드 스타일
- `testing.md`: 테스트 작성 가이드
- `api-design.md`: API 설계 규칙
- `exception-handling.md`: 예외 처리 규칙
- `transaction-concurrency.md`: 트랜잭션 관리
- `git-conventions.md`: Git 컨벤션
- `logging.md`: 로깅 규칙

## 테스트

```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests "*UseCaseTest"
```

## 코드 포맷팅

```bash
# ktlint 자동 포맷
./gradlew ktlintFormat

# ktlint 검사
./gradlew ktlintCheck
```

## Docker

```bash
# 개발 환경
docker build -f Dockerfile-dev -t weeth-server:dev .

# 운영 환경
docker build -f Dockerfile-prod -t weeth-server:prod .
```

## 환경별 프로파일

- `local`: 로컬 개발 환경
- `dev`: 개발 서버
- `prod`: 운영 서버
- `test`: 테스트 환경

프로파일 변경:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 주요 기능

### 인증/인가

- JWT 기반 인증 (RS256)
- OAuth2 소셜 로그인 (Kakao, Apple)
- Access Token / Refresh Token

### API 문서화

- Swagger UI를 통한 대화형 API 문서
- `@ApiErrorCodeExample`로 에러 코드 자동 문서화
- `@ApiSuccessCodeExample`로 성공 응답 자동 문서화

### 도메인

- **User**: 사용자 관리, 소셜 로그인, 프로필 관리
- **Attendance**: 출석 체크, 출석 기록 조회
- **Schedule**: 일정 생성, 조회, 관리
- **Board**: 게시판, 게시글 CRUD
- **Comment**: 댓글 CRUD, 대댓글
- **File**: 파일 업로드 (S3), 이미지 관리
- **Penalty**: 페널티 관리
- **Account**: 회계 관리, 영수증 관리

## 모니터링

- Actuator Health Check: `/actuator/health`
- Prometheus Metrics: `/actuator/prometheus`

## 문제 해결

### 빌드 실패

```bash
# 캐시 정리 후 재빌드
./gradlew clean build --refresh-dependencies
```

### 포트 충돌

`application-local.yml`에서 포트 변경:

```yaml
server:
  port: 8081
```

## 기여

1. 브랜치 생성: `feat/WTH-123-feature-name`
2. 커밋: `feat: Add feature`
3. PR 생성

자세한 내용은 `.claude/rules/git-conventions.md` 참고

## 라이선스

Copyright © 2024 Weeth Team