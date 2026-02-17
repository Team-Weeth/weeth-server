# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weeth Server is a community platform backend built with Spring Boot 3.5.10. The codebase is in active **Java → Kotlin migration** — new code should be written in Kotlin, while ~271 Java files remain. Lombok and MapStruct are temporary dependencies being phased out in favor of Kotlin idioms and manual mappers.

## Build & Development Commands

```bash
./gradlew clean build                    # Full build
./gradlew test                           # Run all tests
./gradlew test --tests "*UseCaseTest"    # Run tests by pattern
./gradlew test --tests "CreateUserUseCaseTest"  # Run specific test class
./gradlew ktlintFormat                   # Auto-format with ktlint
./gradlew ktlintCheck                    # Check formatting only
./gradlew bootRun                        # Run locally (default profile)
./gradlew bootRun --args='--spring.profiles.active=dev'  # Run with specific profile
```

**Prerequisites:** JDK 21, MySQL 8.0, Redis 7.0+, environment variables configured in `.env`

**Profiles:** `local` (default dev), `dev` (dev server, ddl-auto: update), `prod` (Swagger disabled, ddl-auto: validate), `test`

## Architecture

### Layer Structure
```
presentation → application → domain ← infrastructure
```
- **presentation/**: Controllers, ResponseCode enums
- **application/**: UseCase (command/query), DTOs, Mappers, Exceptions, Validators
- **domain/**: Entities (Rich Domain Model), VO, Enums, Repositories, Ports, Domain Services
- **infrastructure/**: Port implementations (Adapters for S3, external APIs, etc.)

### Domain Package Layout
Each of the 8 domains (`user`, `attendance`, `schedule`, `board`, `comment`, `file`, `penalty`, `account`) follows:
```
domain/{name}/
├── application/
│   ├── dto/request/, dto/response/
│   ├── mapper/
│   ├── usecase/command/    # @Transactional, state-changing
│   ├── usecase/query/      # @Transactional(readOnly=true), returns DTOs
│   └── exception/          # {Domain}ErrorCode enum + exception classes
├── domain/
│   ├── entity/             # JPA entities with business logic
│   ├── enums/
│   ├── repository/         # JpaRepository + Reader interfaces
│   ├── port/               # Interfaces for external systems
│   └── service/            # Multi-entity logic only (no thin wrappers)
├── infrastructure/         # Port implementations
└── presentation/
    ├── {Domain}Controller.java
    └── {Domain}ResponseCode.java
```

### Key Patterns
- **UseCase = orchestration only** — business logic lives in Entities (Rich Domain Model)
- **No thin wrapper services** — UseCases call Repositories directly, no GetService/SaveService
- **Port-Adapter** — domain owns Port interfaces, infrastructure implements them
- **Cross-domain reads** via Reader interfaces; cross-domain writes via Repository directly
- **`@Transactional` on UseCase only** — Domain Services have no transaction annotations

### Response Format
All API responses wrapped in `CommonResponse<T>` with code/message/data. Success codes use `ResponseCodeInterface` enums (1xxx range), error codes use `ErrorCodeInterface` enums (2xxx domain errors, 3xxx server, 4xxx client).

### Error Code Ranges

| Domain | Success | Error |
|--------|---------|-------|
| Account | 11xx | 21xx |
| Attendance | 12xx | 22xx |
| Board | 13xx | 23xx |
| Comment | 140xx | 240x |
| File | 15xx | 25xx |
| Penalty | 160xx | 260x |
| Schedule | 17xx | 27xx |
| User | 18xx | 28xx |
| JWT (Global) | — | 29xx |

### Authentication
JWT with symmetric key (JJWT 0.13.0), OAuth2 via Kakao and Apple. `@CurrentUser` annotation injects authenticated user ID into controller methods.

## Testing

- **Kotest** (DescribeSpec default, BehaviorSpec for BDD, StringSpec for simple logic)
- **MockK** + **springmockk** for mocking
- **Testcontainers** for MySQL integration tests
- **Fixture pattern**: `{Entity}TestFixture` objects with factory methods in `fixture/` directories
- Test architecture mirrors source: mock Repository/Reader/Port in UseCase tests, mock Port (not adapter) in application tests

## Kotlin Migration Notes

- New code: Kotlin. Existing Java code migrated incrementally.
- Replace Lombok with Kotlin data classes/properties
- Replace MapStruct with manual `@Component` Mapper classes (see `.claude/rules/mapper-dto.md`)
- Use `?.`, `?:`, `requireNotNull` — avoid `!!`
- Entities: regular `class` (not `data class`); DTOs: `data class`

## Detailed Rules

Architecture, code style, testing, API design, exception handling, transactions, git conventions, and logging rules are documented in `.claude/rules/`. Refer to those files for comprehensive guidance on each topic.
