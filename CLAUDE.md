# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weeth Server is a community platform backend built with Spring Boot 3.5.10. The codebase has completed **Java → Kotlin migration** — all code is Kotlin.

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
Each of the 10 domains (`user`, `attendance`, `session`, `schedule`, `board`, `comment`, `file`, `penalty`, `account`, `cardinal`) follows:
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
    ├── {Domain}Controller.kt
    └── {Domain}ResponseCode.kt
```

### Key Patterns
- **UseCase = orchestration only** — business logic lives in Entities (Rich Domain Model)
- **No thin wrapper services** — UseCases call Repositories directly, no GetService/SaveService
- **Port-Adapter** — domain owns Port interfaces, infrastructure implements them
- **Cross-domain reads** via Reader interfaces; cross-domain writes via Repository directly
- **`@Transactional` on UseCase only** — Domain Services have no transaction annotations

### Response Format
All API responses wrapped in `CommonResponse<T>` with code/message/data. 5-digit code format `XDDNN`: X=category (1=Success, 2=Domain Error, 3=Infra Error, 4=Client Error), DD=domain ID, NN=sequence. See `.claude/rules/api-design.md` for full domain ID mapping and code ranges.

### Authentication
JWT with symmetric key (JJWT 0.13.0), OAuth2 via Kakao and Apple. `@CurrentUser` annotation injects authenticated user ID into controller methods.

## Testing

- **Kotest** (DescribeSpec default, BehaviorSpec for BDD, StringSpec for simple logic)
- **MockK** + **springmockk** for mocking
- **Testcontainers** for MySQL integration tests
- **Fixture pattern**: `{Entity}TestFixture` objects with factory methods in `fixture/` directories
- Test architecture mirrors source: mock Repository/Reader/Port in UseCase tests, mock Port (not adapter) in application tests

## Kotlin Migration Status

**✅ Complete** — 305 Kotlin files (100%)

- Java → Kotlin migration fully complete
- Lombok and MapStruct dependencies removed
- All 13 mappers migrated to manual `@Component` Mapper classes (see `.claude/rules/mapper-dto.md`)
- Entity fields use `private set` for Rich Domain Model pattern (see architecture.md)
- OSIV disabled: `spring.jpa.open-in-view: false` in `application.yml`

## Kotlin Conventions

- Use `?.`, `?:`, `requireNotNull` — avoid `!!`
- Entities: regular `class` (not `data class`); DTOs: `data class`
- Entity setters: `private set` to enforce business logic via named methods
- Example:
  ```kotlin
  var name: String
      private set

  fun updateName(newName: String) {
      require(newName.isNotBlank()) { "Name cannot be empty" }
      this.name = newName
  }
  ```

## Detailed Rules

Architecture, code style, testing, API design, exception handling, transactions, and git conventions are documented in `.claude/rules/`. Refer to those files for comprehensive guidance on each topic.
