---
name: oe-junit
description: Use when writing or fixing Java tests in the OpenEMS OSGi/bnd workspace, including JUnit 4 and JUnit Jupiter tests.
---

# OpenEMS Java Test Skill

Use this skill when writing or fixing Java Edge/Backend tests.

## Test Scope

- Test the use case and its observable behavior, not every method, branch, or implementation detail.
- Prefer the highest practical component-level entry point.
- Do not test private methods or small internal helpers directly.
- Cover the reported behavior together with meaningful boundaries.
- Do not add unrelated robustness scenarios unless they are explicitly requested.
- One test should normally represent one use case or behavioral rule.
- Multiple assertions are fine when they describe one use-case result.

Before implementing, briefly summarize the planned test cases.

Ask the developer only when:

- the expected behavior or scope is unclear
- additional edge cases would broaden the requested scope
- a JUnit 4 test class could be migrated to JUnit Jupiter

For small and clear changes, continue without waiting for confirmation.

## Existing Tests and Parameterization

- Inspect existing tests before adding new ones.
- Extend an existing test when it already covers the same use case.
- Avoid duplicate coverage.
- Prefer parameterized tests when cases use the same setup and assertions and differ only in input and expected output.
- Keep separate tests when cases represent different behavior or would be harder to understand when parameterized.

## Framework Choice

- Prefer JUnit Jupiter using the version already configured in the repository.
- Never mix JUnit 4 and Jupiter within one test class.
- For new test classes, use JUnit Jupiter unless a technical constraint requires JUnit 4.
- When modifying a small JUnit 4 class, ask whether the complete class should be migrated to Jupiter.
- Recommend migration when it is low-risk and mainly requires changing imports, annotations, assertions, or straightforward parameterized tests.
- Keep JUnit 4 when custom runners, complex rules, runtime constraints, or a large migration make it risky or out of scope.
- Do not add unnecessary test dependencies.

## OpenEMS Test Patterns

- Prefer existing OpenEMS test infrastructure over custom fixtures or test harnesses.
- Use `ComponentTest`, `TestCase`, channel assertions, and existing component mocks where applicable.
- Build configs through existing builders such as `MyConfig.create().setId("id0").build()`.
- Preserve intentional trailing `//` in fluent test chains.
- Avoid custom helper methods unless they clearly improve readability.
- Prefer a little duplication over abstractions that hide setup, inputs, execution, or expected behavior.
- Keep tests deterministic and close to the changed behavior.
- Avoid starting a full OSGi runtime unless the existing test style requires it.

## Controller Test Focus

For controller tests, consider only aspects relevant to the requested use case:

- scheduler and controller-order effects
- ESS, grid, PV, and load sign conventions
- relevant channel values and states
- meaningful min/max boundaries
- configuration activation or updates
- resulting channels, set-points, or power behavior

Do not automatically create tests for every item in this list.

## Validation

Use the Gradle wrapper and the narrowest relevant task.

On Windows PowerShell:

```powershell
.\gradlew.bat :bundle.name:test
.\gradlew.bat :bundle.name:checkstyleTest
```