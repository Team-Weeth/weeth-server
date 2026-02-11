---
name: database-manage
description: DB schema inspection and management. Use when asked to "show schema", "show tables", "check DB", "DB context", "database info", "스키마 덤프", "엔티티 구조", "테이블 확인", "스키마 확인".
allowed-tools: Read, Glob, Grep, Bash, Task
---

# Database Manage

Inspect DB schema, analyze entity structures, and dump live schema from MySQL.
**All output MUST be written in Korean (한국어).**

## Schema Information Sources (Priority Order)

1. **Schema snapshot** - Read `references/schema.md` if it exists (fastest)
2. **Schema dump script** - Dump live schema from DB (`scripts/dump-schema.sh`)
3. **Code-based analysis** - Scan entity/repository files (no DB connection required)

## Instructions

### Step 1: Check Schema Snapshot

Check if an existing snapshot is available.

```
Read: .claude/skills/database-manage/references/schema.md
```

- If file exists → respond based on this file
- If file is missing or outdated → proceed to Step 2 or Step 3

### Step 2: Dump Live Schema from DB

If local MySQL is running, use the script to fetch the latest schema.

```bash
# Pass connection info via environment variables
DB_HOST=localhost DB_PORT=3306 DB_USER=root DB_PASSWORD=<password> DB_NAME=weeth \
  .claude/skills/database-manage/scripts/dump-schema.sh

# Or pass via arguments
.claude/skills/database-manage/scripts/dump-schema.sh -h localhost -P 3306 -u root -p <password> -d weeth
```

**IMPORTANT**: Never guess passwords. Always ask the user or read from environment variables.

Script output:
- Saves full schema to `references/schema.md`
- Includes table list, columns, indexes, FK relationships

### Step 3: Code-Based Analysis (When DB Is Unavailable)

When DB connection is not possible, analyze from code only.

#### 3-1. Check Project DB Configuration
- Use `Glob` to search `**/application*.{yml,yaml,properties}`
- Check datasource URL, driver, dialect
- Check ddl-auto setting, migration tool configuration

#### 3-2. Analyze Entity Structure
- Use `Grep` to find files with `@Entity` annotation
- Analyze fields, relationships (@OneToMany, @ManyToOne, etc.), indexes per entity
- Check BaseEntity inheritance structure
- Check `@Table(name = "...")` mappings

#### 3-3. Analyze Repositories
- Use `Grep` to search for `JpaRepository`, `@Query`, `@Lock`
- Check custom query methods

#### 3-4. Check Migration Files
- Use `Glob` to search `**/db/migration/**/*.sql`

## Script Details

### dump-schema.sh

Queries MySQL `INFORMATION_SCHEMA` and saves results to `references/schema.md`.

**Output includes:**
- Table list (engine, row count, comments)
- Column details per table (type, nullable, key, default, extra)
- Index info (columns, uniqueness, type)
- FK relationships (referenced table/column)
- Relationship diagram (text-based)

**Connection info methods:**
1. Environment variables: `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`
2. Arguments: `-h host -P port -u user -p password -d database`
3. Defaults: `localhost:3306`, user=`root`, db=`weeth`

## Analysis Commands by Topic

### List All Entities

```
Grep: pattern="@Entity" → get file list
Read each file → extract fields/relationships
```

### Inspect Specific Table

```
Grep: pattern="class {EntityName}" → find entity file
Read → check all fields, relationships, indexes, constraints
Grep: pattern="{EntityName}" in Repository files → find related queries
```

### Relationship Map

```
Grep: pattern="@(OneToMany|ManyToOne|OneToOne|ManyToMany)" → map all relationships
```

### Check Indexes

```
Grep: pattern="@Index|@Table.*indexes" → find index definitions
```

## Output Format

### Full Schema Summary

```markdown
# DB 스키마 요약

## DB 설정
- **DB 종류**: MySQL 8.0
- **DDL 전략**: validate
- **마이그레이션**: Flyway / 없음

## 엔티티 목록

| 엔티티 | 테이블명 | 주요 필드 | 관계 |
|--------|----------|-----------|------|
| User | users | id, name, email, role | Profile(1:1), Post(1:N) |
| Post | posts | id, title, content, userId | User(N:1), Comment(1:N) |

## 관계 다이어그램 (텍스트)

User ──1:1──> Profile
User ──1:N──> Post
Post ──1:N──> Comment
Comment ──N:1──> User

## 인덱스

| 테이블 | 인덱스명 | 컬럼 | 유니크 |
|--------|----------|------|--------|
| users | idx_user_email | email | Yes |
```

### Specific Entity Detail

```markdown
# User 엔티티 상세

## 기본 정보
- **클래스**: `com.example.domain.user.entity.User`
- **테이블**: `users`
- **상속**: `BaseEntity` (createdAt, updatedAt)

## 필드

| 필드 | 컬럼 | 타입 | 제약조건 |
|------|------|------|----------|
| id | id | Long | PK, AUTO_INCREMENT |
| name | name | String | NOT NULL, max=100 |
| email | email | String | NOT NULL, UNIQUE |

## 관계

| 타입 | 대상 | 매핑 | Fetch |
|------|------|------|-------|
| @OneToMany | Post | mappedBy="user" | LAZY |

## Repository 쿼리

| 메서드 | 설명 |
|--------|------|
| findByEmail(email) | 이메일로 조회 |
```

## Examples

### Example: Dump Schema
User says: "DB 스키마 덤프해줘" / "스키마 업데이트해줘"
Actions:
1. Ask user for DB connection info
2. Run `scripts/dump-schema.sh`
3. Verify `references/schema.md` was created
Result: Latest schema saved to references/schema.md

### Example: View Full Schema
User says: "DB 스키마 보여줘" / "테이블 구조 확인해줘"
Actions:
1. Check if references/schema.md exists → if yes, output directly
2. If not → scan entities from code
3. Output schema summary
Result: Full table list, relationships, index info displayed

### Example: Inspect Specific Entity
User says: "User 엔티티 구조 알려줘"
Actions:
1. Search and read the entity file
2. Check related Repository queries
3. Output detailed info
Result: Entity fields, relationships, indexes, Repository queries displayed

### Example: Analyze Relationships
User says: "엔티티 관계 보여줘" / "ERD 그려줘"
Actions:
1. Check Relationships section in references/schema.md
2. If not available, search @OneToMany etc. annotations in code
3. Output text ERD
Result: Full entity relationship diagram displayed

## Rules
- **All output in Korean (한국어)**
- Analyze based on actual code/DB only (never guess)
- Notify user if no entity files are found
- **Never guess passwords** - always ask the user directly
- Mask sensitive info (passwords, connection strings) in output
- Recommend adding references/schema.md to .gitignore (may contain connection info)