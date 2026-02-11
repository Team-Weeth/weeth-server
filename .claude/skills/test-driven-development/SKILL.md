---
name: test-driven-development
description: Use when implementing any feature or bugfix, before writing implementation code
---

# Test-Driven Development (TDD)

## Overview

Write the test first. Watch it fail. Write minimal code to pass.

**Core principle:** If you didn't watch the test fail, you don't know if it tests the right thing.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

**Always:**
- New features
- Bug fixes
- Refactoring
- Behavior changes

**Exceptions (ask your human partner):**
- Throwaway prototypes
- Generated code
- Configuration files

Thinking "skip TDD just this once"? Stop. That's rationalization.

## The Iron Law

```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

Write code before the test? Delete it. Start over.

**No exceptions:**
- Don't keep it as "reference"
- Don't "adapt" it while writing tests
- Don't look at it
- Delete means delete

Implement fresh from tests. Period.


## Red Phase: Write Failing Test

1. Express intended behavior as test first
2. **Test only one behavior** at a time
3. **Must verify failure** by running test (compilation errors count as failures)

```kotlin
// Kotest DescribeSpec example
class CalculatorTest : DescribeSpec({
    describe("Calculator") {
        it("두 숫자를 더한다") {
            val calculator = Calculator()
            calculator.add(2, 3) shouldBe 5
        }
    }
})
```

Verify failure message matches intent. Unexpected failure reasons indicate test issues.

## Green Phase: Make Test Pass

1. **Write minimal code** to pass the test
2. Hardcoding, duplication, messy code allowed
3. Goal is only green bar

```kotlin
class Calculator {
    fun add(a: Int, b: Int): Int = 5  // Hardcoding OK
}
```

"Working code" first, "clean code" next.

## Refactor Phase: Improve Code

1. **Keep tests passing** while improving
2. Remove duplication, improve naming, apply patterns
3. **Must re-run tests** after refactoring
4. No new features - structural improvements only

```kotlin
class Calculator {
    fun add(a: Int, b: Int): Int = a + b  // Generalize
}
```


## Checklist

### Red
- [ ] Test verifies single behavior?
- [ ] Verified failure by running test?
- [ ] Failure message as intended?

### Green
- [ ] Passed with simplest approach?
- [ ] Test is green?

### Refactor
- [ ] Tests still green?
- [ ] Duplication removed?
- [ ] Names reveal intent?
- [ ] No new features added?
