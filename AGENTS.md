# AGENTS.md

Codex instructions for this repository.

## Project Overview

Weeth Server is a Spring Boot 3.5.10 backend for a community platform.
The Java to Kotlin migration is complete; treat Kotlin as the only production language.

## Communication

- Reply in Korean unless the user explicitly asks for another language.
- Keep implementation notes concise and concrete.
- When changing code, explain the affected files and verification result.

## Build And Verification

Use these commands from the repository root:

```bash
./gradlew clean build
./gradlew test
./gradlew test --tests "*UseCaseTest"
./gradlew test --tests "CreateUserUseCaseTest"
./gradlew ktlintFormat
./gradlew ktlintCheck
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Prerequisites: JDK 21, MySQL 8.0, Redis 7.0+, and environment variables configured outside source control.

After Kotlin edits, run `./gradlew ktlintFormat` when practical. For behavioral changes, run the narrowest relevant test first, then a broader test/build when risk warrants it.

## Sensitive Files

Do not create or edit secrets without explicit user approval:

- `.env*`
- `*.pem`
- `*.key`
- files containing `secret` or `credential` in the name
- production or development application config files under `src/main/resources/application-prod*` or `src/main/resources/application-dev*`

## Architecture

The dependency direction is:

```text
presentation -> application -> domain <- infrastructure
```

Core rules:

- UseCase classes orchestrate only; business rules belong in entities or domain services.
- Do not create thin wrapper services for simple repository delegation.
- Put `@Transactional` on UseCase methods, not on domain services.
- Same-domain access uses repositories directly.
- Cross-domain reads use Reader interfaces.
- Cross-domain writes use repositories directly only when the same transaction is required; otherwise consider domain events.
- Domain owns Port interfaces; infrastructure implements adapters.

## Rule Files

Detailed project rules live in `.agents/rules/` (a symlink to `.claude/rules/` — the single source of truth; edit files there). Read the relevant file before making changes in that area:

- API and response codes: `.agents/rules/api-design.md`
- Package structure and layer rules: `.agents/rules/architecture.md`
- Kotlin conventions: `.agents/rules/code-style.md`
- Exceptions and error codes: `.agents/rules/exception-handling.md`
- Commit and branch conventions: `.agents/rules/git-conventions.md`
- Mapper and DTO patterns: `.agents/rules/mapper-dto.md`
- Test style and fixtures: `.agents/rules/testing.md`
- Transactions and concurrency: `.agents/rules/transaction-concurrency.md`

## Testing

- Use Kotest as the default Kotlin test framework.
- Use MockK and springmockk for mocks.
- Use Testcontainers for integration tests that require MySQL or external infrastructure.
- Test structure should mirror source structure.
- Shared fixtures belong under `src/test/kotlin/com/weeth/domain/{domain}/fixture/`.

For shared MockK mocks in `DescribeSpec`, clear mocks in `beforeTest` and restub defaults as needed.

## Kotlin Conventions

- Avoid `!!`; prefer safe calls, Elvis, `requireNotNull`, or `checkNotNull`.
- Entities are regular `class` types, not `data class`.
- DTOs are `data class` types.
- Entity state should be mutated through named business methods with `private set` on mutable properties.
- Manual `@Component` mapper classes are used; do not introduce MapStruct.

## Codex Skills

Project-specific skills live in `.claude/skills/` as the single source of truth.
`.agents/skills` is a symlink to `.claude/skills` so Codex can discover the same skills without a separate install step.
