# Weeth Server

동아리 관리 서비스 백엔드 저장소

> **Java → Kotlin 마이그레이션 진행 중**
> 새로운 코드는 Kotlin으로 작성되며, 기존 Java 코드(~271 파일)는 점진적으로 마이그레이션됩니다.

## 📋 목차

- [기술 스택](#-기술-스택)
- [빠른 시작](#-빠른-시작)
- [아키텍처](#-아키텍처)
- [프로젝트 구조](#-프로젝트-구조)
- [개발 가이드](#-개발-가이드)
- [테스트](#-테스트)

## 🛠 기술 스택

### Core
- **Language**: Kotlin 2.1.0 (Java 17에서 점진적 마이그레이션)
- **Framework**: Spring Boot 3.5.10
- **Build**: Gradle 8.12 (Kotlin DSL)

### Database & Cache
- **Database**: MySQL 8.0
- **Cache**: Redis 7.0+
- **ORM**: Spring Data JPA

### Infrastructure
- **Storage**: AWS S3 (SDK v2)
- **Auth**: JWT (JJWT 0.13.0, Symmetric Key), OAuth2 (Kakao, Apple)
- **API Docs**: SpringDoc OpenAPI 3 (Swagger UI)
- **Monitoring**: Spring Actuator, Micrometer Prometheus

### Testing
- **Framework**: Kotest 5.9.1 (DescribeSpec, BehaviorSpec, StringSpec)
- **Mocking**: MockK 1.13.14, SpringMockK 4.0.2
- **Integration**: Testcontainers 2.0.3 (MySQL)

### Code Quality
- **Linter/Formatter**: ktlint 1.8.0
- **Logging**: SLF4J + Logback, Loki aggregation

## 🚀 빠른 시작

### 사전 요구사항

- JDK 17
- MySQL 8.0
- Redis 7.0+
- Gradle 8.12 (Wrapper 포함)

### 환경 변수 설정

.env` 파일 생성 or 환경변수 주입


### 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 로컬 실행 (기본 프로파일)
./gradlew bootRun

# 특정 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 프로파일

| Profile | 용도 | DDL Auto | Swagger |
|---------|------|----------|---------|
| `local` | 로컬 개발 (기본) | `update` | 활성화 |
| `dev` | 개발 서버 | `update` | 활성화 |
| `prod` | 운영 서버 | `validate` | 비활성화 |
| `test` | 테스트 실행 | `create-drop` | 비활성화 |

## 🏗 아키텍처

### 레이어 구조

```
presentation → application → domain ← infrastructure
```

- **presentation**: Controller, ResponseCode 열거형
- **application**: UseCase (command/query), DTO, Mapper, Exception, Validator
- **domain**: Entity (Rich Domain Model), VO, Enum, Repository, Port, Domain Service
- **infrastructure**: Port 구현체 (S3, 외부 API 어댑터)

### 핵심 패턴

#### 1. Rich Domain Model
- Entity가 비즈니스 로직, 검증, 상태 전이를 담당
- UseCase는 오케스트레이션만 수행 (얇은 조정 계층)

#### 2. Port-Adapter Pattern
- `domain/port/`: 도메인 언어로 작성된 인터페이스 (예: `FileStorage`, `PushNotificationSender`)
- `infrastructure/`: 기술 구현체 (예: `S3FileStorage`, `FcmPushNotificationSender`)
- UseCase는 Port 인터페이스만 의존 → 테스트 용이, 교체 가능

#### 3. UseCase 분리
- **Command UseCase** (`usecase/command/`): 상태 변경, `@Transactional`
- **Query Service** (`usecase/query/`): 읽기 전용, `@Transactional(readOnly = true)`

#### 4. 도메인 간 참조
- **읽기**: 대상 도메인의 Reader 인터페이스 사용
- **쓰기 (동일 트랜잭션)**: Repository 직접 호출
- **쓰기 (트랜잭션 분리)**: Domain Event 활용

### 응답 형식

모든 API 응답은 `CommonResponse<T>`로 래핑:

```json
{
  "code": 1100,
  "message": "사용자 조회 성공",
  "data": { ... }
}
```

- **성공 코드**: `1xxx` (도메인별 `*ResponseCode` 열거형)
- **에러 코드**: `2xxx` (도메인 에러), `3xxx` (서버), `4xxx` (클라이언트)

### 에러 코드 범위

| Domain | Success | Error |
|--------|---------|-------|
| Account | 11xx    | 21xx  |
| Attendance | 12xx    | 22xx  |
| Board | 13xx    | 23xx  |
| Comment | 14xx    | 24xx  |
| File | 15xx    | 25xx  |
| Penalty | 16xx    | 26xx  |
| Schedule | 17xx    | 27xx  |
| User | 18xx    | 28xx  |
| JWT (전역) | —       | 29xx  |

## 📁 프로젝트 구조

```
src/main/
├── java/com/weeth/              # 레거시 Java 코드 (~271 파일, 점진적 마이그레이션)
└── kotlin/weeth/
    ├── domain/
    │   ├── user/                # 사용자 관리
    │   ├── attendance/          # 출석 관리
    │   ├── schedule/            # 일정 관리 (Event, Meeting)
    │   ├── board/               # 게시판 (Notice, Post)
    │   ├── comment/             # 댓글
    │   ├── file/                # 파일 업로드 (S3)
    │   ├── penalty/             # 페널티
    │   └── account/             # 회계
    └── global/
        ├── auth/                # JWT, OAuth2, @CurrentUser
        ├── config/              # Spring Configuration
        └── common/              # 공통 유틸, 응답 포맷

각 도메인 내부 구조:
domain/{name}/
├── application/
│   ├── dto/request/
│   ├── dto/response/
│   ├── mapper/                  # 수동 Mapper (MapStruct 대체)
│   ├── usecase/command/         # 상태 변경 UseCase
│   ├── usecase/query/           # 읽기 전용 QueryService
│   ├── exception/               # {Domain}ErrorCode enum
│   └── validator/
├── domain/
│   ├── entity/                  # JPA Entity (비즈니스 로직 포함)
│   ├── vo/                      # Value Object (@Embeddable, value class)
│   ├── enums/
│   ├── repository/              # JpaRepository + Reader 인터페이스
│   ├── port/                    # 외부 시스템 추상화 인터페이스
│   └── service/                 # 다중 엔티티 로직만 (얇은 래퍼 금지)
├── infrastructure/              # Port 구현체
└── presentation/
    └── {Domain}Controller.kt
```

## 💻 개발 가이드

### 코드 포맷팅

```bash
# 자동 포맷 (커밋 전 필수)
./gradlew ktlintFormat

# 검사만 수행
./gradlew ktlintCheck
```


### Git 컨벤션

#### 브랜치 네이밍
```
feat/{TICKET}-description      # 예: feat/WTH-123-user-login
fix/{TICKET}-description       # 예: fix/WTH-456-token-expiry
refactor/{TICKET}-description
```

#### 커밋 메시지
```
type: message

예시:
feat: Add user authentication
fix: Resolve null pointer in UserService
refactor: Extract validation logic to Entity
test: Add UserUseCase integration tests
```

## 🧪 테스트

### 실행

```bash
# 전체 테스트
./gradlew test

# 패턴 매칭
./gradlew test --tests "*UseCaseTest"

# 특정 클래스
./gradlew test --tests "CreateUserUseCaseTest"
```

## 🐳 Docker

```bash
# 개발 환경 빌드
docker build -f Dockerfile-dev -t weeth-server:dev .

# 운영 환경 빌드
docker build -f Dockerfile-prod -t weeth-server:prod .
```


## 📝 주요 기능

### 인증/인가
- JWT 기반 인증 (대칭키, JJWT 0.13.0)
- OAuth2 소셜 로그인 (Kakao, Apple)
- Access Token / Refresh Token

### 도메인 기능
- **User**: 사용자 관리, 소셜 로그인, 프로필 관리
- **Attendance**: 출석 체크, 출석 기록 조회
- **Schedule**: 일정 생성/조회/관리 (Event, Meeting)
- **Board**: 게시판, 게시글 CRUD (Notice, Post)
- **Comment**: 댓글 CRUD, 대댓글
- **File**: 파일 업로드 (AWS S3), 이미지 관리
- **Penalty**: 페널티 관리
- **Account**: 회계 관리, 영수증 관리


## 📄 라이선스

Copyright © 2024 Weeth Team
