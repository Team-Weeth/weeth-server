# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weeth Server is a community platform backend built with Spring Boot 3.5.10. All code is Kotlin (Java → Kotlin migration complete; Lombok/MapStruct removed — do not reintroduce them).

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

**Profiles:** `local` (default dev), `local-monitoring` (local + monitoring stack), `dev` (dev server, ddl-auto: update), `prod` (Swagger disabled, ddl-auto: validate)

## Architecture

```
presentation → application → domain ← infrastructure
```

13 domains (`user`, `attendance`, `session`, `schedule`, `board`, `comment`, `file`, `penalty`, `account`, `cardinal`, `club`, `dashboard`, `university`), each following the package layout in `.claude/rules/architecture.md`.

Core principles (details in the rule files):
- **Rich Domain Model** — business logic lives in Entities; UseCase = orchestration only
- **No thin wrapper services** — UseCases call Repositories directly
- **Port-Adapter** — domain owns Port interfaces, infrastructure implements them
- **Cross-domain reads** via Reader interfaces; **`@Transactional` on UseCase only**

### Authentication
JWT with symmetric key (JJWT), OAuth2 via Kakao and Apple. `@CurrentUser` annotation injects authenticated user ID into controller methods.

### Notable Settings
- OSIV disabled: `spring.jpa.open-in-view: false` in `application.yml`
- All API responses wrapped in `CommonResponse<T>`; 5-digit code format `XDDNN` — see `.claude/rules/api-design.md`

## Testing

Kotest + MockK + springmockk + Testcontainers. Conventions, fixture pattern, and mock lifecycle rules are in `.claude/rules/testing.md`.

## Detailed Rules

Architecture, code style, testing, API design, exception handling, transactions, and git conventions are documented in `.claude/rules/` — these are loaded automatically and are the single source of truth. `.agents/rules` (Codex) is a symlink to `.claude/rules`; edit only `.claude/rules`.
