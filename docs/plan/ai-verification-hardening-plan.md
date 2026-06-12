# AI 검증 강화 계획 — 생성자-평가자 분리 + 결정론적 가드레일

> 참고 자료:
> - [anthropics/cwc-long-running-agents](https://github.com/anthropics/cwc-long-running-agents) — 생성자-평가자 분리, default-FAIL 계약, 훅 가드레일
> - [Augment Code: Spec-Driven Development](https://www.augmentcode.com/guides/claude-code-spec-driven-development) — 검증 가능한 완료 기준, 인프라 기반 강제
> - [Josh McDonald: SDD with Claude Code](https://joshmcdonald.medium.com/running-a-small-team-on-a-big-project-spec-driven-development-with-claude-code-9a1b97f58551) — 훅 4종 + 다단계 리뷰 파이프라인

## 1. 검토 결론

### 현재 상태 (이미 갖춘 것)

| 영역 | 현재 구현 | 비고 |
|------|----------|------|
| 포맷/린트 훅 | `ktlint-format.sh` (PostToolUse, exit 2 피드백) | 결정론적 가드레일 ①호 — 이미 운영 중 |
| 하네스 무결성 | `check-harness.sh` (CI 게이트) | 포인터 드리프트 결정론적 검증 |
| 타입 체크 CI 게이트 | `gradlew clean test` (CI) | Kotlin 정적 타입 → 환각 시그니처는 컴파일 단계에서 이미 차단 |
| 개발 워크플로 | 메인 세션 + 룰/스킬/훅 (빌더 에이전트 2종은 거의 미사용) | **구현한 세션이 자기 컨텍스트로 리뷰·완료 보고 (self-grading)** |

### 평가

**① 생성자-평가자 분리** — **도입 가치 높음. 단, 메인 세션 워크플로에 맞춘다.**
실제 개발은 빌더 서브에이전트가 아니라 **메인 세션에서 룰 + 스킬 + 훅으로**
이루어진다. 즉 self-grading 문제의 실제 발생 지점은 "구현한 메인 세션이 자기
컨텍스트로 리뷰하고 완료를 보고하는 것"이다. 평가자의 핵심 가치(깨끗한 컨텍스트,
쓰기 권한 없음)는 서브에이전트 형태여야만 얻을 수 있으므로 **평가자만 유일한
서브에이전트로 정의**하고, 호출은 메인 세션이 스킬을 통해 하도록 설계한다.
기존 빌더 에이전트(feature-developer / system-architect)는 거의 사용하지 않으므로
이번 계획의 연결 대상에서 제외한다.

**① default-FAIL 계약 (test-results.json + verify-gate 훅)** — **축소 도입.**
원본 패턴은 무인 장시간 루프(밤새 자율 실행)용이다. 이 프로젝트는 대화형 작업 +
사람 PR 리뷰가 기본이므로 파일 기반 계약 + PreToolUse 증거 게이트의 전체 기계장치는
유지 비용 > 효과. 대신 **"평가자만 PASS를 선언할 수 있다"는 역할 계약**과
**평가자 프롬프트의 default-FAIL 자세**(모든 기준은 불통과로 시작, Read로 확인한
증거로만 통과 전환)로 핵심 효과를 가져온다. 전체 기계장치는 자율 루프를 도입할 때
재검토 (4단계 참조).

**② 결정론적 훅 가드레일** — **이미 절반 도입됨. 남은 최대 공백은 아키텍처 규칙.**
글에서 말하는 "타입 체크 CI 게이트"는 Kotlin + 기존 CI로 이미 충족된다.
이 프로젝트에서 진짜 프롬프트로만 강제되고 있는 것은 **아키텍처 규칙**
(계층 의존성, `@Transactional` 위치, 래퍼 금지 등)이다. 이를 Konsist 테스트로
변환하면 기존 `gradlew test` CI 게이트에 그대로 올라타는, 모델이 건너뛸 수 없는
결정론적 게이트가 된다. 훅보다 테스트가 이 규칙들의 올바른 강제 지점이다.

**스킵하는 것**: Spec Gate 훅(스펙 없는 쓰기 차단), Haiku 완료 체크 Stop 훅,
스펙 인덱스 운영 — 팀 병렬 작업용 오버헤드로 현재 규모에 비해 과함.

## 2. 단계별 계획

### 1단계 — 평가자 에이전트 + 호출 스킬 (ROI 최고, 비용 최저)

메인 세션(생성자)이 구현을 마치면 스킬을 통해 평가자 서브에이전트를 스폰하는 구조.
생성자와 평가자가 "세션 vs 서브에이전트"로 분리되어 컨텍스트 오염 없이 채점된다.

```text
메인 세션 (생성자: 룰+스킬+훅으로 구현)
   │  작업 시작 시: 태스크 파일 작성 (사용자 요청 원문 + 수용 기준)
   │  구현 완료 → verify-implementation 스킬 (완료 보고 전 필수)
   │     스폰 전: git diff 해시 기록
   ▼
implementation-evaluator 서브에이전트 (평가자: 깨끗한 컨텍스트)
   │  - 요구사항은 태스크 파일에서만 도출 (빌더 요약 아님)
   │  - 읽기 전용: frontmatter PreToolUse 훅으로 Bash allowlist 강제
   │  스폰 후: diff 해시 재확인 — 불일치 시 판정 무효
   │  PASS → 판정 인용해 완료 보고 + 메트릭 로그 append
   │  NEEDS_WORK → findings 반영 후 새 평가자로 전체 기준 재채점 (최대 2회)
   ▼
사람 리뷰 (PR)
```

**평가 게이트 — 모든 작업에 돌리지 않는다** (토큰/시간 과부하 방지)

평가 1회 비용 = opus 서브에이전트 스폰(룰 + diff + 테스트 출력, 수만 토큰) +
테스트 실행 수 분, NEEDS_WORK 시 ×2~3. 간단한 작업에 무조건 돌면 검증 비용이
작업 비용을 압도한다. Augment 가이드도 같은 원리로 "단일 파일 수정엔 SDD 스킵"을
권고. **태스크 파일 존재 여부를 평가 발동 게이트로 사용한다** — 평가자가 어차피
태스크 파일 없이는 기준 1을 채점할 수 없으므로, 두 메커니즘이 자연스럽게 맞물린다.

| 구분 | 기준 | 검증 경로 |
|------|------|----------|
| **평가 대상** (태스크 파일 작성 → 완료 시 평가 자동 발동) | 새 기능/엔드포인트, 도메인 로직 변경, 동작이 바뀌는 리팩토링, 다중 파일 변경 | 태스크 파일 → 구현 → verify-implementation |
| **평가 생략** (태스크 파일 없음 → 평가 미발동) | 문서/주석, 설정값, 오타·단일 라인 수정, 테스트만 수정, 포맷 정리 | 기존 결정론 게이트로 충분: ktlint 훅 + 컴파일 + CI (+2단계 후 Konsist) |

- 경계가 애매하면 빌더가 상향 적용(태스크 파일 작성), 사용자는 언제든
  `/verify-implementation`으로 수동 발동 가능
- 생략 결정도 메트릭 로그에 남긴다(`tier: skipped`) — 임계값이 너무 느슨한지
  (생략된 작업에서 버그가 PR 리뷰에 잡히는지) 데이터로 검증

**산출물 0**: 태스크 파일 — 기준 1의 진실 공급원 (`.claude/tasks/current-task.md`)

평가자는 깨끗한 컨텍스트에서 시작하므로 대화의 요구사항을 모른다. 빌더가 "이런
작업이었다"고 요약해 넘기면 자기가 구현한 범위에 맞춰 요구사항을 유리하게
프레이밍하게 되어(악의가 아니라 구조적으로) 자기 채점이 뒷문으로 돌아온다.

- **작성 시점**: 작업 시작(계획) 단계 — 구현 후 소급 작성 금지. 사후 프레이밍을
  줄이는 핵심은 "구현 전에 쓴다"는 시점이다.
- **내용**: ① 사용자 요청 **원문 인용**(요약 아님) ② 수용 기준 목록(검증 가능한
  문장으로) ③ 명시적 제외 범위
- **평가자 계약**: "기준 1의 요구사항은 태스크 파일에서 도출한다. 빌더의 주장이나
  스폰 프롬프트의 요약에서 도출하지 않는다. 태스크 파일이 없으면 기준 1은 채점
  불가 = 불통과."
- 룰 레이어(아래 역할 계약)에 "평가 대상 규모의 작업은 시작 시 태스크 파일을 먼저
  쓴다"를 추가. 강제 훅 없이, 어차피 계획 단계에서 만들어지는 아티팩트를 평가자
  입력으로 지정하는 것뿐이다.

**산출물 1**: `.claude/agents/implementation-evaluator.md`

```yaml
---
name: implementation-evaluator
description: "구현 결과를 깨끗한 컨텍스트에서 채점. 쓰기 권한 없음. PASS / NEEDS_WORK 판정."
tools: Read, Glob, Grep, Bash   # Edit/Write/Task 없음
model: opus
hooks:
  PreToolUse:
    - matcher: Bash
      hooks:
        - type: command
          command: "$CLAUDE_PROJECT_DIR/.claude/hooks/evaluator-bash-allowlist.sh"
---
```

**읽기 전용은 프롬프트 약속이 아니라 구조로 강제한다** (피드백 반영):
- `evaluator-bash-allowlist.sh` (PreToolUse 훅): `git diff` / `git log` / `git status` /
  `git show` / `./gradlew test*` / `./gradlew compileKotlin` / `./gradlew compileTestKotlin`
  만 통과, 그 외 Bash는 deny. 현실적 실패 모드는 평가자가 발견한 문제를 "친절하게
  직접 고쳐버리는" 것 — 이 순간 분리가 조용히 무너지므로 훅으로 차단한다.
- **2차 방어선 (diff 해시 체크)**: 인자 패턴 매칭은 옵션 순서·변수 확장 변형에
  취약하므로, 오케스트레이터(스킬)가 평가자 스폰 **전후로 `git diff | sha256sum`
  (+ `git status --porcelain` 해시)을 비교**한다. 불일치 → "평가자가 트리를
  건드렸다" = 판정 무효, 사용자에게 보고. 이로써 "평가자가 트리를 건드리지
  않았다"가 믿음이 아니라 검증 가능한 사실이 된다.

프롬프트 핵심 요소:
- **default-FAIL 계약**: 모든 완료 기준은 `불통과`로 시작한다. Read/Bash로 직접
  확인한 증거(코드 라인, 테스트 출력, 빌드 결과)가 있을 때만 `통과`로 바꾼다.
  "구현했다고 적혀 있음"은 증거가 아니다.
- **채점 기준** (각각 증거 필수):
  1. 요구사항 충족 — diff가 **태스크 파일의 수용 기준**을 실제 구현하는가
     (태스크 파일 없으면 채점 불가 = 불통과)
  2. `.claude/rules/architecture.md` 준수 — 계층 의존성, UseCase 책임, Entity 로직 위치
     (※ 2단계 Konsist 도입 후에는 "인코딩 안 된 의미적 잔여물"로 축소 — 아래 2단계 참조)
  3. 테스트 — 영향 범위 한정 실행(`./gradlew test --tests "..."` 또는 증분 test) +
     **테스트가 의미 있는지** 채점: 전부 모킹으로 우회하지 않는지, 실패 경로를
     검증하는지, 단언이 실제 동작을 검증하는지. 결정론적 게이트가 못 하는 일이고
     평가자에 opus를 쓰는 비용이 정당화되는 지점. 전체 스위트는 CI가 백스톱.
  4. API 계약 — `CommonResponse`, `@ApiErrorCodeExample`, 에러 코드 체계 (해당 시)
- **출력 형식** (한글):
  ```
  판정: PASS | NEEDS_WORK
  기준별 결과: [기준] 통과/불통과 — 증거: <파일:라인 또는 명령 출력 요약>
  NEEDS_WORK 시: 구체적 미비점 목록 (다음 빌더 세션의 입력이 됨)
  ```
- 스폰 프롬프트로 전달받는 것: diff 기준점(커밋/브랜치) + **태스크 파일 경로**.
  요구사항 요약은 전달하지 않는다 — 요구사항의 유일한 출처는 태스크 파일이다.

**산출물 2**: `.claude/skills/verify-implementation/SKILL.md` (호출 진입점)

- **트리거 description**: "태스크 파일이 있는 구현 작업을 마치고 완료를 보고하기 전,
  또는 '검증해줘'·'평가해줘' 요청 시 사용. 태스크 파일 없는 경미한 작업(문서·설정·
  단일 라인 수정)에는 자동 발동하지 않는다" → 평가 게이트 표와 일치
- 스킬 내용:
  1. 태스크 파일 존재 확인 — 자동 발동 경로에서는 이미 존재(게이트 조건).
     수동 발동인데 없으면 사용자 요청 원문을 인용해 작성한 뒤 진행
  2. 평가 범위 결정 — `git diff` 기준점(직전 커밋 또는 작업 시작 지점) 확정 후
     **스폰 전 해시 기록**: `git diff | sha256sum` + `git status --porcelain | sha256sum`
  3. Agent 툴로 `implementation-evaluator` 스폰 — 전달: diff 기준점 + 태스크 파일
     경로 (요구사항 요약은 전달 금지)
  4. **스폰 후 해시 재확인** — 불일치 시 판정 무효화, 사용자 보고
  5. `NEEDS_WORK` → findings 반영 후 재평가. **재평가 컨텍스트 위생**: 매번 새로
     스폰된 평가자가 **전체 기준을 처음부터 재채점**한다 (수정 과정에서 생긴 다른
     기준의 회귀를 잡기 위해). 이전 findings는 "이것들이 고쳐졌는지 특히 확인하라"는
     부록으로만 전달. 최대 2회, 실패 시 사용자 에스컬레이션.
  6. 완료 보고에 평가자 판정·기준별 증거를 인용
  7. **메트릭 로그 append** — `.claude/metrics/eval-log.jsonl`에 한 줄:
     `{date, task, first_verdict, failed_criteria, retry_count}`. 한 달치가 쌓이면
     1차 PASS율(기준 강도 보정), 평가자가 놓치고 사람 PR 리뷰에서 잡힌 이슈
     (이스케이프율), opus→sonnet 전환 판단, 3단계 도입 여부가 전부 데이터로 결정된다.
- 기존 `code-review` 스킬과의 역할 구분: code-review는 "버그·취약점 탐지"(리뷰어 관점),
  verify-implementation은 "완료 여부 판정"(default-FAIL 채점) — 대체가 아니라 별개 게이트

**역할 계약 명시** (룰 레이어):
- `CLAUDE.md` 또는 `.claude/rules/`에 계약 추가:
  1. **"기능 구현·도메인 로직 변경·동작이 바뀌는 리팩토링 규모의 작업은 시작 시
     태스크 파일(사용자 요청 원문 + 수용 기준)을 먼저 쓴다."** (평가 게이트 표의
     "평가 대상" 기준과 동일 — 경미한 작업은 해당 없음)
  2. **"태스크 파일이 있는 작업의 완료 보고는 verify-implementation 평가 결과를
     인용한다. 생성자(메인 세션)는 스스로 PASS를 선언하지 않는다."**
- `check-harness.sh` 무결성 검사에 새 평가자 에이전트 파일·스킬·훅 스크립트 참조
  포인터가 포함되는지 확인 (스킬 이름 참조 검사는 기존 3번 항목이 커버, 훅 스크립트는
  5번 항목 패턴에 `.claude/hooks/` 추가 필요 여부 점검)
- 기존 빌더 에이전트 2종은 수정하지 않는다(미사용). 추후 다시 쓰게 되면 각 워크플로의
  리뷰 단계에서 같은 스킬을 호출하도록 한 줄만 바꾸면 된다 — 진입점을 스킬로 단일화한
  이유.

**도입 결과 (2026-06-12)**: 전 산출물 구현 완료. 스모크 테스트(WTH-390 커밋을 소급
태스크 파일로 채점, headless `claude --agent implementation-evaluator -p`):
- 출력 형식·default-FAIL·file:line 증거 인용 모두 계약대로 동작, 판정 PASS
- frontmatter PreToolUse 훅 발동 확인 (`git --no-pager`·파이프 차단 → 읽기성
  `git --no-pager` 변형은 allowlist에 정규화 추가)
- 스폰 전후 diff/status 해시 일치 — 트리 불변 검증 성공
- 관찰: headless 모드에서 일부 git 명령이 권한 단계에서 거부되어 평가자가 파일
  직접 Read로 우회함 (판정 품질에는 영향 없었음). 세션 내 Agent 툴 스폰 경로에서
  재확인 필요 — 새 에이전트는 다음 세션부터 레지스트리에 등록됨.

### 2단계 — Konsist 아키텍처 테스트 (결정론적 게이트)

**산출물**: `build.gradle.kts` 의존성 + `src/test/kotlin/com/weeth/architecture/ArchitectureTest.kt`

```kotlin
testImplementation("com.lemonappdev:konsist:0.17.3")  // Kotlin 2.1 호환
```

Kotest `StringSpec`으로 작성 (기존 스택 일치). 인코딩할 규칙 — 전부
`.claude/rules/architecture.md`에서 그대로 옮긴다:

| # | 규칙 | Konsist 검사 |
|---|------|-------------|
| 1 | application은 infrastructure를 import하지 않는다 | **architecture assertion (Layer DSL)** — import 블랙리스트보다 의도가 코드에 드러나고 유지보수 쉬움 |
| 2 | domain은 application/presentation/infrastructure를 import하지 않는다 | **architecture assertion (Layer DSL)** — 규칙 1과 함께 계층 정의 1곳에서 관리 |
| 3 | `@Transactional`은 `usecase` 패키지 클래스에만 | 어노테이션 위치 검사 (domain/service 금지 포함) |
| 4 | Command UseCase는 `usecase/command`에 `*UseCase` 네이밍 | 위치+네이밍 검사 |
| 5 | QueryService는 `usecase/query`에 `Get*QueryService` 네이밍 | 위치+네이밍 검사 |
| 6 | Entity(`domain/entity`)는 `data class` 금지 | 클래스 종류 검사 |
| 7 | Port는 `domain/port` 인터페이스, 구현체는 `infrastructure`에 | 인터페이스/구현 위치 검사 |
| 8 | Lombok/MapStruct/Mockito import 금지 | import 블랙리스트 |

**도입 절차** (기존 위반 가능성 대비):
1. 테스트를 먼저 작성해 **위반 목록을 출력만** 하고 실패하지 않게 실행
2. 위반 0건인 규칙 → 즉시 활성화
3. 위반 있는 규칙 → 위반 건을 명시적 예외 목록(파일 상단 상수)에 박고 활성화,
   예외는 system-architect-agent 리팩토링 백로그로 등록
4. CI는 수정 불필요 — `gradlew clean test`에 자동 포함됨

**알려진 주의점**: Konsist 0.17.3 내장 파서가 kotlin-compiler-embeddable 2.0.20이라
Kotlin 2.1 전용 신문법을 쓴 파일에서 파싱 이슈가 날 수 있다 — 위반 스캔 단계(1번)에서
함께 드러나므로 별도 사전 조사는 불필요.

**도입 결과 (2026-06-12)**: 11개 규칙 작성, 파서 이슈 없음. 위반 스캔 결과와
baseline 백로그(테스트 파일의 BASELINE 상수와 동일, 제거만 허용):

| 부채 | 대상 | 해소 방향 |
|------|------|----------|
| domain → application.exception import | Repository 5, Policy 5, VO 2, enum 1 (13개 파일) | 도메인이 던지는 예외를 domain 계층으로 이동 |
| application → infrastructure | `SocialLoginUseCase` → `SocialAuthPortRegistry` | Registry의 Port 추출 또는 위치 이동 |
| 소문자 `Usecase` 접미사 | `ManageClubMemberUsecase`, `GenerateFileUrlUsecase` | 리네임 |
| port 패키지 내 클래스 | `FileUploadUrl` (Port 반환 VO) | `domain/vo`로 이동 또는 규칙 예외 확정 |

**도입 후 평가자 기준 2 다이어트** (피드백 반영): Konsist가 들어오면 평가자가 돌리는
`gradlew test`에 아키텍처 검사가 이미 포함된다. 그 시점부터 평가자가 같은 8개 규칙을
LLM 판단으로 또 채점하면 중복이고, 판정 충돌(Konsist 통과 vs 평가자 위반 주장)이
생긴다. 기준 2를 **인코딩 안 된 의미적 잔여물**로 좁힌다:
- UseCase가 오케스트레이션 범위를 넘는 비즈니스 로직을 갖는지 (위치는 맞지만 책임이 틀린 경우)
- 트랜잭션 경계의 의미적 적절성 (어노테이션 위치는 규칙대로지만 경계 안에 외부 I/O가 있는지 등)
- Entity 메서드가 실제로 불변식을 지키는지 (구조가 아니라 내용)

### 3단계 — Stop 훅 컴파일 게이트 (선택적, 1·2단계 안착 후)

**산출물**: `.claude/hooks/compile-gate.sh` + `settings.json` Stop 훅 등록

- 세션 종료(Stop) 시 더티 `.kt` 파일이 있으면 `./gradlew compileKotlin
  compileTestKotlin` 증분 실행. 실패 시 exit 2로 컴파일 오류를 피드백 →
  "다 됐습니다" 보고 전에 깨진 코드를 차단.
- **필수 안전장치**:
  - `stop_hook_active` 플래그 확인 → 무한 루프 방지 (훅 입력 JSON에 포함됨)
  - 더티 `.kt` 없으면 즉시 exit 0 (문서 작업 세션에 비용 0)
  - 타임아웃 120s, Gradle 데몬 전제 (증분 컴파일 ~10-30s)
- **트레이드오프**: 모든 응답 종료마다 지연이 생긴다. 평가자가 이미 컴파일/테스트를
  돌리므로 중복일 수 있음.
- **도입 판단은 데이터로**: 1단계의 메트릭 로그(`eval-log.jsonl`)에서 "컴파일 실패가
  평가 단계에서 잡힌 빈도"와 "평가를 거치지 않은 세션에서 깨진 코드로 종료된 사례"를
  근거로 결정한다. 관측 장치 없이 일화에 의존하지 않는다. verify-implementation이
  일반 대화 세션의 작업까지 커버하므로 이 훅의 필요성은 원안보다 더 낮아진 상태.

### 4단계 — 보류: 전체 default-FAIL 계약 기계장치

`test-results.json` + `verify-gate.sh`(증거 Read 전 결과 파일 쓰기 차단) +
`track-read.sh` + `commit-on-stop.sh` + kill-switch는 **무인 자율 루프**
(`/loop`, 백그라운드 에이전트, 밤샘 실행)를 도입하는 시점에 cwc-long-running-agents
저장소에서 그대로 가져온다. 대화형 작업에서는 1단계의 역할 계약이 같은 효과를
훨씬 싸게 낸다.

## 3. 작업 순서 및 검증

| 순서 | 작업 | 검증 방법 |
|------|------|----------|
| 1 | evaluator-bash-allowlist.sh 훅 + implementation-evaluator 에이전트 작성 | 최근 커밋 하나를 태스크 파일과 함께 채점시켜 출력 형식/증거 인용 확인. 평가자에게 일부러 쓰기성 Bash를 유도해 훅 deny 확인 |
| 2 | 태스크 파일 양식 + verify-implementation 스킬(해시 체크·재평가 위생·메트릭 로그 포함) + 룰에 역할 계약 2줄 추가 | `check-harness.sh` 통과 + 메인 세션에서 소규모 작업 1건 후 스킬이 자동 호출되어 평가 루프가 도는지, eval-log.jsonl이 쌓이는지 확인 |
| 3 | Konsist 의존성 + 위반 스캔 (Layer DSL 기반) | 위반 목록 리뷰 → 규칙별 활성화/예외 결정 (2.0.20 파서 이슈도 이 단계에서 드러남) |
| 4 | ArchitectureTest 활성화 + 평가자 기준 2 다이어트 | `./gradlew test` 통과 + CI 그린 |
| 5 | (조건부) compile-gate Stop 훅 — eval-log.jsonl 데이터로 필요성 판단 | 일부러 깨진 코드로 세션 종료 → exit 2 피드백 확인 |

## 4. 리스크

- **평가자 토큰/시간 비용**: 평가 1회 = opus 스폰 수만 토큰 + 테스트 수 분,
  재시도 시 ×2~3. 1차 방어는 평가 게이트(태스크 파일 없는 경미한 작업은 미발동),
  2차는 평가 범위 한정(이번 diff + 태스크 파일 + 관련 규칙 파일만, 테스트는
  `--tests` 한정). opus→sonnet 전환은 eval-log.jsonl 데이터(이스케이프율)로 판단.
  게이트 임계값이 안 맞으면(평가가 너무 자주/드물게 돌면) 표의 기준을 조정한다.
- **태스크 파일 프레이밍 잔존 위험**: 태스크 파일도 결국 빌더(메인 세션)가 쓴다.
  "구현 전 작성 + 사용자 요청 원문 인용"으로 사후 프레이밍을 차단하지만, 수용 기준
  자체가 느슨하게 쓰일 수는 있다 — 사람 PR 리뷰에서 태스크 파일도 함께 보는 것으로
  보완 (diff에 포함되므로 자연히 노출됨).
- **Konsist 기존 위반**: 활성화 전 스캔 단계에서 흡수. 예외 목록은 "줄어들기만
  해야 하는" 명시적 부채로 관리.
- **평가 루프 발산**: NEEDS_WORK 무한 반복 방지 — 최대 2회 재시도 후 사용자
  에스컬레이션. 재평가는 항상 전체 기준 재채점 (이전 실패 항목만 보면 회귀를 놓침).
- **서브에이전트 훅 우회 가능성**: frontmatter 훅의 Bash 인자 패턴 매칭은 변형에
  취약하고 deny 우회 사례도 보고된 바 있음 — diff 해시 체크가 2차 방어선이며,
  최종 판정의 전제 조건은 패턴 매칭이 아니라 해시 일치다.
- **Stop 훅 지연**: 3단계를 조건부로 미뤄 메트릭 데이터 확인 후 결정.
